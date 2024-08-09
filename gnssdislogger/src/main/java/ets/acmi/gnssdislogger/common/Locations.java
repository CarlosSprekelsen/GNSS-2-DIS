package ets.acmi.gnssdislogger.common;


import android.location.Location;

public class Locations {


    public static Location getLocationWithAdjustedAltitude(Location loc, PreferenceHelper ph) {
        if (!loc.hasAltitude()) {
            return loc;
        }

        if (ph.shouldAdjustAltitudeFromGeoIdHeight() && loc.getExtras() != null) {
            String geoidheight = loc.getExtras().getString(BundleConstants.GEOIDHEIGHT);
            if (!Strings.isNullOrEmpty(geoidheight)) {
                loc.setAltitude(loc.getAltitude() - Double.parseDouble(geoidheight));
            } else {
                //If geoid height not present for adjustment, don't record an elevation at all.
                loc.removeAltitude();
            }
        }

        if (loc.hasAltitude() && ph.getSubtractAltitudeOffset() != 0) {
            loc.setAltitude(loc.getAltitude() - ph.getSubtractAltitudeOffset());
        }

        return loc;
    }

    public static Location getLocationAdjustedForGPSWeekRollover(Location loc) {
        long recordedTime = loc.getTime();
        //If the date is before April 6, 23:59:59, there's a GPS week rollover problem
        if (recordedTime < 1554595199000L) {
            recordedTime = recordedTime + 619315200000L;  //add 1024 weeks
            loc.setTime(recordedTime);
        }

        return loc;
    }

}
