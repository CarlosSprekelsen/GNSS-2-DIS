package ets.acmi.gnssdislogger.loggers.customurl;

import android.location.Location;

import com.birbit.android.jobqueue.JobManager;

import java.net.URLEncoder;
import java.util.Date;

import ets.acmi.gnssdislogger.common.AppSettings;
import ets.acmi.gnssdislogger.common.PreferenceHelper;
import ets.acmi.gnssdislogger.common.SerializableLocation;
import ets.acmi.gnssdislogger.common.Session;
import ets.acmi.gnssdislogger.common.Strings;
import ets.acmi.gnssdislogger.common.Systems;
import ets.acmi.gnssdislogger.common.events.UploadEvents;
import ets.acmi.gnssdislogger.loggers.FileLogger;

public class CustomUrlLogger implements FileLogger {

    private final String customLoggingUrl;
    private final int batteryLevel;
    private final String httpMethod;
    private final String httpBody;
    private final String httpHeaders;
    private final String basicAuthUsername;
    private final String basicAuthPassword;

    public CustomUrlLogger(String customLoggingUrl, int batteryLevel, String httpMethod, String httpBody, String httpHeaders) {
        this(customLoggingUrl, batteryLevel, httpMethod, httpBody, httpHeaders, "", "");

    }

    public CustomUrlLogger(String customLoggingUrl, int batteryLevel, String httpMethod, String httpBody, String httpHeaders, String basicAuthUsername, String basicAuthPassword) {
        this.customLoggingUrl = customLoggingUrl;
        this.batteryLevel = batteryLevel;
        this.httpMethod = httpMethod;
        this.httpBody = httpBody;
        this.httpHeaders = httpHeaders;
        this.basicAuthUsername = basicAuthUsername;
        this.basicAuthPassword = basicAuthPassword;
    }

    @Override
    public void write(Location loc) throws Exception {
        if (!Session.getInstance().hasDescription()) {
            annotate("", loc);
        }
    }

    @Override
    public void annotate(String description, Location loc) throws Exception {

        String finalUrl = getFormattedTextBlock(customLoggingUrl, loc, description, Systems.getAndroidId(), batteryLevel, Strings.getBuildSerial(),
                Session.getInstance().getStartTimeStamp(), Session.getInstance().getCurrentFormattedFileName(), PreferenceHelper.getInstance().getCurrentProfileName(), Session.getInstance().getTotalTravelled());
        String finalBody = getFormattedTextBlock(httpBody, loc, description, Systems.getAndroidId(), batteryLevel, Strings.getBuildSerial(),
                Session.getInstance().getStartTimeStamp(), Session.getInstance().getCurrentFormattedFileName(), PreferenceHelper.getInstance().getCurrentProfileName(), Session.getInstance().getTotalTravelled());
        String finalHeaders = getFormattedTextBlock(httpHeaders, loc, description, Systems.getAndroidId(), batteryLevel, Strings.getBuildSerial(),
                Session.getInstance().getStartTimeStamp(), Session.getInstance().getCurrentFormattedFileName(), PreferenceHelper.getInstance().getCurrentProfileName(), Session.getInstance().getTotalTravelled());


        JobManager jobManager = AppSettings.getJobManager();
        jobManager.addJobInBackground(new CustomUrlJob(new CustomUrlRequest(finalUrl, httpMethod, finalBody, finalHeaders, basicAuthUsername, basicAuthPassword), new UploadEvents.CustomUrl()));
    }


    private String getFormattedTextBlock(String customLoggingUrl, Location loc, String description, String androidId,
                                         float batteryLevel, String buildSerial, long sessionStartTimeStamp, String fileName, String profileName, double distance)
            throws Exception {

        String logUrl = customLoggingUrl;
        SerializableLocation sLoc = new SerializableLocation(loc);
        logUrl = logUrl.replaceAll("(?i)%lat", String.valueOf(sLoc.getLatitude()));
        logUrl = logUrl.replaceAll("(?i)%lon", String.valueOf(sLoc.getLongitude()));
        logUrl = logUrl.replaceAll("(?i)%sat", String.valueOf(sLoc.getSatelliteCount()));
        logUrl = logUrl.replaceAll("(?i)%desc", String.valueOf(URLEncoder.encode(Strings.htmlDecode(description), "UTF-8")));
        logUrl = logUrl.replaceAll("(?i)%alt", String.valueOf(sLoc.getAltitude()));
        logUrl = logUrl.replaceAll("(?i)%acc", String.valueOf(sLoc.getAccuracy()));
        logUrl = logUrl.replaceAll("(?i)%dir", String.valueOf(sLoc.getBearing()));
        logUrl = logUrl.replaceAll("(?i)%prov", String.valueOf(sLoc.getProvider()));
        logUrl = logUrl.replaceAll("(?i)%spd", String.valueOf(sLoc.getSpeed()));
        logUrl = logUrl.replaceAll("(?i)%timestamp", String.valueOf(sLoc.getTime() / 1000));
        logUrl = logUrl.replaceAll("(?i)%time", Strings.getIsoDateTime(new Date(sLoc.getTime())));
        logUrl = logUrl.replaceAll("(?i)%date", Strings.getIsoCalendarDate(new Date(sLoc.getTime())));
        logUrl = logUrl.replaceAll("(?i)%starttimestamp", String.valueOf(sessionStartTimeStamp / 1000));
        logUrl = logUrl.replaceAll("(?i)%batt", String.valueOf(batteryLevel));
        logUrl = logUrl.replaceAll("(?i)%aid", String.valueOf(androidId));
        logUrl = logUrl.replaceAll("(?i)%ser", String.valueOf(buildSerial));
        logUrl = logUrl.replaceAll("(?i)%act", String.valueOf(sLoc.getDetectedActivity()));
        logUrl = logUrl.replaceAll("(?i)%filename", fileName);
        logUrl = logUrl.replaceAll("(?i)%profile", URLEncoder.encode(profileName, "UTF-8"));
        logUrl = logUrl.replaceAll("(?i)%hdop", sLoc.getHDOP());
        logUrl = logUrl.replaceAll("(?i)%vdop", sLoc.getVDOP());
        logUrl = logUrl.replaceAll("(?i)%pdop", sLoc.getPDOP());
        logUrl = logUrl.replaceAll("(?i)%dist", String.valueOf((int) distance));

        return logUrl;
    }


    @Override
    public String getName() {
        return "URL";
    }
}


