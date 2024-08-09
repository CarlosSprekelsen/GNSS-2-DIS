package ets.acmi.gnssdislogger.listeners;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.TaskStackBuilder;
import androidx.core.text.HtmlCompat;

import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityRecognitionClient;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

import org.slf4j.Logger;

import java.text.SimpleDateFormat;
import java.util.Date;

import de.greenrobot.event.EventBus;
import ets.acmi.gnssdislogger.R;
import ets.acmi.gnssdislogger.RestarterReceiver;
import ets.acmi.gnssdislogger.common.BundleConstants;
import ets.acmi.gnssdislogger.common.EventBusHook;
import ets.acmi.gnssdislogger.common.IntentConstants;
import ets.acmi.gnssdislogger.common.Locations;
import ets.acmi.gnssdislogger.common.Maths;
import ets.acmi.gnssdislogger.common.PreferenceHelper;
import ets.acmi.gnssdislogger.common.Session;
import ets.acmi.gnssdislogger.common.Strings;
import ets.acmi.gnssdislogger.common.Systems;
import ets.acmi.gnssdislogger.common.events.CommandEvents;
import ets.acmi.gnssdislogger.common.events.ServiceEvents;
import ets.acmi.gnssdislogger.common.slf4j.Logs;
import ets.acmi.gnssdislogger.common.slf4j.SessionLogcatAppender;
import ets.acmi.gnssdislogger.loggers.FileLoggerFactory;
import ets.acmi.gnssdislogger.loggers.nmea.NmeaFileLogger;
import ets.acmi.gnssdislogger.senders.AlarmReceiver;
import ets.acmi.gnssdislogger.senders.FileSenderFactory;
import ets.acmi.gnssdislogger.ui.activity.GnssMainActivity;
import ets.acmi.gnssdislogger.ui.activity.NotificationAnnotationActivity;

public class GnssLoggingService extends Service {
    private static final Logger LOG = Logs.of(GnssLoggingService.class);
    private static NotificationManager notificationManager;
    private static final int NOTIFICATION_ID = 8675309;
    private final IBinder binder = new GnssLoggingBinder();
    private LocationManager gnssLocationManager;
    private GnssStatusCallback gnssStatusCallback;
    private AlarmManager nextPointAlarmManager;
    private PendingIntent activityRecognitionPendingIntent;
    private NotificationCompat.Builder nfc;
    // ---------------------------------------------------
    // Helpers and managers
    // ---------------------------------------------------
    private final PreferenceHelper preferenceHelper = PreferenceHelper.getInstance();
    private final Session session = Session.getInstance();
    private LocationManager passiveLocationManager;
    private LocationManager towerLocationManager;
    private GeneralGnssListener gnssListenerStatus;
    private GeneralGnssListener towerLocationListener;
    private GeneralGnssListener passiveLocationListener;
    private Intent alarmIntent;
    private final Handler handler = new Handler();
    // ---------------------------------------------------
    private final Runnable stopManagerRunnable = () -> {
        LOG.warn("Absolute timeout reached, giving up on this point");
        stopManagerAndResetAlarm();
    };

    @Override
    public IBinder onBind(Intent arg0) {
        return binder;
    }

    @Override
    public void onCreate() {

        nextPointAlarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        registerEventBus();
    }

