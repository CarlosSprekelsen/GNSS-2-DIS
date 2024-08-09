package ets.acmi.gnssdislogger.common;


public class PreferenceNames {
    static final String MINIMUM_INTERVAL = "time_before_logging";
    static final String MINIMUM_DISTANCE = "distance_before_logging";
    static final String MINIMUM_ACCURACY = "accuracy_before_logging";
    static final String KEEP_GPS_ON_BETWEEN_FIXES = "keep_fix";
    static final String LOGGING_RETRY_TIME = "retry_time";
    static final String ABSOLUTE_TIMEOUT = "absolute_timeout";
    static final String START_LOGGING_ON_APP_LAUNCH = "startOnApplicationLaunch";
    static final String START_LOGGING_ON_BOOTUP = "startOnBootUp";
    static final String LOG_TO_KML = "log_kml";
    static final String LOG_TO_GPX = "log_gpx";
    static final String LOG_AS_GPX_11 = "log_gpx_11";
    static final String LOG_TO_CSV = "log_plain_text";
    static final String LOG_TO_GEOJSON = "log_geojson";
    static final String LOG_TO_NMEA = "log_nmea";
    static final String LOG_TO_URL = "log_customurl_enabled";
    static final String LOG_TO_URL_PATH = "log_customurl_url";
    static final String LOG_TO_URL_BODY = "log_customurl_body";
    static final String LOG_TO_URL_HEADERS = "log_customurl_headers";
    static final String LOG_TO_URL_METHOD = "log_customurl_method";
    static final String LOG_TO_URL_BASICAUTH_USERNAME = "log_customurl_basicauth_username";
    static final String LOG_TO_URL_BASICAUTH_PASSWORD = "log_customurl_basicauth_password";
    static final String LOG_PASSIVE_LOCATIONS = "log_passive_locations";
    static final String LOG_SATELLITE_LOCATIONS = "log_satellite_locations";
    static final String LOG_NETWORK_LOCATIONS = "log_network_locations";
    static final String NEW_FILE_CREATION_MODE = "new_file_creation";
    static final String CUSTOM_FILE_NAME = "new_file_custom_name";
    static final String CUSTOM_FILE_NAME_KEEP_CHANGING = "new_file_custom_keep_changing";
    static final String ASK_CUSTOM_FILE_NAME = "new_file_custom_each_time";
    static final String AUTOSEND_ENABLED = "autoUpload_enabled";
    static final String AUTOSEND_FREQUENCY = "autoUpload_frequency_minutes";
    static final String AUTOSEND_ON_STOP = "autoUpload_frequency_whenstoppressed";
    static final String AUTOSEND_ZIP = "autoUpload_send_zip";

    static final String DEBUG_TO_FILE = "debug_to_file";

    static final String HIDE_NOTIFICATION_BUTTONS = "hide_notification_buttons";
    static final String HIDE_NOTIFICATION_FROM_STATUS_BAR = "hide_notification_from_status_bar";
    static final String DISPLAY_IMPERIAL = "useImperial";


    static final String FTP_SERVER = "ftp_server";
    static final String FTP_PORT = "ftp_port";
    static final String FTP_USERNAME = "ftp_username";
    static final String FTP_PASSWORD = "ftp_password";
    static final String FTP_USE_FTPS = "ftp_useftps";
    static final String FTP_SSLORTLS = "ftp_ssltls";
    static final String FTP_IMPLICIT = "ftp_implicit";
    static final String FTP_AUTO_SEND_ENABLED = "ftp_enabled";
    static final String FTP_DIRECTORY = "ftp_directory";

    static final String GNSSLOGGER_FOLDER = "gnsslogger_folder";
    static final String PREFIX_SERIAL_TO_FILENAME = "new_file_prefix_serial";
    static final String ACTIVITYRECOGNITION_DONTLOGIFSTILL = "activityrecognition_dontlogifstill";
    static final String ALTITUDE_SUBTRACT_OFFSET = "altitude_subtract_offset";
    static final String ALTITUDE_SHOULD_ADJUST = "altitude_subtract_geoid_height";
    static final String AUTOSEND_WIFI_ONLY = "autoUpload_wifi_only";
    static final String CURRENT_PROFILE_NAME = "current_profile_name";
    static final String SELECTED_NAVITEM = "selected_navitem";

    static final String LAST_VERSION_SEEN_BY_USER = "last_version_seen";
    static final String USER_SPECIFIED_LANGUAGE = "user_specified_locale";

    static final String LATLONG_DISPLAY_FORMAT = "latlong_display_format";
    static final String SFTP_ENABLED = "sftp_enabled";
    static final String SFTP_HOST = "sftp_host";
    static final String SFTP_PORT = "sftp_port";
    static final String SFTP_USER = "sftp_user";
    static final String SFTP_PASSWORD = "sftp_password";
    static final String SFTP_PRIVATE_KEY_PATH = "sftp_private_key_path";
    static final String SFTP_PRIVATE_KEY_PASSPHRASE = "sftp_private_key_passphrase";
    static final String SFTP_KNOWN_HOST_KEY = "sftp_known_host_key";
    static final String SFTP_REMOTE_SERVER_PATH = "sftp_remote_server_path";
    static final String LOG_TO_DIS = "log_dis_enabled";
    static final String DIS_UDP_HOST = "dis_udp_host";
    static final String DIS_UDP_PORT = "dis_udp_port";
    static final String DIS_SITE = "dis_site";
    static final String DIS_APPLICATION = "dis_application";
    static final String DIS_EXERCISE = "dis_exercise";
    static final String DIS_FORCEID = "dis_force_id";
    static final String DIS_ENUMERATION = "dis_enumeration";
    static final String DIS_MARKING = "dis_marking";
    static final String DIS_ENTITYID = "dis_entity_id";

    static final String LOG_TO_EAG = "log_eag_enabled";

    public enum DegreesDisplayFormat {
        DEGREES_MINUTES_SECONDS, DEGREES_DECIMAL_MINUTES, DECIMAL_DEGREES
    }

}
