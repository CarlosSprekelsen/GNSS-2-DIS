package ets.acmi.gnssdislogger.ui.activity;


import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.greenrobot.event.EventBus;
import ets.acmi.gnssdislogger.R;
import ets.acmi.gnssdislogger.common.EventBusHook;
import ets.acmi.gnssdislogger.common.PreferenceHelper;
import ets.acmi.gnssdislogger.common.Session;
import ets.acmi.gnssdislogger.common.Systems;
import ets.acmi.gnssdislogger.common.events.CommandEvents;
import ets.acmi.gnssdislogger.common.events.ServiceEvents;
import ets.acmi.gnssdislogger.common.events.UploadEvents;
import ets.acmi.gnssdislogger.common.slf4j.Logs;
import ets.acmi.gnssdislogger.common.slf4j.SessionLogcatAppender;
import ets.acmi.gnssdislogger.listeners.GnssLoggingService;
import ets.acmi.gnssdislogger.loggers.Files;
import ets.acmi.gnssdislogger.senders.FileSender;
import ets.acmi.gnssdislogger.senders.FileSenderFactory;
import ets.acmi.gnssdislogger.ui.Dialogs;
import ets.acmi.gnssdislogger.ui.fragments.views.BottomNavigationDrawer;
import ets.acmi.gnssdislogger.ui.fragments.views.FragmentGenericView;
import ets.acmi.gnssdislogger.ui.fragments.views.FragmentLogView;
import ets.acmi.gnssdislogger.ui.fragments.views.FragmentMapsView;

public class GnssMainActivity extends AppCompatActivity {
    private static final Logger LOG = Logs.of(GnssMainActivity.class);
    private static boolean userInvokedUpload;
    private static Intent serviceIntent;
    private FloatingActionButton actionButton;
    private final PreferenceHelper preferenceHelper = PreferenceHelper.getInstance();
    private final Session session = Session.getInstance();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        loadPresetProperties();
        loadVersionSpecificProperties();
        Systems.setLocale(preferenceHelper.getUserSpecifiedLocale(), getBaseContext(), getResources());


        if (!Systems.hasUserGrantedAllNecessaryPermissions(this)) {
            Systems.askUserForPermissions(this, null);
        } else {
            LOG.debug("Permission check OK");

            if (preferenceHelper.shouldStartLoggingOnAppLaunch()) {
                LOG.debug("Start logging on app launch");
                EventBus.getDefault().postSticky(new CommandEvents.RequestStartStop(true));
            }
        }

        setContentView(R.layout.activity_main);
        BottomAppBar bar = findViewById(R.id.bottom_app_bar);
        setSupportActionBar(bar);


        actionButton = findViewById(R.id.fab);

        actionButton.setOnClickListener(v -> {
            requestToggleLogging();
            if (session.isStarted()) {
                setActionButtonStart();
            } else {
                setActionButtonStop();
            }
        });

        startAndBindService();
        registerEventBus();
        loadViews();