    private void requestActivityRecognitionUpdates() {

        if (preferenceHelper.shouldNotLogIfUserIsStill()) {
            LOG.debug("Requesting activity recognition updates");
            Intent intent = new Intent(getApplicationContext(), GnssLoggingService.class);
            activityRecognitionPendingIntent = PendingIntent.getService(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            ActivityRecognitionClient arClient = ActivityRecognition.getClient(getApplicationContext());
            arClient.requestActivityUpdates(preferenceHelper.getMinimumLoggingInterval() * 1000, activityRecognitionPendingIntent);
        }

    }

    private void stopActivityRecognitionUpdates() {
        try {
            if (activityRecognitionPendingIntent != null) {
                LOG.debug("Stopping activity recognition updates");
                ActivityRecognitionClient arClient = ActivityRecognition.getClient(getApplicationContext());
                arClient.removeActivityUpdates(activityRecognitionPendingIntent);
            }
        } catch (Exception ex) {
            LOG.error("Could not stop activity recognition service", ex);
        }
    }

    private void registerEventBus() {
        EventBus.getDefault().registerSticky(this);
    }

    private void unregisterEventBus() {
        try {
            EventBus.getDefault().unregister(this);
        } catch (Throwable t) {
            //this may crash if registration did not go through. just be safe
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        handleIntent(intent);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        LOG.warn(SessionLogcatAppender.MARKER_INTERNAL, "GnssLoggingService is being destroyed by Android OS.");
        unregisterEventBus();
        removeNotification();
        super.onDestroy();

        if (session.isStarted()) {
            LOG.error("Service unexpectedly destroyed while GNSSLogger was running. Will send broadcast to RestarterReceiver.");
            Intent broadcastIntent = new Intent(getApplicationContext(), RestarterReceiver.class);
            broadcastIntent.putExtra("was_running", true);
            sendBroadcast(broadcastIntent);
        }
    }

    @Override
    public void onLowMemory() {
        LOG.error("Android is low on memory!");
        super.onLowMemory();
    }

    private void handleIntent(Intent intent) {

        ActivityRecognitionResult arr = ActivityRecognitionResult.extractResult(intent);
        if (arr != null) {
            EventBus.getDefault().post(new ServiceEvents.ActivityRecognitionEvent(arr));
            return;
        }

        if (intent != null) {
            Bundle bundle = intent.getExtras();

            if (bundle != null) {


                if (!Systems.locationPermissionsGranted(this)) {
                    LOG.error("User has not granted permission to access location services. Will not continue!");
                    stopLogging();
                    stopSelf();
                    return;
                }

                boolean needToStartGnssManager = false;

                if (bundle.getBoolean(IntentConstants.IMMEDIATE_START)) {
                    LOG.info("Intent received - Start Logging Now");
                    EventBus.getDefault().post(new CommandEvents.RequestStartStop(true));
                }

                if (bundle.getBoolean(IntentConstants.IMMEDIATE_STOP)) {
                    LOG.info("Intent received - Stop logging now");
                    EventBus.getDefault().post(new CommandEvents.RequestStartStop(false));
                }

                if (bundle.getBoolean(IntentConstants.GET_STATUS)) {
                    LOG.info("Intent received - Sending Status by broadcast");
                    EventBus.getDefault().post(new CommandEvents.GetStatus());
                }


                if (bundle.getBoolean(IntentConstants.AUTOSEND_NOW)) {
                    LOG.info("Intent received - Send Email Now");
                    EventBus.getDefault().post(new CommandEvents.AutoSend(null));
                }

                if (bundle.getBoolean(IntentConstants.GET_NEXT_POINT)) {
                    LOG.info("Intent received - Get Next Point");
                    needToStartGnssManager = true;
                }

                if (bundle.getString(IntentConstants.SET_DESCRIPTION) != null) {
                    LOG.info("Intent received - Set Next Point Description: " + bundle.getString(IntentConstants.SET_DESCRIPTION));
                    EventBus.getDefault().post(new CommandEvents.Annotate(bundle.getString(IntentConstants.SET_DESCRIPTION)));
                }


                if (bundle.get(IntentConstants.PREFER_CELLTOWER) != null) {
                    boolean preferCellTower = bundle.getBoolean(IntentConstants.PREFER_CELLTOWER);
                    LOG.debug("Intent received - Set Prefer Cell Tower: " + preferCellTower);

                    if (preferCellTower) {
                        preferenceHelper.setShouldLogNetworkLocations(true);
                        preferenceHelper.setShouldLogSatelliteLocations(false);
                    } else {
                        preferenceHelper.setShouldLogSatelliteLocations(true);
                        preferenceHelper.setShouldLogNetworkLocations(false);
                    }

                    needToStartGnssManager = true;
                }

                if (bundle.get(IntentConstants.TIME_BEFORE_LOGGING) != null) {
                    int timeBeforeLogging = bundle.getInt(IntentConstants.TIME_BEFORE_LOGGING);
                    LOG.debug("Intent received - logging interval: " + timeBeforeLogging);
                    preferenceHelper.setMinimumLoggingInterval(timeBeforeLogging);
                    needToStartGnssManager = true;
                }

                if (bundle.get(IntentConstants.DISTANCE_BEFORE_LOGGING) != null) {
                    int distanceBeforeLogging = bundle.getInt(IntentConstants.DISTANCE_BEFORE_LOGGING);
                    LOG.debug("Intent received - Set Distance Before Logging: " + distanceBeforeLogging);
                    preferenceHelper.setMinimumDistanceInMeters(distanceBeforeLogging);
                    needToStartGnssManager = true;
                }

                if (bundle.get(IntentConstants.GPS_ON_BETWEEN_FIX) != null) {
                    boolean keepBetweenFix = bundle.getBoolean(IntentConstants.GPS_ON_BETWEEN_FIX);
                    LOG.debug("Intent received - Set Keep Between Fix: " + keepBetweenFix);
                    preferenceHelper.setShouldKeepGPSOnBetweenFixes(keepBetweenFix);
                    needToStartGnssManager = true;
                }

                if (bundle.get(IntentConstants.RETRY_TIME) != null) {
                    int retryTime = bundle.getInt(IntentConstants.RETRY_TIME);
                    LOG.debug("Intent received - Set duration to match accuracy: " + retryTime);
                    preferenceHelper.setLoggingRetryPeriod(retryTime);
                    needToStartGnssManager = true;
                }

                if (bundle.get(IntentConstants.ABSOLUTE_TIMEOUT) != null) {
                    int absoluteTimeout = bundle.getInt(IntentConstants.ABSOLUTE_TIMEOUT);
                    LOG.debug("Intent received - Set absolute timeout: " + absoluteTimeout);
                    preferenceHelper.setAbsoluteTimeoutForAcquiringPosition(absoluteTimeout);
                    needToStartGnssManager = true;
                }

                if (bundle.get(IntentConstants.LOG_ONCE) != null) {
                    boolean logOnceIntent = bundle.getBoolean(IntentConstants.LOG_ONCE);
                    LOG.debug("Intent received - FragmentLogView Once: " + logOnceIntent);
                    needToStartGnssManager = false;
                    logOnce();
                }

                try {
                    if (bundle.get(Intent.EXTRA_ALARM_COUNT) != "0") {
                        needToStartGnssManager = true;
                    }
                } catch (Throwable t) {
                    LOG.warn(SessionLogcatAppender.MARKER_INTERNAL, "Received a weird EXTRA_ALARM_COUNT value. Cannot continue.");
                    needToStartGnssManager = false;
                }


                if (needToStartGnssManager && session.isStarted()) {
                    startGnssManager();
                }
            }
        } else {
            // A null intent is passed in if the service has been killed and restarted.
            LOG.debug("Service restarted with null intent. Were we logging previously - " + session.isStarted());
            if (session.isStarted()) {
                startLogging();
            }

        }
    }

    /**
     * Sets up the auto send timers based on user preferences.
     */
    @TargetApi(23)
    private void setupAutoSendTimers() {
        LOG.debug("Setting up autosend timers. Auto Send Enabled - " + preferenceHelper.isAutoSendEnabled()
                + ", Auto Send Delay - " + session.getAutoSendDelay());

        if (preferenceHelper.isAutoSendEnabled() && session.getAutoSendDelay() > 0) {
            long triggerTime = System.currentTimeMillis() + (long) (session.getAutoSendDelay() * 60 * 1000);

            alarmIntent = new Intent(this, AlarmReceiver.class);
            cancelAlarm();

            PendingIntent sender = PendingIntent.getBroadcast(this, 0, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
            if (Systems.isDozing(this)) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, sender);
            } else {
                am.set(AlarmManager.RTC_WAKEUP, triggerTime, sender);
            }
            LOG.debug("Autosend alarm has been set");

        } else {
            if (alarmIntent != null) {
                LOG.debug("alarmIntent was null, canceling alarm");
                cancelAlarm();
            }
        }
    }

    private void logOnce() {
        session.setSinglePointMode(true);

        if (session.isStarted()) {
            startGnssManager();
        } else {
            startLogging();
        }
    }

    private void cancelAlarm() {
        if (alarmIntent != null) {
            AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
            PendingIntent sender = PendingIntent.getBroadcast(this, 0, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            am.cancel(sender);
        }
    }

    /**
     * Method to be called if user has chosen to auto send log files when he
     * stops logging
     */
    private void autoSendLogFileOnStop() {
        if (preferenceHelper.isAutoSendEnabled() && preferenceHelper.shouldAutoSendOnStopLogging()) {
            autoSendLogFile(null);
        }
    }

    /**
     * Calls the Auto Senders which process the files and send it.
     */
    private void autoSendLogFile(@Nullable String formattedFileName) {

        LOG.debug("Filename: " + formattedFileName);

        if (!Strings.isNullOrEmpty(formattedFileName) || !Strings.isNullOrEmpty(Strings.getFormattedFileName())) {
            String fileToSend = Strings.isNullOrEmpty(formattedFileName) ? Strings.getFormattedFileName() : formattedFileName;
            FileSenderFactory.autoSendFiles(fileToSend);
            setupAutoSendTimers();
        }
    }

    private void resetAutoSendTimersIfNecessary() {

        if (session.getAutoSendDelay() != preferenceHelper.getAutoSendInterval()) {
            session.setAutoSendDelay(preferenceHelper.getAutoSendInterval());
            setupAutoSendTimers();
        }
    }

    /**
     * Resets the form, resets file name if required, reobtains preferences
     */
    private void startLogging() {
        LOG.debug(".");
        session.setAddNewTrackSegment(true);


        try {
            startForeground(NOTIFICATION_ID, getNotification());
        } catch (Exception ex) {
            LOG.error("Could not start GNSSLoggingService in foreground. ", ex);
        }

        session.setStarted(true);

        resetAutoSendTimersIfNecessary();
        showNotification();
        setupAutoSendTimers();
        resetCurrentFileName(true);
        notifyClientsStarted(true);
        startPassiveManager();
        startGnssManager();
        requestActivityRecognitionUpdates();
        requestActivityRecognitionUpdates();

    }

    private void notifyByBroadcast(boolean loggingStarted) {
        LOG.debug("Sending a custom broadcast");
        String event = (loggingStarted) ? "started" : "stopped";
        Intent sendIntent = new Intent();
        sendIntent.setAction("ets.acmi.ets.acmi.gnssdislogger.EVENT");
        sendIntent.putExtra("gnssloggerevent", event);
        sendIntent.putExtra("filename", session.getCurrentFormattedFileName());
        sendIntent.putExtra("startedtimestamp", session.getStartTimeStamp());
        sendBroadcast(sendIntent);
    }

    /**
     * Informs main activity and broadcast listeners whether logging has started/stopped
     */
    private void notifyClientsStarted(boolean started) {
        LOG.info((started) ? getString(R.string.started) : getString(R.string.stopped));
        notifyByBroadcast(started);
        EventBus.getDefault().post(new ServiceEvents.LoggingStatus(started));
    }

    /**
     * Notify status of logger
     */
    private void notifyStatus(boolean started) {
        LOG.info((started) ? getString(R.string.started) : getString(R.string.stopped));
        notifyByBroadcast(started);
    }

    /**
     * Stops logging, removes notification, stops GNSS manager, stops autosend timer
     */
    private void stopLogging() {
        LOG.debug(".");
        session.setAddNewTrackSegment(true);
        session.setTotalTravelled(0);
        session.setPreviousLocationInfo(null);
        session.setStarted(false);
        session.setUserStillSinceTimeStamp(0);
        session.setLatestTimeStamp(0);
        stopAbsoluteTimer();
        // Email log file before setting location info to null
        autoSendLogFileOnStop();
        cancelAlarm();
        session.setCurrentLocationInfo(null);
        session.setSinglePointMode(false);
        stopForeground(true);

        removeNotification();
        stopAlarm();
        stopGnssManager();
        stopPassiveManager();
        stopActivityRecognitionUpdates();
        notifyClientsStarted(false);
        session.setCurrentFileName("");
        session.setCurrentFormattedFileName("");
        stopSelf();
    }

    /**
     * Hides the notification icon in the status bar if it's visible.
     */
    private void removeNotification() {
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
    }

    /**
     * Shows a notification icon in the status bar for GNSS Logger
     */
    private Notification getNotification() {

        Intent stopLoggingIntent = new Intent(this, GnssLoggingService.class);
        stopLoggingIntent.setAction("NotificationButton_STOP");
        stopLoggingIntent.putExtra(IntentConstants.IMMEDIATE_STOP, true);
        PendingIntent piStop = PendingIntent.getService(this, 0, stopLoggingIntent, 0);

        Intent annotateIntent = new Intent(this, NotificationAnnotationActivity.class);
        annotateIntent.setAction("ets.acmi.ets.acmi.gnssdislogger.NOTIFICATION_BUTTON");
        PendingIntent piAnnotate = PendingIntent.getActivity(this, 0, annotateIntent, 0);

        // What happens when the notification item is clicked
        Intent contentIntent = new Intent(this, GnssMainActivity.class);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addNextIntent(contentIntent);

        PendingIntent pending = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        CharSequence contentTitle = getString(R.string.gnsslogger_still_running);
        CharSequence contentText = getString(R.string.app_name);
        long notificationTime = System.currentTimeMillis();

        if (session.hasValidLocation()) {
            contentTitle = Strings.getFormattedLatitude(session.getCurrentLatitude()) + ", "
                    + Strings.getFormattedLongitude(session.getCurrentLongitude());

            contentText = HtmlCompat.fromHtml("<b>" + getString(R.string.txt_altitude) + "</b> " + Strings.getDistanceDisplay(this, session.getCurrentLocationInfo().getAltitude(), preferenceHelper.shouldDisplayImperialUnits(), false)
                            + "  "
                            + "<b>" + getString(R.string.txt_travel_duration) + "</b> " + Strings.getDescriptiveDurationString((int) (System.currentTimeMillis() - session.getStartTimeStamp()) / 1000, this)
                            + "  "
                            + "<b>" + getString(R.string.txt_accuracy) + "</b> " + Strings.getDistanceDisplay(this, session.getCurrentLocationInfo().getAccuracy(), preferenceHelper.shouldDisplayImperialUnits(), true),
                    HtmlCompat.FROM_HTML_MODE_LEGACY);

            notificationTime = session.getCurrentLocationInfo().getTime();
        }

        if (nfc == null) {

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

                NotificationChannel channel = new NotificationChannel("ets.acmi.gnssdislogger", getString(R.string.app_name), NotificationManager.IMPORTANCE_DEFAULT);
                channel.enableLights(false);
                channel.enableVibration(false);
                channel.setSound(null, null);
                channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);

                channel.setShowBadge(true);
                manager.createNotificationChannel(channel);

            }

            nfc = new NotificationCompat.Builder(getApplicationContext(), "ets.acmi.gnssdislogger")
                    .setSmallIcon(R.drawable.notification)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.gnssloggericon3))
                    .setPriority(preferenceHelper.shouldHideNotificationFromStatusBar() ? NotificationCompat.PRIORITY_MIN : NotificationCompat.PRIORITY_LOW)
                    .setCategory(NotificationCompat.CATEGORY_SERVICE)
                    .setVisibility(NotificationCompat.VISIBILITY_SECRET) //This hides the notification from lock screen
                    .setContentTitle(contentTitle)
                    .setContentText(contentText)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(contentText).setBigContentTitle(contentTitle))
                    .setOngoing(true)
                    .setContentIntent(pending);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                nfc.setPriority(NotificationCompat.PRIORITY_LOW);
            }

            if (!preferenceHelper.shouldHideNotificationButtons()) {
                nfc.addAction(R.drawable.annotate2, getString(R.string.menu_annotate), piAnnotate)
                        .addAction(android.R.drawable.ic_menu_close_clear_cancel, getString(R.string.shortcut_stop), piStop);
            }
        }


        nfc.setContentTitle(contentTitle);
        nfc.setContentText(contentText);
        nfc.setStyle(new NotificationCompat.BigTextStyle().bigText(contentText).setBigContentTitle(contentTitle));
        nfc.setWhen(notificationTime);

        //notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        //notificationManager.notify(NOTIFICATION_ID, nfc.build());
        return nfc.build();
    }

    private void showNotification() {
        Notification notif = getNotification();
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, notif);
    }

    @SuppressWarnings("ResourceType")
    private void startPassiveManager() {
        if (preferenceHelper.shouldLogPassiveLocations()) {
            LOG.debug("Starting passive location listener");
            if (passiveLocationListener == null) {
                passiveLocationListener = new GeneralGnssListener(this, BundleConstants.PASSIVE);
            }
            passiveLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            passiveLocationManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, 1000, 0, passiveLocationListener);
        }
    }

    /**
     * Starts the location manager. There are two location managers - GNSS and
     * Cell Tower. This code determines which manager to request updates from
     * based on user preference and whichever is enabled. If GNSS is enabled on
     * the phone, that is used. But if the user has also specified that they
     * prefer cell towers, then cell towers are used. If neither is enabled,
     * then nothing is requested.
     */
    @SuppressWarnings("ResourceType")
    private void startGnssManager() {

        //If the user has been still for more than the minimum seconds
        if (userHasBeenStillForTooLong()) {
            LOG.info("No movement detected in the past interval, will not log");
            setAlarmForNextPoint();
            return;
        }

        if (gnssListenerStatus == null) {
            gnssListenerStatus = new GeneralGnssListener(this, "GNSS");
        }

        if (towerLocationListener == null) {
            towerLocationListener = new GeneralGnssListener(this, "CELL");
        }

        if (gnssStatusCallback == null) {
            gnssStatusCallback = new GnssStatusCallback();
        }

        gnssLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        towerLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        checkTowerAndGnssStatus();

        if (session.isGnssEnabled() && preferenceHelper.shouldLogSatelliteLocations()) {
            LOG.info("Requesting GNSS location updates");
            // gnss satellite based
            gnssLocationManager.registerGnssStatusCallback(gnssStatusCallback);
            gnssLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, gnssListenerStatus);

            //gnssLocationManager.addNmeaListener(gnssStatusCallback);
            //GeneralNMEAListener.NMEAListenerLegacy listenerLegacy = new GeneralNMEAListener.NMEAListenerLegacy(gnssStatusCallback, this);
            GeneralNMEAListener.NMEAListener24 newListener = new GeneralNMEAListener.NMEAListener24(gnssListenerStatus, this);
            gnssLocationManager.addNmeaListener(newListener);
            LOG.info("Added NMEA listener, new version");

            session.setUsingGnss(true);
            startAbsoluteTimer();
        }

        if (session.isTowerEnabled() && (preferenceHelper.shouldLogNetworkLocations() || !session.isGnssEnabled())) {
            LOG.info("Requesting cell and wifi location updates");
            session.setUsingGnss(false);
            // Cell tower and wifi based
            towerLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 0, towerLocationListener);

            startAbsoluteTimer();
        }

        if (!session.isTowerEnabled() && !session.isGnssEnabled()) {
            LOG.error("No provider available!");
            session.setUsingGnss(false);
            LOG.error(getString(R.string.gpsprovider_unavailable));
            stopLogging();
            setLocationServiceUnavailable();
            return;
        }

        if (!preferenceHelper.shouldLogNetworkLocations() && !preferenceHelper.shouldLogSatelliteLocations() && !preferenceHelper.shouldLogPassiveLocations()) {
            LOG.error("No location provider selected!");
            session.setUsingGnss(false);
            stopLogging();
            return;
        }

        EventBus.getDefault().post(new ServiceEvents.WaitingForLocation(true));
        session.setWaitingForLocation(true);
    }

    private boolean userHasBeenStillForTooLong() {
        return !session.hasDescription() && !session.isSinglePointMode() &&
                (session.getUserStillSinceTimeStamp() > 0 && (System.currentTimeMillis() - session.getUserStillSinceTimeStamp()) > (preferenceHelper.getMinimumLoggingInterval() * 1000));
    }

    private void startAbsoluteTimer() {
        if (preferenceHelper.getAbsoluteTimeoutForAcquiringPosition() >= 1) {
            handler.postDelayed(stopManagerRunnable, preferenceHelper.getAbsoluteTimeoutForAcquiringPosition() * 1000);
        }
    }

    private void stopAbsoluteTimer() {
        handler.removeCallbacks(stopManagerRunnable);
    }

    /**
     * This method is called periodically to determine whether the cell tower /
     * gnss providers have been enabled, and sets class level variables to those
     * values.
     */
    private void checkTowerAndGnssStatus() {
        session.setTowerEnabled(towerLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER));
        session.setGnssEnabled(gnssLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER));
    }

    /**
     * Stops the location managers
     */
    @SuppressWarnings("ResourceType")
    private void stopGnssManager() {

        if (towerLocationListener != null) {
            LOG.debug("Removing towerLocationManager updates");
            towerLocationManager.removeUpdates(towerLocationListener);
        }

        if (gnssListenerStatus != null) {
            LOG.debug("Removing gnssLocationManager updates");
            gnssLocationManager.removeUpdates(gnssListenerStatus);
            gnssLocationManager.unregisterGnssStatusCallback(gnssStatusCallback);
        }

        session.setWaitingForLocation(false);
        EventBus.getDefault().post(new ServiceEvents.WaitingForLocation(false));

    }

    @SuppressWarnings("ResourceType")
    private void stopPassiveManager() {
        if (passiveLocationManager != null) {
            LOG.debug("Removing passiveLocationManager updates");
            passiveLocationManager.removeUpdates(passiveLocationListener);
        }
    }

    /**
     * Sets the current file name based on user preference.
     */
    private void resetCurrentFileName(boolean newLogEachStart) {

        String oldFileName = session.getCurrentFormattedFileName();

        /* Update the file name, if required. (New day, Re-start service) */
        if (preferenceHelper.shouldCreateCustomFile()) {
            if (Strings.isNullOrEmpty(Strings.getFormattedFileName())) {
                session.setCurrentFileName(preferenceHelper.getCustomFileName());
            }

            LOG.debug("Should change file name dynamically: " + preferenceHelper.shouldChangeFileNameDynamically());

            if (!preferenceHelper.shouldChangeFileNameDynamically()) {
                session.setCurrentFileName(Strings.getFormattedFileName());
            }

        } else if (preferenceHelper.shouldCreateNewFileOnceAMonth()) {
            // 201001.gpx
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMM");
            session.setCurrentFileName(sdf.format(new Date()));
        } else if (preferenceHelper.shouldCreateNewFileOnceADay()) {
            // 20100114.gpx
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
            session.setCurrentFileName(sdf.format(new Date()));
        } else if (newLogEachStart) {
            // 20100114183329.gpx
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
            session.setCurrentFileName(sdf.format(new Date()));
        }

        if (!Strings.isNullOrEmpty(oldFileName)
                && !oldFileName.equalsIgnoreCase(Strings.getFormattedFileName())
                && session.isStarted()) {
            LOG.debug("New file name, should auto upload the old one");
            EventBus.getDefault().post(new CommandEvents.AutoSend(oldFileName));
        }

        session.setCurrentFormattedFileName(Strings.getFormattedFileName());

        LOG.info("Filename: " + Strings.getFormattedFileName());
        EventBus.getDefault().post(new ServiceEvents.FileNamed(Strings.getFormattedFileName()));

    }


    private void setLocationServiceUnavailable() {
        EventBus.getDefault().post(new ServiceEvents.LocationServicesUnavailable());
    }

    /**
     * Stops location manager, then starts it.
     */
    void restartGnssManagers() {
        LOG.debug("Restarting location managers");
        stopGnssManager();
        startGnssManager();
    }

    /**
     * This event is raised when the GeneralGnssListener has a new location.
     * This method in turn updates notification, writes to file, reobtains
     * preferences, notifies main service client and resets location managers.
     *
     * @param loc Location object
     */
    void onLocationChanged(Location loc) {
        if (!session.isStarted()) {
            LOG.debug("onLocationChanged called, but session.isStarted is false");
            stopLogging();
            return;
        }

        boolean isPassiveLocation = loc.getExtras().getBoolean(BundleConstants.PASSIVE);
        long currentTimeStamp = System.currentTimeMillis();

        LOG.debug("Has description? " + session.hasDescription() + ", Single point? " + session.isSinglePointMode() + ", Last timestamp: " + session.getLatestTimeStamp());

        // Don't log a point until the user-defined time has elapsed
        // However, if user has set an annotation, just log the point, disregard time and distance filters
        // However, if it's a passive location, disregard the time filter
        if (!isPassiveLocation && !session.hasDescription() && !session.isSinglePointMode() && (currentTimeStamp - session.getLatestTimeStamp()) < (preferenceHelper.getMinimumLoggingInterval() * 1000)) {
            return;
        }

        //Don't log a point if user has been still
        // However, if user has set an annotation, just log the point, disregard time and distance filters
        if (userHasBeenStillForTooLong()) {
            LOG.info("Received location but the user hasn't moved, ignoring");
            return;
        }

        if (!isPassiveLocation && !isFromValidListener(loc)) {
            return;
        }

        //Check if a ridiculous distance has been travelled since previous point - could be a bad GNSS jump
        if (session.getCurrentLocationInfo() != null) {
            double distanceTravelled = Maths.calculateDistance(loc.getLatitude(), loc.getLongitude(), session.getCurrentLocationInfo().getLatitude(), session.getCurrentLocationInfo().getLongitude());
            long timeDifference = (int) Math.abs(loc.getTime() - session.getCurrentLocationInfo().getTime()) / 1000;

            if (timeDifference > 0 && (distanceTravelled / timeDifference) > 357) { //357 m/s ~=  1285 km/h
                LOG.warn(String.format("Very large jump detected - %d meters in %d sec - discarding point", (long) distanceTravelled, timeDifference));
                return;
            }
        }

        // Don't do anything until the user-defined accuracy is reached
        // even for annotations
        if (preferenceHelper.getMinimumAccuracy() > 0) {

            if (!loc.hasAccuracy() || loc.getAccuracy() == 0) {
                return;
            }

            if (preferenceHelper.getMinimumAccuracy() < Math.abs(loc.getAccuracy())) {

                if (session.getFirstRetryTimeStamp() == 0) {
                    session.setFirstRetryTimeStamp(System.currentTimeMillis());
                }

                if (currentTimeStamp - session.getFirstRetryTimeStamp() <= preferenceHelper.getLoggingRetryPeriod() * 1000) {
                    LOG.warn("Only accuracy of " + loc.getAccuracy() + " m. Point discarded." + getString(R.string.inaccurate_point_discarded));
                    //return and keep trying
                    return;
                }

                if (currentTimeStamp - session.getFirstRetryTimeStamp() > preferenceHelper.getLoggingRetryPeriod() * 1000) {
                    LOG.warn("Only accuracy of " + loc.getAccuracy() + " m and timeout reached." + getString(R.string.inaccurate_point_discarded));
                    //Give up for now
                    stopManagerAndResetAlarm();

                    //reset timestamp for next time.
                    session.setFirstRetryTimeStamp(0);
                    return;
                }

                //Success, reset timestamp for next time.
                session.setFirstRetryTimeStamp(0);
            }
        }

        //Don't do anything until the user-defined distance has been traversed
        // However, if user has set an annotation, just log the point, disregard time and distance filters
        // However, if it's a passive location, ignore distance filter.
        if (!isPassiveLocation && !session.hasDescription() && !session.isSinglePointMode() && preferenceHelper.getMinimumDistanceInterval() > 0 && session.hasValidLocation()) {

            double distanceTraveled = Maths.calculateDistance(loc.getLatitude(), loc.getLongitude(),
                    session.getCurrentLatitude(), session.getCurrentLongitude());

            if (preferenceHelper.getMinimumDistanceInterval() > distanceTraveled) {
                LOG.warn(String.format(getString(R.string.not_enough_distance_traveled), String.valueOf(Math.floor(distanceTraveled))) + ", point discarded");
                stopManagerAndResetAlarm();
                return;
            }
        }


        LOG.info(SessionLogcatAppender.MARKER_LOCATION, loc.getLatitude() + "," + loc.getLongitude());
        loc = Locations.getLocationWithAdjustedAltitude(loc, preferenceHelper);
        loc = Locations.getLocationAdjustedForGPSWeekRollover(loc);
        resetCurrentFileName(false);
        session.setLatestTimeStamp(System.currentTimeMillis());
        session.setFirstRetryTimeStamp(0);
        session.setCurrentLocationInfo(loc);
        setDistanceTraveled(loc);
        showNotification();

        if (isPassiveLocation) {
            LOG.debug("Logging passive location to file");
        }

        writeToFile(loc);
        resetAutoSendTimersIfNecessary();
        stopManagerAndResetAlarm();

        EventBus.getDefault().post(new ServiceEvents.LocationUpdate(loc));

        if (session.isSinglePointMode()) {
            LOG.debug("Single point mode - stopping now");
            stopLogging();
        }
    }

    private boolean isFromValidListener(Location loc) {

        if (!preferenceHelper.shouldLogSatelliteLocations() && !preferenceHelper.shouldLogNetworkLocations()) {
            return true;
        }

        if (!preferenceHelper.shouldLogNetworkLocations()) {
            return loc.getProvider().equalsIgnoreCase(LocationManager.GPS_PROVIDER);
        }

        if (!preferenceHelper.shouldLogSatelliteLocations()) {
            return !loc.getProvider().equalsIgnoreCase(LocationManager.GPS_PROVIDER);
        }

        return true;
    }

    private void setDistanceTraveled(Location loc) {
        // Distance
        if (session.getPreviousLocationInfo() == null) {
            session.setPreviousLocationInfo(loc);
        }
        // Calculate this location and the previous location location and add to the current running total distance.
        // NOTE: Should be used in conjunction with 'distance required before logging' for more realistic values.
        double distance = Maths.calculateDistance(
                session.getPreviousLatitude(),
                session.getPreviousLongitude(),
                loc.getLatitude(),
                loc.getLongitude());
        session.setPreviousLocationInfo(loc);
        session.setTotalTravelled(session.getTotalTravelled() + distance);
    }

    void stopManagerAndResetAlarm() {
        if (!preferenceHelper.shouldKeepGPSOnBetweenFixes()) {
            stopGnssManager();
        }

        stopAbsoluteTimer();
        setAlarmForNextPoint();
    }


    private void stopAlarm() {
        Intent i = new Intent(this, GnssLoggingService.class);
        i.putExtra(IntentConstants.GET_NEXT_POINT, true);
        PendingIntent pi = PendingIntent.getService(this, 0, i, 0);
        nextPointAlarmManager.cancel(pi);
    }

    @TargetApi(23)
    private void setAlarmForNextPoint() {
        LOG.debug("Set alarm for " + preferenceHelper.getMinimumLoggingInterval() + " seconds");

        Intent i = new Intent(this, GnssLoggingService.class);
        i.putExtra(IntentConstants.GET_NEXT_POINT, true);
        PendingIntent pi = PendingIntent.getService(this, 0, i, 0);
        nextPointAlarmManager.cancel(pi);

        if (Systems.isDozing(this)) {
            //Only invoked once per 15 minutes in doze mode
            LOG.warn("Device is dozing, using infrequent alarm");
            nextPointAlarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + preferenceHelper.getMinimumLoggingInterval() * 1000, pi);
        } else {
            nextPointAlarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + preferenceHelper.getMinimumLoggingInterval() * 1000, pi);
        }
    }


    /**
     * Calls file helper to write a given location to a file.
     *
     * @param loc Location object
     */
    private void writeToFile(Location loc) {
        session.setAddNewTrackSegment(false);

        try {
            LOG.debug("Calling file writers");
            FileLoggerFactory.write(getApplicationContext(), loc);

            if (session.hasDescription()) {
                LOG.info("Writing annotation: " + session.getDescription());
                FileLoggerFactory.annotate(getApplicationContext(), session.getDescription(), loc);
            }
        } catch (Exception e) {
            LOG.error(getString(R.string.could_not_write_to_file), e);
        }

        session.clearDescription();
        EventBus.getDefault().post(new ServiceEvents.AnnotationStatus(true));
    }


    public void onNmeaSentence(long timestamp, String nmeaSentence) {

        if (preferenceHelper.shouldLogToNmea()) {
            NmeaFileLogger nmeaLogger = new NmeaFileLogger(Strings.getFormattedFileName());
            nmeaLogger.write(timestamp, nmeaSentence);
        }
    }

    @EventBusHook
    public void onEvent(CommandEvents.RequestToggle requestToggle) {
        if (session.isStarted()) {
            stopLogging();
        } else {
            startLogging();
        }
    }

    @EventBusHook
    public void onEvent(CommandEvents.RequestStartStop startStop) {
        if (startStop.start) {
            startLogging();
        } else {
            stopLogging();
        }

        EventBus.getDefault().removeStickyEvent(CommandEvents.RequestStartStop.class);
    }

    @EventBusHook
    public void onEvent(CommandEvents.GetStatus getStatus) {
        CommandEvents.GetStatus statusEvent = EventBus.getDefault().removeStickyEvent(CommandEvents.GetStatus.class);
        if (statusEvent != null) {
            notifyStatus(session.isStarted());
        }

    }

    @EventBusHook
    public void onEvent(CommandEvents.AutoSend autoSend) {
        autoSendLogFile(autoSend.formattedFileName);

        EventBus.getDefault().removeStickyEvent(CommandEvents.AutoSend.class);
    }

    @EventBusHook
    public void onEvent(CommandEvents.Annotate annotate) {
        final String desc = annotate.annotation;
        if (desc.length() == 0) {
            LOG.debug("Clearing annotation");
            session.clearDescription();
        } else {
            LOG.debug("Pending annotation: " + desc);
            session.setDescription(desc);
            EventBus.getDefault().post(new ServiceEvents.AnnotationStatus(false));

            if (session.isStarted()) {
                startGnssManager();
            } else {
                logOnce();
            }
        }

        EventBus.getDefault().removeStickyEvent(CommandEvents.Annotate.class);
    }

    @EventBusHook
    public void onEvent(CommandEvents.LogOnce logOnce) {
        logOnce();
    }

    @EventBusHook
    public void onEvent(ServiceEvents.ActivityRecognitionEvent activityRecognitionEvent) {

        session.setLatestDetectedActivity(activityRecognitionEvent.result.getMostProbableActivity());

        if (!preferenceHelper.shouldNotLogIfUserIsStill()) {
            session.setUserStillSinceTimeStamp(0);
            return;
        }

        if (activityRecognitionEvent.result.getMostProbableActivity().getType() == DetectedActivity.STILL) {
            LOG.debug(activityRecognitionEvent.result.getMostProbableActivity().toString());
            if (session.getUserStillSinceTimeStamp() == 0) {
                LOG.debug("Just entered still state, attempt to log");
                startGnssManager();
                session.setUserStillSinceTimeStamp(System.currentTimeMillis());
            }

        } else {
            LOG.debug(activityRecognitionEvent.result.getMostProbableActivity().toString());
            //Reset the still-since timestamp
            session.setUserStillSinceTimeStamp(0);
            LOG.debug("Just exited still state, attempt to log");
            startGnssManager();
        }
    }


    /**
     * Can be used from calling classes as the go-between for methods and
     * properties.
     */
    class GnssLoggingBinder extends Binder {
        public GnssLoggingService getService() {
            return GnssLoggingService.this;
        }
    }

}
