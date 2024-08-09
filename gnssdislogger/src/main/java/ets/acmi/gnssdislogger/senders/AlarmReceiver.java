package ets.acmi.gnssdislogger.senders;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.content.ContextCompat;

import org.slf4j.Logger;

import de.greenrobot.event.EventBus;
import ets.acmi.gnssdislogger.common.AppSettings;
import ets.acmi.gnssdislogger.common.events.CommandEvents;
import ets.acmi.gnssdislogger.common.slf4j.Logs;
import ets.acmi.gnssdislogger.listeners.GnssLoggingService;


public class AlarmReceiver extends BroadcastReceiver {

    private static final Logger LOG = Logs.of(AlarmReceiver.class);

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            LOG.debug("Alarm received");

            EventBus.getDefault().post(new CommandEvents.AutoSend(null));

            Intent serviceIntent = new Intent(AppSettings.getInstance(), GnssLoggingService.class);
            ContextCompat.startForegroundService(context, serviceIntent);
        } catch (Exception ex) {
            LOG.error("AlarmReceiver", ex);
        }
    }
}
