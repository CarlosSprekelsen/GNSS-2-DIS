package ets.acmi.gnssdislogger.common;

import android.location.Location;

import java.io.Serializable;

public class SerializableLocation implements Serializable {

    private double altitude;
    private double accuracy;
    private float bearing;
    private double latitude;
    private double longitude;
    private String provider;
    private float speed;
    private long time;
    private final boolean hasAltitude;
    private final boolean hasAccuracy;
    private final boolean hasBearing;
    private final boolean hasSpeed;
    private final int satelliteCount;
    private final String detectedActivity;
    private final String hdop;
    private final String vdop;
    private final String pdop;


    public SerializableLocation(Location loc) {

        altitude = loc.getAltitude();
        accuracy = loc.getAccuracy();
        bearing = loc.getBearing();
        latitude = loc.getLatitude();
        longitude = loc.getLongitude();
        provider = loc.getProvider();
        speed = loc.getSpeed();
        time = loc.getTime();
        hasAltitude = loc.hasAltitude();
        hasAccuracy = loc.hasAccuracy();
        hasBearing = loc.hasBearing();
        hasSpeed = loc.hasSpeed();
        satelliteCount = Maths.getBundledSatelliteCount(loc);
        detectedActivity = extractExtra(loc, BundleConstants.DETECTED_ACTIVITY);
        hdop = extractExtra(loc, BundleConstants.HDOP);
        vdop = extractExtra(loc, BundleConstants.VDOP);
        pdop = extractExtra(loc, BundleConstants.PDOP);
    }

    private String extractExtra(Location loc, String key) {
        if (loc.getExtras() != null
                && !Strings.isNullOrEmpty(loc.getExtras().getString(key))) {
            return loc.getExtras().getString(key);
        }

        return "";
    }

    public boolean hasAltitude() {
        return hasAltitude;
    }

    public boolean hasAccuracy() {
        return hasAccuracy;
    }

    public boolean hasBearing() {
        return hasBearing;
    }

    public boolean hasSpeed() {
        return hasSpeed;
    }

    public double getAltitude() {
        return altitude;
    }

    public void setAltitude(double altitude) {
        this.altitude = altitude;
    }

    public double getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(double accuracy) {
        this.accuracy = accuracy;
    }

    public float getBearing() {
        return bearing;
    }

    public void setBearing(float bearing) {
        this.bearing = bearing;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public float getSpeed() {
        return speed;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public int getSatelliteCount() {
        return satelliteCount;
    }

    public String getDetectedActivity() {
        return detectedActivity;
    }

    public String getHDOP() {
        return hdop;
    }

    public String getVDOP() {
        return vdop;
    }

    public String getPDOP() {
        return pdop;
    }
}
