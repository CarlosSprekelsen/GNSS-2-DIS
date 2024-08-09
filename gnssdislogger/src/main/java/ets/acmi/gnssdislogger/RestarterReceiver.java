package ets.acmi.gnssdislogger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.content.ContextCompat;

import org.slf4j.Logger;

import ets.acmi.gnssdislogger.common.IntentConstants;
import ets.acmi.gnssdislogger.common.slf4j.Logs;
import ets.acmi.gnssdislogger.listeners.GnssLoggingService;

public class RestarterReceiver extends BroadcastReceiver {

    private static final Logger LOG = Logs.of(RestarterReceiver.class);

    @Override
    public void onReceive(Context context, Intent intent) {
        LOG.warn("GPSLogger service is being killed, broadcast received. Attempting to restart");
        boolean wasRunning = intent.getBooleanExtra("was_running", false);
        LOG.info("was_running:" + wasRunning);

        Intent serviceIntent = new Intent(context, GnssLoggingService.class);

        if (wasRunning) {
            serviceIntent.putExtra(IntentConstants.IMMEDIATE_START, true);
            ContextCompat.startForegroundService(context, serviceIntent);
        } else {
            serviceIntent.putExtra(IntentConstants.IMMEDIATE_STOP, true);
            ContextCompat.startForegroundService(context, serviceIntent);
        }

    }
}
