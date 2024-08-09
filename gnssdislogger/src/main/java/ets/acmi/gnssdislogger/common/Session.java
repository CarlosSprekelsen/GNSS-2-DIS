package ets.acmi.gnssdislogger.common;


import android.content.SharedPreferences;
import android.location.Location;
import android.preference.PreferenceManager;

import com.google.android.gms.location.DetectedActivity;

import edu.nps.moves.dis.EntityStatePdu;

public class Session {


    private static Session instance = null;
    private SharedPreferences prefs;
    private Location previousLocationInfo;
    private Location currentLocationInfo;
    private EntityStatePdu previousEntityStatePdu;
    private Double previousSeconds;


    private Session() {
        // initialize session
        previousEntityStatePdu = new EntityStatePdu();
    }

    public static Session getInstance() {
        if (instance == null) {
            instance = new Session();
            instance.prefs = PreferenceManager.getDefaultSharedPreferences(AppSettings.getInstance().getApplicationContext());
        }

        return instance;
    }

    private String get(String key, String defaultValue) {
        return prefs.getString("SESSION_" + key, defaultValue);
    }

    private void set(String key, String value) {
        prefs.edit().putString("SESSION_" + key, value).apply();
    }


    public boolean isSinglePointMode() {
        return Boolean.parseBoolean(get("isSinglePointMode", "false"));
    }

    public void setSinglePointMode(boolean singlePointMode) {
        set("isSinglePointMode", String.valueOf(singlePointMode));
    }

    /**
     * @return whether GPS (tower) is enabled
     */
    public boolean isTowerEnabled() {
        return Boolean.parseBoolean(get("towerEnabled", "false"));
    }

    /**
     * @param towerEnabled set whether GPS (tower) is enabled
     */
    public void setTowerEnabled(boolean towerEnabled) {
        set("towerEnabled", String.valueOf(towerEnabled));
    }

    /**
     * @return whether GPS (satellite) is enabled
     */
    public boolean isGnssEnabled() {
        return Boolean.parseBoolean(get("gnssEnabled", "false"));
    }

    /**
     * @param gnssEnabled set whether GPS (satellite) is enabled
     */
    public void setGnssEnabled(boolean gnssEnabled) {
        set("gnssEnabled", String.valueOf(gnssEnabled));
    }

    /**
     * @return whether logging has started
     */
    public boolean isStarted() {
        return Boolean.parseBoolean(get("LOGGING_STARTED", "false"));
    }

    /**
     * @param isStarted set whether logging has started
     */
    public void setStarted(boolean isStarted) {

        set("LOGGING_STARTED", String.valueOf(isStarted));

        if (isStarted) {
            set("startTimeStamp", String.valueOf(System.currentTimeMillis()));
        }
    }

    /**
     * @return the isUsingGnss
     */
    public boolean isUsingGnss() {
        return Boolean.parseBoolean(get("isUsingGnss", "false"));
    }

    /**
     * @param isUsingGnss the isUsingGnss to set
     */
    public void setUsingGnss(boolean isUsingGnss) {
        set("isUsingGnss", String.valueOf(isUsingGnss));
    }

    /**
     * @return the currentFileName (without extension)
     */
    public String getCurrentFileName() {
        return get("currentFileName", "");
    }


    /**
     * @param currentFileName the currentFileName to set
     */
    public void setCurrentFileName(String currentFileName) {
        set("currentFileName", currentFileName);
    }

    /**
     * @return the number of satellites in view
     */
    public int getSatellitesinView() {
        return Integer.parseInt(get("satellites_in_view", "0"));
    }

    /**
     * @param satellites sets the number of satellites in view
     */
    public void setSatellitesinView(int satellites) {
        set("satellites_in_view", String.valueOf(satellites));
    }

    /**
     * @return the number of satellites in Fix
     */
    public int getSatellitesInFix() {
        return Integer.parseInt(get("satellites_in_fix", "0"));
    }

    /**
     * @param satellites sets the number of visible satellites
     */
    public void setSatellitesInFix(int satellites) {
        set("satellites_in_fix", String.valueOf(satellites));
    }


    /**
     * @return the currentLatitude
     */
    public double getCurrentLatitude() {
        if (getCurrentLocationInfo() != null) {
            return getCurrentLocationInfo().getLatitude();
        } else {
            return 0;
        }
    }

    public double getPreviousLatitude() {
        Location loc = getPreviousLocationInfo();
        return loc != null ? loc.getLatitude() : 0;
    }

    public double getPreviousLongitude() {
        Location loc = getPreviousLocationInfo();
        return loc != null ? loc.getLongitude() : 0;
    }

    public double getTotalTravelled() {
        return Double.parseDouble(get("totalTravelled", "0"));
    }

    public void setTotalTravelled(double totalTravelled) {
        if (totalTravelled == 0) {
            setNumLegs(1);
        } else {
            setNumLegs(getNumLegs() + 1);
        }
        set("totalTravelled", String.valueOf(totalTravelled));
    }

    public int getNumLegs() {
        return Integer.parseInt(get("numLegs", "0"));
    }

    private void setNumLegs(int numLegs) {
        set("numLegs", String.valueOf(numLegs));
    }

    public Location getPreviousLocationInfo() {
        return previousLocationInfo;
    }

    public void setPreviousLocationInfo(Location previousLocationInfo) {
        this.previousLocationInfo = previousLocationInfo;
    }


