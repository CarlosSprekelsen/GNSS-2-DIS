package ets.acmi.gnssdislogger.ui.fragments.views;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.text.HtmlCompat;

import org.slf4j.Logger;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import de.greenrobot.event.EventBus;
import ets.acmi.gnssdislogger.R;
import ets.acmi.gnssdislogger.common.EventBusHook;
import ets.acmi.gnssdislogger.common.PreferenceHelper;
import ets.acmi.gnssdislogger.common.Session;
import ets.acmi.gnssdislogger.common.Strings;
import ets.acmi.gnssdislogger.common.events.CommandEvents;
import ets.acmi.gnssdislogger.common.events.ServiceEvents;
import ets.acmi.gnssdislogger.common.slf4j.Logs;
import ets.acmi.gnssdislogger.common.slf4j.SessionLogcatAppender;
import ets.acmi.gnssdislogger.loggers.FileLogger;
import ets.acmi.gnssdislogger.loggers.FileLoggerFactory;
import ets.acmi.gnssdislogger.ui.components.InteractiveScrollView;


public class FragmentLogView extends FragmentGenericView implements View.OnClickListener {

    private static final Logger LOG = Logs.of(FragmentLogView.class);
    private Context context;
    private final PreferenceHelper preferenceHelper = PreferenceHelper.getInstance();
    private final Session session = Session.getInstance();

    private View rootView;

    private TextView logTextView;
    private InteractiveScrollView scrollView;

    private long startTime = 0;
    private final Handler timerHandler = new Handler();

    private boolean doAutomaticScroll = true;
    private final Runnable timerRunnable = new Runnable() {

        @Override
        public void run() {
            showLogcatMessages();
            timerHandler.postDelayed(this, 1500);
        }
    };


    public FragmentLogView() {
    }

    public static FragmentLogView newInstance() {

        FragmentLogView fragment = new FragmentLogView();
        Bundle bundle = new Bundle(1);
        bundle.putInt("a_number", 1);

        fragment.setArguments(bundle);
        return fragment;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_log_view, container, false);

        logTextView = rootView.findViewById(R.id.logview_txtstatus);
        scrollView = rootView.findViewById(R.id.logview_scrollView);
        scrollView.setOnScrolledUpListener(scrollY -> doAutomaticScroll = false);
        scrollView.setOnBottomReachedListener(scrollY -> doAutomaticScroll = true);

        if (getActivity() != null) {
            this.context = getActivity().getApplicationContext();
        }

        setImageTooltips();
        showPreferencesSummary();

        if (session.hasValidLocation()) {
            displayLocationInfo(session.getCurrentLocationInfo());
        }

