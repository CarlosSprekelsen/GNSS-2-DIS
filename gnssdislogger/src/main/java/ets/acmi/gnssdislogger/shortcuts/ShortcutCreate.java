package ets.acmi.gnssdislogger.shortcuts;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.afollestad.materialdialogs.MaterialDialog;

import ets.acmi.gnssdislogger.R;

public class ShortcutCreate extends Activity {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        final CharSequence[] items = {getString(R.string.shortcut_start), getString(R.string.shortcut_stop)};

        new MaterialDialog.Builder(this)
                .title(R.string.shortcut_pickaction)
                .items(items)
                .itemsCallbackSingleChoice(-1, (materialDialog, view, item, charSequence) -> {
                    Intent shortcutIntent;
                    String shortcutLabel;
                    int shortcutIcon;

                    if (item == 0) {
                        shortcutIntent = new Intent(getApplicationContext(), ShortcutStart.class);
                        shortcutLabel = getString(R.string.shortcut_start);
                        shortcutIcon = R.drawable.gps_shortcut_start;

                    } else {
                        shortcutIntent = new Intent(getApplicationContext(), ShortcutStop.class);
                        shortcutLabel = getString(R.string.shortcut_stop);
                        shortcutIcon = R.drawable.gps_shortcut_stop;
                    }

                    Intent.ShortcutIconResource iconResource = Intent.ShortcutIconResource.fromContext
                            (getApplicationContext(), shortcutIcon);
                    Intent intent = new Intent();
                    intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
                    intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, shortcutLabel);
                    intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, iconResource);
                    setResult(RESULT_OK, intent);

                    finish();
                    return true;
                }).show();


    }
}