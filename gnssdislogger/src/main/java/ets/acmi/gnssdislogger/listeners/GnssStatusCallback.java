package ets.acmi.gnssdislogger.listeners;

import android.location.GnssStatus;
import android.os.Build;

import androidx.annotation.RequiresApi;

import org.slf4j.Logger;

import de.greenrobot.event.EventBus;
import ets.acmi.gnssdislogger.common.Session;
import ets.acmi.gnssdislogger.common.events.ServiceEvents;
import ets.acmi.gnssdislogger.common.slf4j.Logs;

/**
 * @see GnssStatus.Callback
 * Will use the Event Buss to share information with other processes
 * Shares satellite counts (Satellites in view and used on fix)
 */
@RequiresApi(api = Build.VERSION_CODES.N)
public class GnssStatusCallback extends GnssStatus.Callback {

    private static final Logger LOG = Logs.of(GnssStatusCallback.class);
    private final Session session = Session.getInstance();

    @Override
    public void onFirstFix(int ttffMillis) {
        super.onFirstFix(ttffMillis);
        LOG.debug("First Fix received");
    }

    @Override
    public void onSatelliteStatusChanged(GnssStatus gnssStatus) {
        super.onSatelliteStatusChanged(gnssStatus);
        int satellitesInView = gnssStatus.getSatelliteCount();
        int satelliteIndex;
        int satellitesUsedInFix = 0;

        if (satellitesInView != 0) {
            for (satelliteIndex = 0; satelliteIndex < satellitesInView; satelliteIndex++) {
                if (gnssStatus.usedInFix(satelliteIndex)) {
                    satellitesUsedInFix++;
                }
            }
        }
        LOG.debug("Satellites used: " + satellitesUsedInFix + "/" + satellitesInView);
        EventBus.getDefault().post(new ServiceEvents.SatellitesCount(satellitesInView,satellitesUsedInFix));
        //TODO: research why do we need to store satellites on a session. Clean code!
        session.setSatellitesinView(satellitesInView);
        session.setSatellitesInFix(satellitesUsedInFix);
    }

    @Override
    public void onStarted() {
        super.onStarted();
        LOG.debug("GnssStatusCallback started");
    }

    @Override
    public void onStopped() {
        LOG.debug("GnssStatusCallback stopped");
        super.onStopped();
    }

}