    /**
     * Determines whether a valid location is available
     */
    public boolean hasValidLocation() {
        return (getCurrentLocationInfo() != null && getCurrentLatitude() != 0 && getCurrentLongitude() != 0);
    }

    /**
     * @return the currentLongitude
     */
    public double getCurrentLongitude() {
        if (getCurrentLocationInfo() != null) {
            return getCurrentLocationInfo().getLongitude();
        } else {
            return 0;
        }
    }

    /**
     * @return the latestTimeStamp (for location info)
     */
    public long getLatestTimeStamp() {
        return Long.parseLong(get("latestTimeStamp", "0"));
    }

    /**
     * @param latestTimeStamp the latestTimeStamp (for location info) to set
     */
    public void setLatestTimeStamp(long latestTimeStamp) {
        set("latestTimeStamp", String.valueOf(latestTimeStamp));
    }

    /**
     * @return the timestamp when measuring was started
     */
    public long getStartTimeStamp() {
        return Long.parseLong(get("startTimeStamp", String.valueOf(System.currentTimeMillis())));
    }

    /**
     * @return whether to create a new track segment
     */
    public boolean shouldAddNewTrackSegment() {
        return Boolean.parseBoolean(get("addNewTrackSegment", "false"));
    }

    /**
     * @param addNewTrackSegment set whether to create a new track segment
     */
    public void setAddNewTrackSegment(boolean addNewTrackSegment) {
        set("addNewTrackSegment", String.valueOf(addNewTrackSegment));
    }

    /**
     * @return the autoSendDelay to use for the timer
     */
    public float getAutoSendDelay() {
        return Float.parseFloat(get("autoSendDelay", "0"));
    }

    /**
     * @param autoSendDelay the autoSendDelay to set
     */
    public void setAutoSendDelay(float autoSendDelay) {
        set("autoSendDelay", String.valueOf(autoSendDelay));
    }

    /**
     * @return the Location class containing latest lat-long information
     */
    public Location getCurrentLocationInfo() {
        return currentLocationInfo;

    }

    /**
     * @param currentLocationInfo the latest Location class
     */
    public void setCurrentLocationInfo(Location currentLocationInfo) {
        this.currentLocationInfo = currentLocationInfo;
    }

    /**
     * @return whether the activity is bound to the GnssLoggingService
     */
    public boolean isBoundToService() {
        return Boolean.parseBoolean(get("isBound", "false"));
    }

    /**
     * @param isBound set whether the activity is bound to the GnssLoggingService
     */
    public void setBoundToService(boolean isBound) {
        set("isBound", String.valueOf(isBound));
    }

    public boolean hasDescription() {
        return !(getDescription().length() == 0);
    }

    public String getDescription() {
        return get("description", "");
    }

    public void setDescription(String newDescription) {
        set("description", newDescription);
    }

    public void clearDescription() {
        setDescription("");
    }

    public boolean isWaitingForLocation() {
        return Boolean.parseBoolean(get("waitingForLocation", "false"));
    }

    public void setWaitingForLocation(boolean waitingForLocation) {
        set("waitingForLocation", String.valueOf(waitingForLocation));
    }

    public boolean isAnnotationMarked() {
        return Boolean.parseBoolean(get("annotationMarked", "false"));
    }

    public void setAnnotationMarked(boolean annotationMarked) {
        set("annotationMarked", String.valueOf(annotationMarked));
    }


    public boolean showMapFragment() {
        return Boolean.parseBoolean(get("logviewMarked", "true"));
    }

    public void setLogviewMarked(boolean logviewMarked) {
        set("logviewMarked", String.valueOf(logviewMarked));
    }


    public String getCurrentFormattedFileName() {
        return get("currentFormattedFileName", "");
    }

    public void setCurrentFormattedFileName(String currentFormattedFileName) {
        set("currentFormattedFileName", currentFormattedFileName);
    }

    public long getUserStillSinceTimeStamp() {
        return Long.parseLong(get("userStillSinceTimeStamp", "0"));
    }

    public void setUserStillSinceTimeStamp(long lastUserStillTimeStamp) {
        set("userStillSinceTimeStamp", String.valueOf(lastUserStillTimeStamp));
    }

    public long getFirstRetryTimeStamp() {
        return Long.parseLong(get("firstRetryTimeStamp", "0"));
    }

    public void setFirstRetryTimeStamp(long firstRetryTimeStamp) {
        set("firstRetryTimeStamp", String.valueOf(firstRetryTimeStamp));
    }

    public void setLatestDetectedActivity(DetectedActivity latestDetectedActivity) {
        set("latestDetectedActivity", Strings.getDetectedActivityName(latestDetectedActivity));
    }

    public String getLatestDetectedActivityName() {
        return get("latestDetectedActivity", "");
    }


/*
   FOR DIS (IEE1278). Used to calculate velocity and acceleration.
 */

    public Double getPreviousSeconds() {
        if (previousSeconds != null) {
            return previousSeconds;
        } else {
            return 0.0;
        }
    }

    public void setPreviousSeconds(Double previousSeconds) {
        this.previousSeconds = previousSeconds;
    }

    public EntityStatePdu getPreviousEntityStatePdu() {
        return previousEntityStatePdu;
    }

    public void setPreviousEntityStatePdu(EntityStatePdu previousEntityStatePdu) {
        this.previousEntityStatePdu = previousEntityStatePdu;
    }
}

