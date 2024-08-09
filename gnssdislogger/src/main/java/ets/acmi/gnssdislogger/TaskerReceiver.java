package ets.acmi.gnssdislogger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.content.ContextCompat;

import org.slf4j.Logger;

import ets.acmi.gnssdislogger.common.slf4j.Logs;
import ets.acmi.gnssdislogger.listeners.GnssLoggingService;

public class TaskerReceiver extends BroadcastReceiver {

    private static final Logger LOG = Logs.of(TaskerReceiver.class);

    @Override
    public void onReceive(Context context, Intent intent) {
        LOG.info("Tasker Command Received");

        Intent serviceIntent = new Intent(context, GnssLoggingService.class);
        serviceIntent.putExtras(intent);
        ContextCompat.startForegroundService(context, serviceIntent);
    }
}
