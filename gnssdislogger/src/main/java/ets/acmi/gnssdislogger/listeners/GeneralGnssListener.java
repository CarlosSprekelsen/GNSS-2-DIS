package ets.acmi.gnssdislogger.listeners;

import android.location.GnssMeasurementsEvent;
import android.location.GnssNavigationMessage;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationProvider;
import android.location.OnNmeaMessageListener;
import android.os.Bundle;

import org.slf4j.Logger;

import ets.acmi.gnssdislogger.R;
import ets.acmi.gnssdislogger.common.BundleConstants;
import ets.acmi.gnssdislogger.common.Session;
import ets.acmi.gnssdislogger.common.slf4j.Logs;

class GeneralGnssListener implements LocationListener {
    private static final Logger LOG = Logs.of(GeneralGnssListener.class);
    private static GnssLoggingService loggingService;
    String latestHdop;
    String latestPdop;
    String latestVdop;
    String geoIdHeight;
    String ageOfDgpsData;
    String dgpsId;

    private final String listenerName;
    private final Session session = Session.getInstance();

    GeneralGnssListener(GnssLoggingService activity, String name) {
        loggingService = activity;
        listenerName = name;
    }

//
// Location Listener implementation
//

    public void onLocationChanged(Location loc) {

        try {
            if (loc != null) {
                Bundle b = new Bundle();
                b.putString(BundleConstants.HDOP, this.latestHdop);
                b.putString(BundleConstants.PDOP, this.latestPdop);
                b.putString(BundleConstants.VDOP, this.latestVdop);
                b.putString(BundleConstants.GEOIDHEIGHT, this.geoIdHeight);
                b.putString(BundleConstants.AGEOFDGPSDATA, this.ageOfDgpsData);
                b.putString(BundleConstants.DGPSID, this.dgpsId);

                b.putBoolean(BundleConstants.PASSIVE, this.listenerName.equalsIgnoreCase(BundleConstants.PASSIVE));
                b.putString(BundleConstants.LISTENER, this.listenerName);
                b.putInt(BundleConstants.SATELLITES_FIX, session.getSatellitesInFix());
                b.putInt(BundleConstants.SATELLITES_VIEW, session.getSatellitesinView());
                b.putString(BundleConstants.DETECTED_ACTIVITY, this.session.getLatestDetectedActivityName());

                loc.setExtras(b);
                loggingService.onLocationChanged(loc);

                this.latestHdop = "";
                this.latestPdop = "";
                this.latestVdop = "";
                this.session.setLatestDetectedActivity(null);
            }

        } catch (Exception ex) {
            LOG.error("GeneralGnssListener.onLocationChanged", ex);
        }

    }


    public void onProviderDisabled(String provider) {
        LOG.info("Provider disabled: " + provider);
        loggingService.restartGnssManagers();
    }


    public void onProviderEnabled(String provider) {
        LOG.info("Provider enabled: " + provider);
        loggingService.restartGnssManagers();
    }

    // onStatusChanged method was deprecated in API level 29. This callback will never be invoked on Android Q and above.
    // shall be replaced by
    //public void onLocationStatusChanged(String provider, int status, Bundle extras) {
    public void onStatusChanged(String provider, int status, Bundle extras) {
        if (status == LocationProvider.OUT_OF_SERVICE) {
            LOG.info(provider + " is out of service");
            loggingService.stopManagerAndResetAlarm();
        }

        if (status == LocationProvider.AVAILABLE) {
            LOG.info(provider + " is available");
        }

        if (status == LocationProvider.TEMPORARILY_UNAVAILABLE) {
            LOG.info(provider + " is temporarily unavailable");
        }
    }


    /**
     * @see GnssMeasurementsEvent.Callback#
     * onGnssMeasurementsReceived(GnssMeasurementsEvent)
     */

    public void onGnssMeasurementsReceived(GnssMeasurementsEvent event) {
    }

    /**
     * @see GnssMeasurementsEvent.Callback#onStatusChanged(int)
     */

    public void onGnssMeasurementsStatusChanged(int status) {
    }

    /**
     * @see GnssNavigationMessage.Callback# onGnssNavigationMessageReceived(GnssNavigationMessage)
     */
    public void onGnssNavigationMessageReceived(GnssNavigationMessage event) {

    }

    /**
     * @see GnssNavigationMessage.Callback#onStatusChanged(int)
     */
    public void onGnssNavigationMessageStatusChanged(int status) {
    }

    /**
     * Called when the listener is registered to listen to GNSS events
     */
    public void onListenerRegistration(String listener, boolean result) {
        LOG.info(loggingService.getString(R.string.started_waiting));
    }

    /**
     * @see OnNmeaMessageListener#onNmeaMessage(String, long)
     */
    public void onNmeaReceived(long l, String s) {
    }

    public void onTTFFReceived(long l) {
    }

}

/*
    public void onGnssStatusChanged(int event) {

        switch (event) {
            case GnssStatus.GPS_EVENT_FIRST_FIX:

                break;



            case GnssStatus.GPS_EVENT_STARTED:
                LOG.info(loggingService.getString(R.string.started_waiting));
                break;

            case GnssStatus.GPS_EVENT_STOPPED:
                LOG.info(loggingService.getString(R.string.gps_stopped));
                break;

        }
    }
*/