        return rootView;
    }


    private void showPreferencesSummary() {
        showCurrentFileName(Strings.getFormattedFileName());

        TextView txtLoggingTo = rootView.findViewById(R.id.loggingto_text);

        setDISinformation();

        List<FileLogger> loggers = FileLoggerFactory.getFileLoggers(getActivity().getApplicationContext());

        if (loggers.size() > 0) {

            StringBuilder enabledLoggers = new StringBuilder();

            for (FileLogger l : loggers) {
                if (!Strings.isNullOrEmpty(l.getName())) {
                    enabledLoggers.append(l.getName()).append(" ");
                }
            }

            if (preferenceHelper.shouldLogToNmea()) {
                enabledLoggers.append("NMEA ");
            }

            txtLoggingTo.setText(enabledLoggers.toString());

        } else {
            txtLoggingTo.setText(R.string.summary_loggingto_screen);
        }
    }

    private void showCurrentFileName(String newFileName) {
        TextView txtFilename = rootView.findViewById(R.id.simpleview_txtfilepath);

        txtFilename.setVisibility(View.VISIBLE);
        txtFilename.setTextIsSelectable(true);
        txtFilename.setSelectAllOnFocus(true);

        txtFilename.setText(
                HtmlCompat.fromHtml(preferenceHelper.getGpsLoggerFolder() + "<br /><strong>" + Strings.getFormattedFileName() + "</strong>", HtmlCompat.FROM_HTML_MODE_LEGACY));
    }

    private void clearColor(ImageView imgView) {
        setColor(imgView, IconColorIndicator.Inactive);
    }

    private void setColor(ImageView imgView, IconColorIndicator colorIndicator) {
        imgView.clearColorFilter();

        if (colorIndicator == IconColorIndicator.Inactive) {
            return;
        }

        int color = -1;
        switch (colorIndicator) {
            case Bad:
                color = Color.parseColor("#FFEEEE");
                break;
            case Good:
                color = ContextCompat.getColor(context, R.color.accentColor);
                break;
            case Warning:
                color = Color.parseColor("#D4FFA300");
                break;
        }

        imgView.setColorFilter(color);

    }

    private void setImageTooltips() {
        TextView latitude = rootView.findViewById(R.id.simple_lat_text);
        latitude.setOnClickListener(this);

        TextView longitude = rootView.findViewById(R.id.simple_lon_text);
        longitude.setOnClickListener(this);

        TextView altitude = rootView.findViewById(R.id.simpleview_txtAltitude);
        altitude.setOnClickListener(this);

        LinearLayout dissettings = rootView.findViewById(R.id.dis_info);
        dissettings.setOnClickListener(this);

        ImageView imgSatellites = rootView.findViewById(R.id.simpleview_imgSatelliteCount);
        imgSatellites.setOnClickListener(this);

        ImageView imgAccuracy = rootView.findViewById(R.id.simpleview_imgAccuracy);
        imgAccuracy.setOnClickListener(this);

        ImageView imgBearing = rootView.findViewById(R.id.simpleview_imgDirection);
        imgBearing.setOnClickListener(this);

        ImageView imgDuration = rootView.findViewById(R.id.simpleview_imgDuration);
        imgDuration.setOnClickListener(this);

        ImageView imgSpeed = rootView.findViewById(R.id.simpleview_imgSpeed);
        imgSpeed.setOnClickListener(this);

        ImageView imgDistance = rootView.findViewById(R.id.simpleview_imgDistance);
        imgDistance.setOnClickListener(this);

        ImageView imgPoints = rootView.findViewById(R.id.simpleview_imgPoints);
        imgPoints.setOnClickListener(this);

    }

    private void showLogcatMessages() {
        CheckBox chkLocationsOnly = rootView.findViewById(R.id.logview_chkLocationsOnly);

        StringBuilder sb = new StringBuilder();
        for (ILoggingEvent message : SessionLogcatAppender.Statuses) {

            if (message.getMarker() == SessionLogcatAppender.MARKER_LOCATION) {
                sb.append(getFormattedMessage(message.getMessage(), R.color.accentColorComplementary, message.getTimeStamp()));
            } else if (!chkLocationsOnly.isChecked()) {
                if (message.getLevel() == Level.ERROR) {
                    sb.append(getFormattedMessage(message.getMessage(), R.color.errorColor, message.getTimeStamp()));

                } else if (message.getLevel() == Level.WARN) {
                    sb.append(getFormattedMessage(message.getMessage(), R.color.warningColor, message.getTimeStamp()));

                } else {
                    sb.append(getFormattedMessage(message.getMessage(), R.color.secondaryColorText, message.getTimeStamp()));
                }
            }

        }
        logTextView.setText(HtmlCompat.fromHtml(sb.toString(), HtmlCompat.FROM_HTML_MODE_LEGACY));

        if (doAutomaticScroll) {
            scrollView.fullScroll(View.FOCUS_DOWN);
        }

    }

    private String getFormattedMessage(String message, int colorResourceId, long timeStamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        String dateStamp = sdf.format(new Date(timeStamp)) + " ";

        String messageFormat = "%s<font color='#%s'>%s</font><br />";

        return String.format(messageFormat,
                dateStamp,
                Integer.toHexString(ContextCompat.getColor(rootView.getContext(), colorResourceId)).substring(2), message);

    }


    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        showPreferencesSummary();
        startTime = System.currentTimeMillis();
        timerHandler.postDelayed(timerRunnable, 0);
    }

    @Override
    public void onPause() {
        super.onPause();
        timerHandler.removeCallbacks(timerRunnable);
    }

    @EventBusHook
    public void onEventMainThread(ServiceEvents.LocationUpdate locationUpdate) {
        displayLocationInfo(locationUpdate.location);
    }

    @EventBusHook
    public void onEventMainThread(ServiceEvents.SatellitesCount satellites) {
        setSatellitesCount(satellites.satellitesInView,satellites.satellitesInFix);
    }

    @EventBusHook
    public void onEventMainThread(ServiceEvents.WaitingForLocation waitingForLocation) {
        onWaitingForLocation(waitingForLocation.waiting);
    }

    @EventBusHook
    public void onEventMainThread(ServiceEvents.LoggingStatus loggingStatus) {

        if (loggingStatus.loggingStarted) {
            showPreferencesSummary();
            clearLocationDisplay();
        } else {
            setSatellitesCount(-1,0);
        }
    }

    @EventBusHook
    public void onEventMainThread(ServiceEvents.FileNamed fileNamed) {
        showCurrentFileName(fileNamed.newFileName);
    }

    @SuppressLint("SetTextI18n")
    private void displayLocationInfo(Location locationInfo) {
        showPreferencesSummary();


        TextView txtLatitude = rootView.findViewById(R.id.simple_lat_text);
        txtLatitude.setText(Strings.getFormattedLatitude(locationInfo.getLatitude()));

        TextView txtLongitude = rootView.findViewById(R.id.simple_lon_text);
        txtLongitude.setText(Strings.getFormattedLongitude(locationInfo.getLongitude()));

        ImageView imgAccuracy = rootView.findViewById(R.id.simpleview_imgAccuracy);
        clearColor(imgAccuracy);

        if (locationInfo.hasAccuracy()) {

            TextView txtAccuracy = rootView.findViewById(R.id.simpleview_txtAccuracy);
            float accuracy = locationInfo.getAccuracy();
            txtAccuracy.setText(Strings.getDistanceDisplay(getActivity(), accuracy, preferenceHelper.shouldDisplayImperialUnits(), true));

            if (accuracy > 500) {
                setColor(imgAccuracy, IconColorIndicator.Warning);
            }

            if (accuracy > 900) {
                setColor(imgAccuracy, IconColorIndicator.Bad);
            } else {
                setColor(imgAccuracy, IconColorIndicator.Good);
            }
        }


        if (locationInfo.hasAltitude()) {
            TextView txtAltitude = rootView.findViewById(R.id.simpleview_txtAltitude);
            txtAltitude.setText(Strings.getDistanceDisplay(getActivity(), locationInfo.getAltitude(), preferenceHelper.shouldDisplayImperialUnits(), false));
        }

        ImageView imgSpeed = rootView.findViewById(R.id.simpleview_imgSpeed);
        clearColor(imgSpeed);

        if (locationInfo.hasSpeed()) {

            setColor(imgSpeed, IconColorIndicator.Good);

            TextView txtSpeed = rootView.findViewById(R.id.simpleview_txtSpeed);
            txtSpeed.setText(Strings.getSpeedDisplay(getActivity(), locationInfo.getSpeed(), preferenceHelper.shouldDisplayImperialUnits()));
        }

        ImageView imgDirection = rootView.findViewById(R.id.simpleview_imgDirection);
        clearColor(imgDirection);

        if (locationInfo.hasBearing()) {
            setColor(imgDirection, IconColorIndicator.Good);
            imgDirection.setRotation(locationInfo.getBearing());

            TextView txtDirection = rootView.findViewById(R.id.simpleview_txtDirection);
            txtDirection.setText(Math.round(locationInfo.getBearing()) + getString(R.string.degree_symbol));
        }

        TextView txtDuration = rootView.findViewById(R.id.simpleview_txtDuration);

        long startTime = session.getStartTimeStamp();
        long currentTime = System.currentTimeMillis();

        txtDuration.setText(Strings.getTimeDisplay(getActivity(), currentTime - startTime));

        double distanceValue = session.getTotalTravelled();

        TextView txtPoints = rootView.findViewById(R.id.simpleview_txtPoints);
        TextView txtTravelled = rootView.findViewById(R.id.simpleview_txtDistance);

        txtTravelled.setText(Strings.getDistanceDisplay(getActivity(), distanceValue, preferenceHelper.shouldDisplayImperialUnits(), true));
        txtPoints.setText(session.getNumLegs() + " " + getString(R.string.points));

        TextView txtFrequency = rootView.findViewById(R.id.detailedview_frequency_text);
        if (preferenceHelper.getMinimumLoggingInterval() > 0) {
            String descriptiveTime = Strings.getDescriptiveDurationString(preferenceHelper.getMinimumLoggingInterval(),
                    getActivity().getApplicationContext());

            txtFrequency.setText(descriptiveTime);
        } else {
            txtFrequency.setText(R.string.summary_freq_max);

        }

        String providerName = locationInfo.getProvider();
        if (!providerName.equalsIgnoreCase(LocationManager.GPS_PROVIDER)) {
            setSatellitesCount(-1,0);
        }
    }

    private void clearLocationDisplay() {

        // DIS info management
        setDISinformation();

        TextView txtLatitude = rootView.findViewById(R.id.simple_lat_text);
        txtLatitude.setText("");

        TextView txtLongitude = rootView.findViewById(R.id.simple_lon_text);
        txtLongitude.setText("");

        TextView txtAltitude = rootView.findViewById(R.id.simpleview_txtAltitude);
        txtAltitude.setText("");

        ImageView imgAccuracy = rootView.findViewById(R.id.simpleview_imgAccuracy);
        clearColor(imgAccuracy);

        TextView txtAccuracy = rootView.findViewById(R.id.simpleview_txtAccuracy);
        txtAccuracy.setText("");
        txtAccuracy.setTextColor(ContextCompat.getColor(context, android.R.color.black));

        ImageView imgDirection = rootView.findViewById(R.id.simpleview_imgDirection);
        clearColor(imgDirection);

        TextView txtDirection = rootView.findViewById(R.id.simpleview_txtDirection);
        txtDirection.setText("");

        ImageView imgSpeed = rootView.findViewById(R.id.simpleview_imgSpeed);
        clearColor(imgSpeed);

        TextView txtSpeed = rootView.findViewById(R.id.simpleview_txtSpeed);
        txtSpeed.setText("");

        TextView txtDuration = rootView.findViewById(R.id.simpleview_txtDuration);
        txtDuration.setText("");

        TextView txtPoints = rootView.findViewById(R.id.simpleview_txtPoints);
        TextView txtTravelled = rootView.findViewById(R.id.simpleview_txtDistance);

        txtPoints.setText("");
        txtTravelled.setText("");
    }

    private void setSatellitesCount(int satellitesInView, int satellitesInFix) {
        ImageView imgSatelliteCount = rootView.findViewById(R.id.simpleview_imgSatelliteCount);
        TextView txtSatelliteCount = rootView.findViewById(R.id.simpleview_txtSatelliteCount);

        if (satellitesInView > -1) {
            setColor(imgSatelliteCount, IconColorIndicator.Good);

            AlphaAnimation fadeIn = new AlphaAnimation(0.6f, 1.0f);
            fadeIn.setDuration(1200);
            fadeIn.setFillAfter(true);
            txtSatelliteCount.startAnimation(fadeIn);
            txtSatelliteCount.setText(String.format("%s/%s",satellitesInFix, satellitesInView));
        } else {
            clearColor(imgSatelliteCount);
            txtSatelliteCount.setText("");
        }

    }

    private void onWaitingForLocation(boolean inProgress) {

        LOG.debug(inProgress + "");

    }

    @Override
    public void onClick(View view) {
        Toast toast = new Toast(getActivity());
        switch (view.getId()) {
            case R.id.dis_info:
                EventBus.getDefault().post(new CommandEvents.MenuOptionClicked(R.id.mnuDisSettings));
                return;
            case R.id.simple_lat_text:
                toast = getToast(R.string.txt_latitude);
                break;
            case R.id.simple_lon_text:
                toast = getToast(R.string.txt_longitude);
                break;
            case R.id.simpleview_txtAltitude:
                toast = getToast(R.string.txt_altitude);
                break;
            case R.id.simpleview_imgSatelliteCount:
                toast = getToast(R.string.txt_satellites);
                break;
            case R.id.simpleview_imgAccuracy:
                toast = getToast(R.string.txt_accuracy);
                break;

            case R.id.simpleview_imgDirection:
                toast = getToast(R.string.txt_direction);
                break;

            case R.id.simpleview_imgDuration:
                toast = getToast(R.string.txt_travel_duration);
                break;

            case R.id.simpleview_imgSpeed:
                toast = getToast(R.string.txt_speed);
                break;

            case R.id.simpleview_imgDistance:
                toast = getToast(R.string.txt_travel_distance);
                break;

            case R.id.simpleview_imgPoints:
                toast = getToast(R.string.txt_number_of_points);
                break;

        }

        int[] location = new int[2];
        view.getLocationOnScreen(location);
        toast.setGravity(Gravity.TOP | Gravity.START, location[0], location[1]);
        toast.show();
    }

    private Toast getToast(String message) {
        return Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT);
    }

    private Toast getToast(int stringResourceId) {
        return getToast(getString(stringResourceId).replace(":", ""));
    }

    private void setDISinformation() {
        //ImageView imgEntity = rootView.findViewById(R.id.simpleview_imgEnt);
        TextView txtCallsign = rootView.findViewById(R.id.simple_callsign_text);
        txtCallsign.setText(preferenceHelper.getDISMarking());

        Drawable drawable = ContextCompat.getDrawable(context, preferenceHelper.getDisDrawable());

        assert drawable != null;
        drawable = DrawableCompat.wrap(drawable);
        drawable.setTint(preferenceHelper.getDisTint());

        txtCallsign.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, null, drawable);

        if (preferenceHelper.shouldLogToDIS()) {
            txtCallsign.setVisibility(View.VISIBLE);
        } else {
            txtCallsign.setVisibility(View.GONE);
        }
    }

    private enum IconColorIndicator {
        Good,
        Warning,
        Bad,
        Inactive
    }
}