        if (session.isStarted()) {
            setActionButtonStart();
        } else {
            setActionButtonStop();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.bottomappbar_menu, menu);
        enableDisableMenuItems();
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NotNull String[] permissions, @NotNull int[] grantResults) {
        Systems.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        LOG.debug("Bottom Menu Item: " + item.getTitle());

        switch (id) {
            case android.R.id.home:
                BottomNavigationDrawer bottomNavDrawerFragment = new BottomNavigationDrawer();
                bottomNavDrawerFragment.show(getSupportFragmentManager(), BottomNavigationDrawer.TAG);
                return true;
            case R.id.mnuAnnotate:
                annotate();
                return true;
            case R.id.mnuViewLog:
                toggleViews();
                return true;
            case R.id.mnuDisSettings:
                loadDisSettings();
                return true;
            case R.id.mnuShare:
                share();
                return true;
            case R.id.mnuAutoSendNow:
                forceAutoSendNow();
                return true;
            case R.id.mnuFtp:
                sendToFtp();
                return true;
            case R.id.mnuSFTP:
                sendToSftp();
                return true;
            default:
                return true;
        }
    }


    @Override
    protected void onStart() {
        super.onStart();
        startAndBindService();
    }

    @Override
    protected void onResume() {
        super.onResume();
        startAndBindService();

        if (session.hasDescription()) {
            setAnnotationReady();
        }

        //populateProfilesList();
        enableDisableMenuItems();
    }

    @Override
    protected void onPause() {
        stopAndUnbindServiceIfRequired();
        super.onPause();
    }

    protected void onStop() {
        super.onStop();
        if (!isFinishing()) {
            stopAndUnbindServiceIfRequired();
        }
    }

    @Override
    protected void onDestroy() {
        stopAndUnbindServiceIfRequired();
        unregisterEventBus();
        super.onDestroy();

    }

    /**
     * Provides a connection to the GPS Logging Service
     */
    private final ServiceConnection gpsServiceConnection = new ServiceConnection() {

        public void onServiceDisconnected(ComponentName name) {
            LOG.debug("Disconnected from GPSLoggingService from MainActivity");
            //loggingService = null;
        }

        public void onServiceConnected(ComponentName name, IBinder service) {
            LOG.debug("Connected to GPSLoggingService from MainActivity");
            //loggingService = ((GnssLoggingService.GpsLoggingBinder) service).getService();
        }
    };

    private void setActionButtonStart() {
        actionButton.setImageResource(android.R.drawable.ic_media_pause);
        actionButton.setBackgroundTintList(this.getResources().getColorStateList(R.color.accentColor, this.getTheme()));
        ConstraintLayout sessionStatusTopBar = findViewById(R.id.session_status);
        sessionStatusTopBar.setVisibility(View.VISIBLE);
    }

    private void setActionButtonStop() {
        actionButton.setImageResource(android.R.drawable.ic_media_play);
        actionButton.setBackgroundTintList(this.getResources().getColorStateList(R.color.accentColorComplementary, this.getTheme()));
        ConstraintLayout sessionStatusTopBar = findViewById(R.id.session_status);
        sessionStatusTopBar.setVisibility(View.INVISIBLE);
    }

    private void registerEventBus() {
        EventBus.getDefault().register(this);
    }

    private void unregisterEventBus() {
        try {
            EventBus.getDefault().unregister(this);
        } catch (Throwable t) {
            //this may crash if registration did not go through. just be safe
        }
    }


    private void loadVersionSpecificProperties() {
        PackageInfo packageInfo;
        try {
            packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            int versionCode = packageInfo.versionCode;

            if (preferenceHelper.getLastVersionSeen() <= 74) {
                LOG.debug("preferenceHelper.getLastVersionSeen() " + preferenceHelper.getLastVersionSeen());
                LOG.debug("Overriding minimum accuracy to 40");

                if (preferenceHelper.getMinimumAccuracy() == 0) {
                    preferenceHelper.setMinimumAccuracy(40);
                }
            }

            preferenceHelper.setLastVersionSeen(versionCode);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

    }


    private void loadPresetProperties() {

        //Either look for /<appfolder>/ets.acmi.gnssdislogger.properties or /sdcard/ets.acmi.gnssdislogger.properties
        File file = new File(Files.storageFolder(getApplicationContext()) + "/ets.acmi.gnssdislogger.properties");
        if (!file.exists()) {
            file = new File(Environment.getExternalStorageDirectory() + "/ets.acmi.gnssdislogger.properties");
            if (!file.exists()) {
                return;
            }
        }

        try {
            LOG.warn("ets.acmi.gnssdislogger.properties found, setting app preferences");
            preferenceHelper.setPreferenceFromPropertiesFile(file);
        } catch (Exception e) {
            LOG.error("Could not load preset properties", e);
        }
    }

    private void loadViews() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        if (session.showMapFragment()) {
            transaction.replace(R.id.view_container, FragmentMapsView.newInstance());
        } else {
            transaction.replace(R.id.view_container, FragmentLogView.newInstance());
        }
        transaction.commitAllowingStateLoss();
    }


    private FragmentGenericView getCurrentFragment() {

        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.view_container);
        if (currentFragment instanceof FragmentGenericView) {
            return ((FragmentGenericView) currentFragment);
        }
        return null;
    }

    private void enableDisableMenuItems() {

        onWaitingForLocation(session.isWaitingForLocation());

        BottomAppBar toolbar = findViewById(R.id.bottom_app_bar);
        MenuItem mnuAnnotate = toolbar.getMenu().findItem(R.id.mnuAnnotate);
        MenuItem mnuViewLog = toolbar.getMenu().findItem(R.id.mnuViewLog);
        MenuItem mnuDisSettings = toolbar.getMenu().findItem(R.id.mnuDisSettings);
        MenuItem mnuAutoSendNow = toolbar.getMenu().findItem(R.id.mnuAutoSendNow);

        if (session.showMapFragment()) {
            mnuViewLog.setIcon(R.drawable.logview_off);
        } else {
            mnuViewLog.setIcon(R.drawable.logview_on);
        }

        if (mnuDisSettings != null) {
            mnuDisSettings.setIcon(preferenceHelper.getDisDrawable());
            mnuDisSettings.getIcon().setTint(Color.WHITE);
            mnuDisSettings.setVisible(preferenceHelper.shouldLogToDIS());
        }

        if (mnuAutoSendNow != null) {
            mnuAutoSendNow.setEnabled(session.isStarted());
        }

        if (mnuAnnotate != null) {

            if (!preferenceHelper.shouldLogToCSV() && !preferenceHelper.shouldLogToGpx()
                    && !preferenceHelper.shouldLogToKml() && !preferenceHelper.shouldLogToCustomUrl()
                    && !preferenceHelper.shouldLogToGeoJSON()
                    && !preferenceHelper.shouldLogToDIS()
                    && !preferenceHelper.shouldLogToEag()) {
                mnuAnnotate.setIcon(R.drawable.annotate2_disabled);
                mnuAnnotate.setEnabled(false);
            } else {
                if (session.isAnnotationMarked()) {
                    mnuAnnotate.setIcon(R.drawable.annotate2_active);
                } else {
                    mnuAnnotate.setIcon(R.drawable.annotate2);
                }
            }

        }
    }

    private void toggleViews() {
        session.setLogviewMarked(!session.showMapFragment());
        loadViews();
    }

    private void forceAutoSendNow() {
        LOG.debug("User forced an auto send");

        if (preferenceHelper.isAutoSendEnabled()) {
            Dialogs.progress(this, getString(R.string.autoUpload_sending), getString(R.string.please_wait));
            EventBus.getDefault().post(new CommandEvents.AutoSend(null));

        } else {
            //TODO: Message to configure Autosend
            launchPreferenceScreen(MainPreferenceActivity.PREFERENCE_FRAGMENTS.UPLOAD);
        }
    }

    private void loadDisSettings() {
        launchPreferenceScreen(MainPreferenceActivity.PREFERENCE_FRAGMENTS.DIS);
    }

    /**
     * Annotates GPX and KML files, TXT files are ignored.
     * The user is prompted for the content of the <name> tag. If a valid
     * description is given, the logging service starts in single point mode.
     */
    private void annotate() {

        if (!preferenceHelper.shouldLogToCSV()
                && !preferenceHelper.shouldLogToEag()
                && !preferenceHelper.shouldLogToDIS()
                && !preferenceHelper.shouldLogToGpx()
                && !preferenceHelper.shouldLogToKml()
                && !preferenceHelper.shouldLogToCustomUrl()
                && !preferenceHelper.shouldLogToGeoJSON()) {
            Toast.makeText(getApplicationContext(), getString(R.string.annotation_requires_logging), Toast.LENGTH_SHORT).show();
            return;
        }

        Dialogs.autoCompleteText(GnssMainActivity.this, "annotations",
                getString(R.string.add_description), getString(R.string.letters_numbers), "",
                (which, dialog, enteredText) -> {
                    if (which == Dialogs.AutoCompleteCallback.CANCEL) {
                        return;
                    }

                    LOG.info("Annotation entered : " + enteredText);
                    EventBus.getDefault().post(new CommandEvents.Annotate(enteredText));
                });

    }

    private void sendToSftp() {
        if (!FileSenderFactory.getSFTPSender().isAvailable()) {
            launchPreferenceScreen(MainPreferenceActivity.PREFERENCE_FRAGMENTS.SFTP);
            return;
        }
        showFileListDialog(FileSenderFactory.getSFTPSender());
    }

    private void sendToFtp() {
        if (!FileSenderFactory.getFtpSender().isAvailable()) {
            launchPreferenceScreen(MainPreferenceActivity.PREFERENCE_FRAGMENTS.FTP);
        } else {
            showFileListDialog(FileSenderFactory.getFtpSender());
        }
    }

    private void showFileListDialog(final FileSender sender) {

        if (!Systems.isNetworkAvailable(this)) {
            Dialogs.alert(getString(R.string.sorry), getString(R.string.no_network_message), this);
            return;
        }

        final File gpxFolder = new File(preferenceHelper.getGpsLoggerFolder());

        if (gpxFolder.exists() && Files.fromFolder(gpxFolder, sender).length > 0) {
            File[] enumeratedFiles = Files.fromFolder(gpxFolder, sender);

            //Order by last modified
            Arrays.sort(enumeratedFiles, (f1, f2) -> {
                if (f1 != null && f2 != null) {
                    return -1 * Long.compare(f1.lastModified(), f2.lastModified());
                }
                return -1;
            });

            List<String> fileList = new ArrayList<>(enumeratedFiles.length);

            for (File f : enumeratedFiles) {
                fileList.add(f.getName());
            }

            final String[] files = fileList.toArray(new String[0]);

            new MaterialDialog.Builder(this)
                    .title(R.string.osm_pick_file)
                    .items(files)
                    .positiveText(R.string.ok)
                    .itemsCallbackMultiChoice(null, (materialDialog, integers, charSequences) -> {

                        List<File> chosenFiles = new ArrayList<>();

                        for (Object item : integers) {
                            LOG.info("Selected file to upload- " + files[Integer.parseInt(item.toString())]);
                            chosenFiles.add(new File(gpxFolder, files[Integer.parseInt(item.toString())]));
                        }

                        if (chosenFiles.size() > 0) {
                            Dialogs.progress(GnssMainActivity.this, getString(R.string.please_wait), getString(R.string.please_wait));
                            userInvokedUpload = true;
                            sender.uploadFile(chosenFiles);

                        }
                        return true;
                    }).show();

        } else {
            Dialogs.alert(getString(R.string.sorry), getString(R.string.no_files_found), this);
        }
    }

    /**
     * Allows user to send a GPX/KML file along with location, or location only
     * using a provider. 'Provider' means any application that can accept such
     * an intent (Facebook, SMS, Twitter, Email, K-9, Bluetooth)
     */
    private void share() {

        try {

            final String locationOnly = getString(R.string.sharing_location_only);
            final File gpxFolder = new File(preferenceHelper.getGpsLoggerFolder());
            if (gpxFolder.exists()) {

                File[] enumeratedFiles = Files.fromFolder(gpxFolder);

                Arrays.sort(enumeratedFiles, (f1, f2) -> -1 * Long.compare(f1.lastModified(), f2.lastModified()));

                List<String> fileList = new ArrayList<>(enumeratedFiles.length);

                for (File f : enumeratedFiles) {
                    fileList.add(f.getName());
                }

                fileList.add(0, locationOnly);
                final String[] files = fileList.toArray(new String[0]);

                new MaterialDialog.Builder(this)
                        .title(R.string.osm_pick_file)
                        .items(files)
                        .positiveText(R.string.ok)
                        .itemsCallbackMultiChoice(null, (materialDialog, integers, charSequences) -> {
                            List<Integer> selectedItems = Arrays.asList(integers);

                            final Intent intent = new Intent(Intent.ACTION_SEND);
                            intent.setType("*/*");

                            if (selectedItems.size() <= 0) {
                                return false;
                            }

                            if (selectedItems.contains(0)) {

                                intent.setType("text/plain");

                                intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.sharing_mylocation));
                                if (session.hasValidLocation()) {
                                    String bodyText = String.format("http://maps.google.com/maps?q=%s,%s",
                                            String.valueOf(session.getCurrentLatitude()),
                                            String.valueOf(session.getCurrentLongitude()));
                                    intent.putExtra(Intent.EXTRA_TEXT, bodyText);
                                    intent.putExtra("sms_body", bodyText);
                                    startActivity(Intent.createChooser(intent, getString(R.string.sharing_via)));
                                }
                            } else {

                                intent.setAction(Intent.ACTION_SEND_MULTIPLE);
                                intent.putExtra(Intent.EXTRA_SUBJECT, "Here are some files.");
                                intent.setType("*/*");

                                ArrayList<Uri> chosenFiles = new ArrayList<>();

                                for (Object path : selectedItems) {
                                    File file = new File(gpxFolder, files[Integer.parseInt(path.toString())]);
                                    Uri providedUri = FileProvider.getUriForFile(getApplicationContext(),
                                            "ets.acmi.ets.acmi.gnssdislogger.fileprovider", file);
                                    chosenFiles.add(providedUri);
                                }

                                intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, chosenFiles);
                                startActivity(Intent.createChooser(intent, getString(R.string.sharing_via)));
                            }
                            return true;
                        }).show();


            } else {
                Dialogs.alert(getString(R.string.sorry), getString(R.string.no_files_found), this);
            }
        } catch (Exception ex) {
            LOG.error("Sharing problem", ex);
        }
    }

    /**
     * Starts the service and binds the activity to it.
     */
    private void startAndBindService() {
        serviceIntent = new Intent(this, GnssLoggingService.class);
        // Start the service in case it isn't already running
        startService(serviceIntent);

        // Now bind to service
        bindService(serviceIntent, gpsServiceConnection, Context.BIND_AUTO_CREATE);
        session.setBoundToService(true);
    }


    /**
     * Stops the service if it isn't logging. Also unbinds.
     */
    private void stopAndUnbindServiceIfRequired() {
        if (session.isBoundToService()) {

            try {
                unbindService(gpsServiceConnection);
                session.setBoundToService(false);
            } catch (Exception e) {
                LOG.warn(SessionLogcatAppender.MARKER_INTERNAL, "Could not unbind service", e);
            }
        }

        if (!session.isStarted()) {
            LOG.debug("Stopping the service");
            try {
                stopService(serviceIntent);
            } catch (Exception e) {
                LOG.error("Could not stop the service", e);
            }
        }
    }

    private void setAnnotationReady() {
        session.setAnnotationMarked(true);
        enableDisableMenuItems();
    }

    private void setAnnotationDone() {
        session.setAnnotationMarked(false);
        enableDisableMenuItems();
    }

    private void onWaitingForLocation(boolean inProgress) {
        ProgressBar fixBar = findViewById(R.id.progressBarGpsFix);
        fixBar.setVisibility(inProgress ? View.VISIBLE : View.INVISIBLE);

        ImageView bulb = findViewById(R.id.notification_bulb);
        bulb.setImageResource(inProgress ? R.drawable.gps_notfixed : R.drawable.gps_fixed);
    }


    @EventBusHook
    public void onEventMainThread(CommandEvents.MenuOptionClicked menuOption) {
        switch (menuOption.itemId) {
            case R.id.settings_general:
                launchPreferenceScreen(MainPreferenceActivity.PREFERENCE_FRAGMENTS.GENERAL);
                break;
            case R.id.setting_files:
                launchPreferenceScreen(MainPreferenceActivity.PREFERENCE_FRAGMENTS.LOGGING);
                break;
            case R.id.setting_live:
                launchPreferenceScreen(MainPreferenceActivity.PREFERENCE_FRAGMENTS.LIVE);
                break;
            case R.id.setting_performance:
                launchPreferenceScreen(MainPreferenceActivity.PREFERENCE_FRAGMENTS.PERFORMANCE);
                break;
            case R.id.setting_autosend:
                launchPreferenceScreen(MainPreferenceActivity.PREFERENCE_FRAGMENTS.UPLOAD);
                break;
            case R.id.exit_app:
                EventBus.getDefault().post(new CommandEvents.RequestStartStop(false));
                finish();
                break;
            case R.id.mnuDisSettings:
                loadDisSettings();
                break;
            case R.id.mnuFaq:
                Intent faqtivity = new Intent(getApplicationContext(), FaqActivity.class);
                startActivity(faqtivity);
                break;

        }
    }

    @EventBusHook
    public void onEventMainThread(UploadEvents.Ftp upload) {
        LOG.debug("FTP Event completed, success: " + upload.success);
        Dialogs.hideProgress();

        if (!upload.success) {
            LOG.error(getString(R.string.ftp_setup_title)
                    + "-"
                    + getString(R.string.upload_failure));
            if (userInvokedUpload) {
                Dialogs.error(getString(R.string.sorry), getString(R.string.upload_failure), upload.message, upload.throwable, this);
                userInvokedUpload = false;
            }
        }
    }

    @EventBusHook
    public void onEventMainThread(UploadEvents.SFTP upload) {

        LOG.debug("SFTP Event completed, success: " + upload.success);
        Dialogs.hideProgress();

        if (!upload.success) {
            LOG.error(getString(R.string.sftp_setup_title) + "- " + getString(R.string.upload_failure));
            if (userInvokedUpload) {
                Dialogs.error(getString(R.string.sorry), getString(R.string.upload_failure), upload.message, upload.throwable, this);
                userInvokedUpload = false;
            }
        }
    }

    @EventBusHook
    public void onEventMainThread(ServiceEvents.WaitingForLocation waitingForLocation) {
        onWaitingForLocation(waitingForLocation.waiting);
    }

    @EventBusHook
    public void onEventMainThread(ServiceEvents.AnnotationStatus annotationStatus) {
        if (annotationStatus.annotationWritten) {
            setAnnotationDone();
        } else {
            setAnnotationReady();
        }
    }

    @EventBusHook
    public void onEventMainThread(ServiceEvents.LoggingStatus loggingStatus) {
        enableDisableMenuItems();
    }


    @EventBusHook
    public void onEventMainThread(ServiceEvents.SatellitesCount satellites) {
        TextView txtSatelliteCount = findViewById(R.id.top_txtSatelliteCount);
        txtSatelliteCount.setText(String.format("%s/%s", satellites.satellitesInFix, satellites.satellitesInView));
    }


    private void requestToggleLogging() {

        if (!Systems.locationPermissionsGranted(GnssMainActivity.this)) {
            Dialogs.alert(getString(R.string.gnsslogger_permissions_rationale_title),
                    getString(R.string.gnsslogger_permissions_permanently_denied), GnssMainActivity.this);
            return;
        }

        if (session.isStarted()) {
            toggleLogging();
            return;
        }

        if (!Files.isAllowedToWriteTo(preferenceHelper.getGpsLoggerFolder())) {

            Dialogs.alert(getString(R.string.error), getString(R.string.pref_logging_file_no_permissions) + "<br />" + preferenceHelper.getGpsLoggerFolder(), GnssMainActivity.this);
            return;
        }

        if (preferenceHelper.shouldCreateCustomFile() && preferenceHelper.shouldAskCustomFileNameEachTime()) {
            Dialogs.autoCompleteText(GnssMainActivity.this, "customfilename",
                    getString(R.string.new_file_custom_title), "ets.acmi.gnssdislogger",
                    preferenceHelper.getCustomFileName(), new Dialogs.AutoCompleteCallback() {

                        @Override
                        public void messageBoxResult(int which, MaterialDialog dialog, String enteredText) {

                            if (which == Dialogs.AutoCompleteCallback.CANCEL) {
                                return;
                            }

                            String originalFileName = preferenceHelper.getCustomFileName();

                            if (!originalFileName.equalsIgnoreCase(enteredText)) {
                                preferenceHelper.setCustomFileName(enteredText);
                            }

                            toggleLogging();
                        }
                    });

        } else {
            toggleLogging();
        }
    }

    private void toggleLogging() {
        EventBus.getDefault().post(new CommandEvents.RequestToggle());
    }

    /**
     * Helper method, launches activity in a delayed handler, less stutter
     */
    private void launchPreferenceScreen(final String whichFragment) {
        new Handler().postDelayed(() -> {
            Intent targetActivity = new Intent(getApplicationContext(), MainPreferenceActivity.class);
            targetActivity.putExtra("preference_fragment", whichFragment);
            startActivity(targetActivity);
        }, 250);
    }


}
