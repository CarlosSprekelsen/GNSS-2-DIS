package ets.acmi.gnssdislogger.shortcuts;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import org.slf4j.Logger;

import de.greenrobot.event.EventBus;
import ets.acmi.gnssdislogger.common.IntentConstants;
import ets.acmi.gnssdislogger.common.events.CommandEvents;
import ets.acmi.gnssdislogger.common.slf4j.Logs;
import ets.acmi.gnssdislogger.listeners.GnssLoggingService;


public class ShortcutStop extends Activity {

    private static final Logger LOG = Logs.of(ShortcutStop.class);

    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        LOG.info("Shortcut - stop logging");
        EventBus.getDefault().post(new CommandEvents.RequestStartStop(false));

        Intent serviceIntent = new Intent(getApplicationContext(), GnssLoggingService.class);
        serviceIntent.putExtra(IntentConstants.IMMEDIATE_STOP, true);
        getApplicationContext().startService(serviceIntent);

        finish();

    }


}