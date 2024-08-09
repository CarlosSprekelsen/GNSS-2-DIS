package ets.acmi.gnssdislogger.common;


import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.preference.PreferenceManager;

import org.slf4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Properties;

import ets.acmi.gnssdislogger.R;
import ets.acmi.gnssdislogger.common.slf4j.Logs;
import ets.acmi.gnssdislogger.loggers.Files;

public class PreferenceHelper {

    private static final Logger LOG = Logs.of(PreferenceHelper.class);
    private static PreferenceHelper instance = null;
    private SharedPreferences prefs;

    /**
     * Use PreferenceHelper.getInstance()
     */
    private PreferenceHelper() {

    }

    public static PreferenceHelper getInstance() {
        if (instance == null) {
            instance = new PreferenceHelper();
            instance.prefs = PreferenceManager.getDefaultSharedPreferences(AppSettings.getInstance().getApplicationContext());
        }

        return instance;
    }


    /**
     * FTP Server name for auto send
     */
    @ProfilePreference(name = PreferenceNames.FTP_SERVER)
    public String getFtpServerName() {
        return prefs.getString(PreferenceNames.FTP_SERVER, "");
    }


    /**
     * FTP Port for auto send
     */
    @ProfilePreference(name = PreferenceNames.FTP_PORT)
    public int getFtpPort() {
        return Strings.toInt(prefs.getString(PreferenceNames.FTP_PORT, "21"), 21);
    }


    /**
     * FTP Username for auto send
     */
    @ProfilePreference(name = PreferenceNames.FTP_USERNAME)
    public String getFtpUsername() {
        return prefs.getString(PreferenceNames.FTP_USERNAME, "");
    }


    /**
     * FTP Password for auto send
     */
    @ProfilePreference(name = PreferenceNames.FTP_PASSWORD)
    public String getFtpPassword() {
        return prefs.getString(PreferenceNames.FTP_PASSWORD, "");
    }

    /**
     * Whether to use FTPS
     */
    @ProfilePreference(name = PreferenceNames.FTP_USE_FTPS)
    public boolean shouldFtpUseFtps() {
        return prefs.getBoolean(PreferenceNames.FTP_USE_FTPS, false);
    }


    /**
     * FTP protocol to use (SSL or TLS)
     */
    @ProfilePreference(name = PreferenceNames.FTP_SSLORTLS)
    public String getFtpProtocol() {
        return prefs.getString(PreferenceNames.FTP_SSLORTLS, "");
    }


    /**
     * Whether to use FTP Implicit mode for auto send
     */
    @ProfilePreference(name = PreferenceNames.FTP_IMPLICIT)
    public boolean isFtpImplicit() {
        return prefs.getBoolean(PreferenceNames.FTP_IMPLICIT, false);
    }


    /**
     * Whether to auto send to FTP target
     */
    @ProfilePreference(name = PreferenceNames.FTP_AUTO_SEND_ENABLED)
    public boolean isFtpAutoSendEnabled() {
        return prefs.getBoolean(PreferenceNames.FTP_AUTO_SEND_ENABLED, false);
    }


    /**
     * FTP Directory on the server for auto send
     */
    @ProfilePreference(name = PreferenceNames.FTP_DIRECTORY)
    public String getFtpDirectory() {
        return prefs.getString(PreferenceNames.FTP_DIRECTORY, "GPSLogger");
    }


    /**
     * GPS Logger folder path on phone.  Falls back to {@link Files#storageFolder(Context)} if nothing specified.
     */
    @ProfilePreference(name = PreferenceNames.GNSSLOGGER_FOLDER)
    public String getGpsLoggerFolder() {
        return prefs.getString(PreferenceNames.GNSSLOGGER_FOLDER, Files.storageFolder(AppSettings.getInstance().getApplicationContext()).getAbsolutePath());
    }


    /**
     * Sets GPS Logger folder path
     */
    public void setGpsLoggerFolder(String folderPath) {
        prefs.edit().putString(PreferenceNames.GNSSLOGGER_FOLDER, folderPath).apply();
    }


    /**
     * The minimum seconds interval between logging points
     */
    @ProfilePreference(name = PreferenceNames.MINIMUM_INTERVAL)
    public int getMinimumLoggingInterval() {
        return Strings.toInt(prefs.getString(PreferenceNames.MINIMUM_INTERVAL, "0"), 0);
    }

