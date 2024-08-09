package ets.acmi.gnssdislogger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.content.ContextCompat;

import org.slf4j.Logger;

import ets.acmi.gnssdislogger.common.IntentConstants;
import ets.acmi.gnssdislogger.common.PreferenceHelper;
import ets.acmi.gnssdislogger.common.slf4j.Logs;
import ets.acmi.gnssdislogger.listeners.GnssLoggingService;

public class StartupReceiver extends BroadcastReceiver {

    private static final Logger LOG = Logs.of(StartupReceiver.class);

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            boolean startImmediately = PreferenceHelper.getInstance().shouldStartLoggingOnBootup();

            LOG.info("Start on bootup - " + startImmediately);

            if (startImmediately) {

                Intent serviceIntent = new Intent(context, GnssLoggingService.class);
                serviceIntent.putExtra(IntentConstants.IMMEDIATE_START, true);
                ContextCompat.startForegroundService(context, serviceIntent);
            }
        } catch (Exception ex) {
            LOG.error("StartupReceiver", ex);

        }

    }

}
