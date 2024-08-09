package ets.acmi.gnssdislogger.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import org.slf4j.Logger;

import ets.acmi.gnssdislogger.R;
import ets.acmi.gnssdislogger.common.IntentConstants;
import ets.acmi.gnssdislogger.common.slf4j.Logs;
import ets.acmi.gnssdislogger.listeners.GnssLoggingService;
import ets.acmi.gnssdislogger.ui.Dialogs;

public class NotificationAnnotationActivity extends AppCompatActivity {

    //Called from the 'annotate' button in the Notification
    //This in turn captures user input and sends the input to the GPS Logging Service

    private static final Logger LOG = Logs.of(NotificationAnnotationActivity.class);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Dialogs.autoCompleteText(NotificationAnnotationActivity.this, "annotations",
                getString(R.string.add_description), getString(R.string.letters_numbers), "",
                (which, dialog, enteredText) -> {
                    if (which == Dialogs.AutoCompleteCallback.OK) {
                        LOG.info("Notification Annotation entered : " + enteredText);
                        Intent serviceIntent = new Intent(getApplicationContext(), GnssLoggingService.class);
                        serviceIntent.putExtra(IntentConstants.SET_DESCRIPTION, enteredText);
                        ContextCompat.startForegroundService(getApplicationContext(), serviceIntent);
                    }

                    finish();
                });

    }

    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish();
            super.finish();
        }

        return super.onKeyDown(keyCode, event);
    }
}