    /**
     * Sets the minimum time interval between logging points
     *
     * @param minimumSeconds - in seconds
     */
    public void setMinimumLoggingInterval(int minimumSeconds) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PreferenceNames.MINIMUM_INTERVAL, String.valueOf(minimumSeconds));
        editor.apply();
    }


    /**
     * The minimum distance, in meters, to have traveled before a point is recorded
     */
    @ProfilePreference(name = PreferenceNames.MINIMUM_DISTANCE)
    public int getMinimumDistanceInterval() {
        return (Strings.toInt(prefs.getString(PreferenceNames.MINIMUM_DISTANCE, "0"), 0));
    }

    /**
     * Sets the minimum distance to have traveled before a point is recorded
     *
     * @param distanceBeforeLogging - in meters
     */
    public void setMinimumDistanceInMeters(int distanceBeforeLogging) {
        prefs.edit().putString(PreferenceNames.MINIMUM_DISTANCE, String.valueOf(distanceBeforeLogging)).apply();
    }


    /**
     * The minimum accuracy of a point before the point is recorded, in meters
     */
    @ProfilePreference(name = PreferenceNames.MINIMUM_ACCURACY)
    public int getMinimumAccuracy() {
        return (Strings.toInt(prefs.getString(PreferenceNames.MINIMUM_ACCURACY, "40"), 40));
    }

    public void setMinimumAccuracy(int minimumAccuracy) {
        prefs.edit().putString(PreferenceNames.MINIMUM_ACCURACY, String.valueOf(minimumAccuracy)).apply();
    }


    /**
     * Whether to keep GPS on between fixes
     */
    @ProfilePreference(name = PreferenceNames.KEEP_GPS_ON_BETWEEN_FIXES)
    public boolean shouldKeepGPSOnBetweenFixes() {
        return prefs.getBoolean(PreferenceNames.KEEP_GPS_ON_BETWEEN_FIXES, false);
    }

    /**
     * Set whether to keep GPS on between fixes
     */
    public void setShouldKeepGPSOnBetweenFixes(boolean keepFix) {
        prefs.edit().putBoolean(PreferenceNames.KEEP_GPS_ON_BETWEEN_FIXES, keepFix).apply();
    }


    /**
     * How long to keep retrying for a fix if one with the user-specified accuracy hasn't been found
     */
    @ProfilePreference(name = PreferenceNames.LOGGING_RETRY_TIME)
    public int getLoggingRetryPeriod() {
        return (Strings.toInt(prefs.getString(PreferenceNames.LOGGING_RETRY_TIME, "60"), 60));
    }


    /**
     * Sets how long to keep trying for an accurate fix
     *
     * @param retryInterval in seconds
     */
    public void setLoggingRetryPeriod(int retryInterval) {
        prefs.edit().putString(PreferenceNames.LOGGING_RETRY_TIME, String.valueOf(retryInterval)).apply();
    }

    /**
     * How long to keep retrying for an accurate point before giving up
     */
    @ProfilePreference(name = PreferenceNames.ABSOLUTE_TIMEOUT)
    public int getAbsoluteTimeoutForAcquiringPosition() {
        return (Strings.toInt(prefs.getString(PreferenceNames.ABSOLUTE_TIMEOUT, "120"), 120));
    }

    /**
     * Sets how long to keep retrying for an accurate point before giving up
     *
     * @param absoluteTimeout in seconds
     */
    public void setAbsoluteTimeoutForAcquiringPosition(int absoluteTimeout) {
        prefs.edit().putString(PreferenceNames.ABSOLUTE_TIMEOUT, String.valueOf(absoluteTimeout)).apply();
    }

    /**
     * Whether to start logging on application launch
     */
    @ProfilePreference(name = PreferenceNames.START_LOGGING_ON_APP_LAUNCH)
    public boolean shouldStartLoggingOnAppLaunch() {
        return prefs.getBoolean(PreferenceNames.START_LOGGING_ON_APP_LAUNCH, false);
    }

    /**
     * Whether to start logging when phone is booted up
     */
    @ProfilePreference(name = PreferenceNames.START_LOGGING_ON_BOOTUP)
    public boolean shouldStartLoggingOnBootup() {
        return prefs.getBoolean(PreferenceNames.START_LOGGING_ON_BOOTUP, false);
    }


    /**
     * Which navigation item the user selected
     */
    public int getUserSelectedNavigationItem() {
        return Strings.toInt(prefs.getString(PreferenceNames.SELECTED_NAVITEM, "0"), 0);
    }

    /**
     * Sets which navigation item the user selected
     */
    public void setUserSelectedNavigationItem(int position) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PreferenceNames.SELECTED_NAVITEM, String.valueOf(position));
        editor.apply();
    }

    /**
     * Whether to hide the buttons when displaying the app notification
     */
    @ProfilePreference(name = PreferenceNames.HIDE_NOTIFICATION_BUTTONS)
    public boolean shouldHideNotificationButtons() {
        return prefs.getBoolean(PreferenceNames.HIDE_NOTIFICATION_BUTTONS, false);
    }


    @ProfilePreference(name = PreferenceNames.HIDE_NOTIFICATION_FROM_STATUS_BAR)
    public boolean shouldHideNotificationFromStatusBar() {
        return prefs.getBoolean(PreferenceNames.HIDE_NOTIFICATION_FROM_STATUS_BAR, false);
    }

    /**
     * Whether to display certain values using imperial units
     */
    @ProfilePreference(name = PreferenceNames.DISPLAY_IMPERIAL)
    public boolean shouldDisplayImperialUnits() {
        return prefs.getBoolean(PreferenceNames.DISPLAY_IMPERIAL, false);
    }

    /**
     * Display format to use for lat long coordinates on screen
     * DEGREES_MINUTES_SECONDS, DEGREES_DECIMAL_MINUTES, DECIMAL_DEGREES
     */
    @ProfilePreference(name = PreferenceNames.LATLONG_DISPLAY_FORMAT)
    public PreferenceNames.DegreesDisplayFormat getDisplayLatLongFormat() {
        String chosenValue = prefs.getString(PreferenceNames.LATLONG_DISPLAY_FORMAT, "DEGREES_MINUTES_SECONDS");
        return PreferenceNames.DegreesDisplayFormat.valueOf(chosenValue);
    }

    public void setDisplayLatLongFormat(PreferenceNames.DegreesDisplayFormat displayFormat) {
        prefs.edit().putString(PreferenceNames.LATLONG_DISPLAY_FORMAT, displayFormat.toString()).apply();
    }


    /**
     * Whether to log to KML file
     */
    @ProfilePreference(name = PreferenceNames.LOG_TO_KML)
    public boolean shouldLogToKml() {
        return prefs.getBoolean(PreferenceNames.LOG_TO_KML, false);
    }


    /**
     * Whether to log to GPX file
     */
    @ProfilePreference(name = PreferenceNames.LOG_TO_GPX)
    public boolean shouldLogToGpx() {
        return prefs.getBoolean(PreferenceNames.LOG_TO_GPX, true);
    }

    /**
     * Whether to log to GPX in GPX 1.0 or 1.1 format
     */
    @ProfilePreference(name = PreferenceNames.LOG_AS_GPX_11)
    public boolean shouldLogAsGpx11() {
        return prefs.getBoolean(PreferenceNames.LOG_AS_GPX_11, false);
    }


    /**
     * Whether to log to a CSV file
     */
    @ProfilePreference(name = PreferenceNames.LOG_TO_CSV)
    public boolean shouldLogToCSV() {
        return prefs.getBoolean(PreferenceNames.LOG_TO_CSV, false);
    }

    /**
     * Whether to log to a GeoJSON file
     */
    @ProfilePreference(name = PreferenceNames.LOG_TO_GEOJSON)
    public boolean shouldLogToGeoJSON() {
        return prefs.getBoolean(PreferenceNames.LOG_TO_GEOJSON, false);
    }


    /**
     * Whether to log to NMEA file
     */
    @ProfilePreference(name = PreferenceNames.LOG_TO_NMEA)
    public boolean shouldLogToNmea() {
        return prefs.getBoolean(PreferenceNames.LOG_TO_NMEA, false);
    }


    /**
     * Whether to log to a custom URL. The app will log to the URL returned by {@link #getCustomLoggingUrl()}
     */
    @ProfilePreference(name = PreferenceNames.LOG_TO_URL)
    public boolean shouldLogToCustomUrl() {
        return prefs.getBoolean(PreferenceNames.LOG_TO_URL, false);
    }

    @ProfilePreference(name = PreferenceNames.LOG_TO_URL_METHOD)
    public String getCustomLoggingHTTPMethod() {
        return prefs.getString(PreferenceNames.LOG_TO_URL_METHOD, "GET");
    }


    @ProfilePreference(name = PreferenceNames.LOG_TO_URL_BODY)
    public String getCustomLoggingHTTPBody() {
        return prefs.getString(PreferenceNames.LOG_TO_URL_BODY, "");
    }

    @ProfilePreference(name = PreferenceNames.LOG_TO_URL_HEADERS)
    public String getCustomLoggingHTTPHeaders() {
        return prefs.getString(PreferenceNames.LOG_TO_URL_HEADERS, "");
    }

    @ProfilePreference(name = PreferenceNames.LOG_TO_URL_BASICAUTH_USERNAME)
    public String getCustomLoggingBasicAuthUsername() {
        return prefs.getString(PreferenceNames.LOG_TO_URL_BASICAUTH_USERNAME, "");
    }

    public void setCustomLoggingBasicAuthUsername(String username) {
        prefs.edit().putString(PreferenceNames.LOG_TO_URL_BASICAUTH_USERNAME, username).apply();
    }

    @ProfilePreference(name = PreferenceNames.LOG_TO_URL_BASICAUTH_PASSWORD)
    public String getCustomLoggingBasicAuthPassword() {
        return prefs.getString(PreferenceNames.LOG_TO_URL_BASICAUTH_PASSWORD, "");
    }

    public void setCustomLoggingBasicAuthPassword(String password) {
        prefs.edit().putString(PreferenceNames.LOG_TO_URL_BASICAUTH_PASSWORD, password).apply();
    }

    /**
     * The custom URL to log to.  Relevant only if {@link #shouldLogToCustomUrl()} returns true.
     */
    @ProfilePreference(name = PreferenceNames.LOG_TO_URL_PATH)
    public String getCustomLoggingUrl() {
        return prefs.getString(PreferenceNames.LOG_TO_URL_PATH, "http://localhost/log?lat=%LAT&longitude=%LON&time=%TIME&s=%SPD");
    }


    @ProfilePreference(name = PreferenceNames.LOG_PASSIVE_LOCATIONS)
    public boolean shouldLogPassiveLocations() {
        return prefs.getBoolean(PreferenceNames.LOG_PASSIVE_LOCATIONS, false);
    }


    @ProfilePreference(name = PreferenceNames.LOG_SATELLITE_LOCATIONS)
    public boolean shouldLogSatelliteLocations() {
        return prefs.getBoolean(PreferenceNames.LOG_SATELLITE_LOCATIONS, true);
    }

    public void setShouldLogSatelliteLocations(boolean value) {
        prefs.edit().putBoolean(PreferenceNames.LOG_SATELLITE_LOCATIONS, value).apply();
    }

    @ProfilePreference(name = PreferenceNames.LOG_NETWORK_LOCATIONS)
    public boolean shouldLogNetworkLocations() {
        return prefs.getBoolean(PreferenceNames.LOG_NETWORK_LOCATIONS, true);
    }

    public void setShouldLogNetworkLocations(boolean value) {
        prefs.edit().putBoolean(PreferenceNames.LOG_NETWORK_LOCATIONS, value).apply();
    }


    /**
     * New file creation preference:
     * onceamonth - once a month,
     * onceaday - once a day,
     * customfile - custom file (static),
     * everystart - every time the service starts
     */
    @ProfilePreference(name = PreferenceNames.NEW_FILE_CREATION_MODE)
    private String getNewFileCreationMode() {
        return prefs.getString(PreferenceNames.NEW_FILE_CREATION_MODE, "everystart");
    }


    /**
     * Whether a new file should be created daily
     */
    public boolean shouldCreateNewFileOnceADay() {
        return (getNewFileCreationMode().equals("onceaday"));
    }


    /**
     * Whether a new file should be created monthly
     */
    public boolean shouldCreateNewFileOnceAMonth() {
        return (getNewFileCreationMode().equals("onceamonth"));
    }


    /**
     * Whether only a custom file should be created
     */
    public boolean shouldCreateCustomFile() {
        return getNewFileCreationMode().equals("custom") || getNewFileCreationMode().equals("static");
    }


    /**
     * The custom filename to use if {@link #shouldCreateCustomFile()} returns true
     */
    @ProfilePreference(name = PreferenceNames.CUSTOM_FILE_NAME)
    public String getCustomFileName() {
        return prefs.getString(PreferenceNames.CUSTOM_FILE_NAME, "ets.acmi.gnssdislogger");
    }


    /**
     * Sets custom filename to use if {@link #shouldCreateCustomFile()} returns true
     */
    public void setCustomFileName(String customFileName) {
        prefs.edit().putString(PreferenceNames.CUSTOM_FILE_NAME, customFileName).apply();
    }

    /**
     * Whether to prompt for a custom file name each time logging starts, if {@link #shouldCreateCustomFile()} returns true
     */
    @ProfilePreference(name = PreferenceNames.ASK_CUSTOM_FILE_NAME)
    public boolean shouldAskCustomFileNameEachTime() {
        return prefs.getBoolean(PreferenceNames.ASK_CUSTOM_FILE_NAME, true);
    }

    /**
     * Whether automatic sending to various targets (email,ftp, dropbox, etc) is enabled
     */
    @ProfilePreference(name = PreferenceNames.AUTOSEND_ENABLED)
    public boolean isAutoSendEnabled() {
        return prefs.getBoolean(PreferenceNames.AUTOSEND_ENABLED, false);
    }


    /**
     * The time, in minutes, before files are sent to the auto-send targets
     */
    @ProfilePreference(name = PreferenceNames.AUTOSEND_FREQUENCY)
    public int getAutoSendInterval() {
        return Math.round(Float.parseFloat(prefs.getString(PreferenceNames.AUTOSEND_FREQUENCY, "60")));
    }


    /**
     * Whether to auto send to targets when logging is stopped
     */
    @ProfilePreference(name = PreferenceNames.AUTOSEND_ON_STOP)
    public boolean shouldAutoSendOnStopLogging() {
        return prefs.getBoolean(PreferenceNames.AUTOSEND_ON_STOP, false);
    }

    /**
     * Whether to write log messages to a debuglog.txt file
     */
    public boolean shouldDebugToFile() {
        return prefs.getBoolean(PreferenceNames.DEBUG_TO_FILE, false);
    }


    /**
     * Whether to zip the files up before auto sending to targets
     */
    @ProfilePreference(name = PreferenceNames.AUTOSEND_ZIP)
    public boolean shouldSendZipFile() {
        return prefs.getBoolean(PreferenceNames.AUTOSEND_ZIP, true);
    }


    /**
     * Whether to prefix the phone's serial number to the logging file
     */
    @ProfilePreference(name = PreferenceNames.PREFIX_SERIAL_TO_FILENAME)
    boolean shouldPrefixSerialToFileName() {
        return prefs.getBoolean(PreferenceNames.PREFIX_SERIAL_TO_FILENAME, false);
    }


    /**
     * Whether to detect user activity and if the user is still, pause logging
     */
    @ProfilePreference(name = PreferenceNames.ACTIVITYRECOGNITION_DONTLOGIFSTILL)
    public boolean shouldNotLogIfUserIsStill() {
        return prefs.getBoolean(PreferenceNames.ACTIVITYRECOGNITION_DONTLOGIFSTILL, false);
    }


    /**
     * Whether to subtract GeoID height from the reported altitude to get Mean Sea Level altitude instead of WGS84
     */
    @ProfilePreference(name = PreferenceNames.ALTITUDE_SHOULD_ADJUST)
    boolean shouldAdjustAltitudeFromGeoIdHeight() {
        return prefs.getBoolean(PreferenceNames.ALTITUDE_SHOULD_ADJUST, false);
    }


    /**
     * How much to subtract from the altitude reported
     */
    @ProfilePreference(name = PreferenceNames.ALTITUDE_SUBTRACT_OFFSET)
    int getSubtractAltitudeOffset() {
        return Strings.toInt(prefs.getString(PreferenceNames.ALTITUDE_SUBTRACT_OFFSET, "0"), 0);
    }


    /**
     * Whether to autosend only if wifi is enabled
     */
    @ProfilePreference(name = PreferenceNames.AUTOSEND_WIFI_ONLY)
    boolean shouldAutoSendOnWifiOnly() {
        return prefs.getBoolean(PreferenceNames.AUTOSEND_WIFI_ONLY, false);
    }


    @ProfilePreference(name = PreferenceNames.CURRENT_PROFILE_NAME)
    public String getCurrentProfileName() {
        return prefs.getString(PreferenceNames.CURRENT_PROFILE_NAME, AppSettings.getInstance().getString(R.string.profile_default));
    }

    public void setCurrentProfileName(String profileName) {
        prefs.edit().putString(PreferenceNames.CURRENT_PROFILE_NAME, profileName).apply();
    }

    /**
     * A preference to keep track of version specific changes.
     */
    @ProfilePreference(name = PreferenceNames.LAST_VERSION_SEEN_BY_USER)
    public int getLastVersionSeen() {
        return Strings.toInt(prefs.getString(PreferenceNames.LAST_VERSION_SEEN_BY_USER, "1"), 1);
    }

    public void setLastVersionSeen(int lastVersionSeen) {
        prefs.edit().putString(PreferenceNames.LAST_VERSION_SEEN_BY_USER, String.valueOf(lastVersionSeen)).apply();
    }


    @ProfilePreference(name = PreferenceNames.USER_SPECIFIED_LANGUAGE)
    public String getUserSpecifiedLocale() {
        return prefs.getString(PreferenceNames.USER_SPECIFIED_LANGUAGE, "");
    }

    public void setUserSpecifiedLocale(String userSpecifiedLocale) {
        prefs.edit().putString(PreferenceNames.USER_SPECIFIED_LANGUAGE, userSpecifiedLocale).apply();
    }

    @ProfilePreference(name = PreferenceNames.CUSTOM_FILE_NAME_KEEP_CHANGING)
    public boolean shouldChangeFileNameDynamically() {
        return prefs.getBoolean(PreferenceNames.CUSTOM_FILE_NAME_KEEP_CHANGING, true);
    }


    public boolean isSFTPEnabled() {
        return prefs.getBoolean(PreferenceNames.SFTP_ENABLED, false);
    }

    public String getSFTPHost() {
        return prefs.getString(PreferenceNames.SFTP_HOST, "127.0.0.1");
    }

    public int getSFTPPort() {
        return Strings.toInt(prefs.getString(PreferenceNames.SFTP_PORT, "22"), 22);
    }

    public String getSFTPUser() {
        return prefs.getString(PreferenceNames.SFTP_USER, "");
    }


    public String getSFTPPassword() {
        return prefs.getString(PreferenceNames.SFTP_PASSWORD, "");
    }

    public String getSFTPPrivateKeyFilePath() {
        return prefs.getString(PreferenceNames.SFTP_PRIVATE_KEY_PATH, "");
    }

    public void setSFTPPrivateKeyFilePath(String filePath) {
        prefs.edit().putString(PreferenceNames.SFTP_PRIVATE_KEY_PATH, filePath).apply();
    }

    public String getSFTPPrivateKeyPassphrase() {
        return prefs.getString(PreferenceNames.SFTP_PRIVATE_KEY_PASSPHRASE, "");
    }

    public String getSFTPKnownHostKey() {
        return prefs.getString(PreferenceNames.SFTP_KNOWN_HOST_KEY, "");
    }

    public void setSFTPKnownHostKey(String hostKey) {
        prefs.edit().putString(PreferenceNames.SFTP_KNOWN_HOST_KEY, hostKey).apply();
    }

    public String getSFTPRemoteServerPath() {
        return prefs.getString(PreferenceNames.SFTP_REMOTE_SERVER_PATH, "/tmp");
    }


    /**
     * Sets preferences in a generic manner from a .properties file
     */

    public void setPreferenceFromPropertiesFile(File file) throws IOException {
        Properties props = new Properties();
        InputStreamReader reader = new InputStreamReader(new FileInputStream(file));
        props.load(reader);

        for (Object key : props.keySet()) {

            SharedPreferences.Editor editor = prefs.edit();
            String value = props.getProperty(key.toString());
            LOG.info("Setting preset property: " + key.toString() + " to " + value);

            if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
                editor.putBoolean(key.toString(), Boolean.parseBoolean(value));
            } else {
                editor.putString(key.toString(), value);
            }
            editor.apply();
        }

    }

    /**
     * Whether to log to IEEE1278 (DIS) Server.
     */
    @ProfilePreference(name = PreferenceNames.LOG_TO_DIS)
    public boolean shouldLogToDIS() {
        return prefs.getBoolean(PreferenceNames.LOG_TO_DIS, true);
    }

    /**
     * DIS Server name
     */
    @ProfilePreference(name = PreferenceNames.DIS_UDP_HOST)
    public String getDISServer() {
        return prefs.getString(PreferenceNames.DIS_UDP_HOST, "localhost");
    }

    /**
     * DIS Server Port
     */
    @ProfilePreference(name = PreferenceNames.DIS_UDP_PORT)
    public String getDISServerPort() {
        return prefs.getString(PreferenceNames.DIS_UDP_PORT, "3000");
    }

    /**
     * IEEE1278 Site
     */
    @ProfilePreference(name = PreferenceNames.DIS_SITE)
    public String getDISSite() {
        return prefs.getString(PreferenceNames.DIS_SITE, "1");
    }

    /**
     * IEEE1278 Application
     */
    @ProfilePreference(name = PreferenceNames.DIS_APPLICATION)
    public String getDISApplication() {
        return prefs.getString(PreferenceNames.DIS_APPLICATION, "51");
    }


    /**
     * IEEE1278 Exercise
     */
    @ProfilePreference(name = PreferenceNames.DIS_EXERCISE)
    public String getDISExercise() {
        return prefs.getString(PreferenceNames.DIS_EXERCISE, "1");
    }


    /**
     * IEEE1278 Enumeration
     */
    @ProfilePreference(name = PreferenceNames.DIS_ENUMERATION)
    public String getDISEnumeration() {
        return prefs.getString(PreferenceNames.DIS_ENUMERATION, "1:2:223:20:01:01:AH-64 Apache");
    }

    /**
     * IEEE1278 Marking (CALLSIGN)
     */
    @ProfilePreference(name = PreferenceNames.DIS_MARKING)
    public String getDISMarking() {
        return prefs.getString(PreferenceNames.DIS_MARKING, "CALLSIGN");
    }

    /**
     * IEEE1278 Entity Id
     */
    @ProfilePreference(name = PreferenceNames.DIS_ENTITYID)
    public String getDISEntityid() {
        return prefs.getString(PreferenceNames.DIS_ENTITYID, "1");
    }


    /**
     * IEEE1278 Force Id
     */
    @ProfilePreference(name = PreferenceNames.DIS_FORCEID)
    public String getDISForceid() {
        return prefs.getString(PreferenceNames.DIS_FORCEID, "1");
    }


    /**
     * IEEE1278 Force Color
     */

    public int getDisTint() {
        switch (prefs.getString(PreferenceNames.DIS_FORCEID, "1")) {
            case "0":
                return Color.YELLOW; //YELLOW
            case "1":
                return Color.BLUE; //BLUE
            case "2":
                return Color.RED; //RED
            default:
                return Color.GREEN; //GREEN
        }
    }

    /**
     * IEEE1278 Force Icon
     */
    public int getDisDrawable() {
        switch (Objects.requireNonNull(prefs.getString(PreferenceNames.DIS_ENUMERATION, "1"))) {
            case "1:2:225:01:03:07:F-16 Block 60":
            case "1:2:071:01:04:02:Mirage 2000–9":
                return R.drawable.dis_fighterjet;
            case "1:2:071:01:05:01:Dassault Rafale":
                return R.drawable.dis_rafale;
            case "1:2:225:04:01:01:Saab 340 AEW(AWACS)":
                return R.drawable.dis_awacs;
            case "1:2:225:04:08:03:KC-30A MRTT Tanker":
                return R.drawable.dis_generic_airplane;
            case "1:2:225:50:04:01:MQ-1 Predator":
                return R.drawable.dis_predator;
            case "1:2:224:20:01:01:AH-64 Apache":
                return R.drawable.dis_apache;
            case "1:2:225:23:01:01:CH-47 Chinook":
                return R.drawable.dis_chinook;
            case "1:2:071:21:10:02:Eurocopter AS565":
                return R.drawable.dis_helicopter;
            case "1:1:222:02:01:01:302:BMP-1 Armored Fighting Vehicle":
                return R.drawable.dis_tank;
            case "3:1:225:06:01:21:Dismounted Infantry":
                return R.drawable.navigation;

        }
        return R.drawable.navigation;

    }


    /**
     * Whether to log to European Air Group format (EAG).
     */
    @ProfilePreference(name = PreferenceNames.LOG_TO_EAG)
    public boolean shouldLogToEag() {
        return prefs.getBoolean(PreferenceNames.LOG_TO_EAG, false);
    }


}

