package ets.acmi.gnssdislogger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.content.ContextCompat;

import org.slf4j.Logger;

import de.greenrobot.event.EventBus;
import ets.acmi.gnssdislogger.common.Session;
import ets.acmi.gnssdislogger.common.events.CommandEvents;
import ets.acmi.gnssdislogger.common.slf4j.Logs;
import ets.acmi.gnssdislogger.listeners.GnssLoggingService;

public class MyPackageUpgradeReceiver extends BroadcastReceiver {

    private static final Logger LOG = Logs.of(MyPackageUpgradeReceiver.class);

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            boolean shouldResumeLogging = Session.getInstance().isStarted();
            LOG.debug("Package has been replaced. Should resume logging: " + shouldResumeLogging);

            if (shouldResumeLogging) {
                EventBus.getDefault().post(new CommandEvents.RequestStartStop(true));

                Intent serviceIntent = new Intent(context, GnssLoggingService.class);
                ContextCompat.startForegroundService(context, serviceIntent);
            }
        } catch (Exception ex) {
            LOG.error("Package upgrade receiver", ex);
        }
    }
}
