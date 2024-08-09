package ets.acmi.gnssdislogger.ui.fragments.settings;

import android.content.Intent;
import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import ets.acmi.gnssdislogger.R;
import ets.acmi.gnssdislogger.common.Strings;
import ets.acmi.gnssdislogger.ui.activity.MainPreferenceActivity;

/**
 * A {@link android.preference.PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p/>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */

public class PreferenceFragmentAutoUpload extends PreferenceFragmentCompat implements Preference.OnPreferenceClickListener {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

        // Load the preferences from an XML resource
        setPreferencesFromResource(R.xml.settings_upload, rootKey);

        findPreference("ftp_setup").setOnPreferenceClickListener(this);
        findPreference("sftp_setup").setOnPreferenceClickListener(this);
    }


    @Override
    public boolean onPreferenceClick(Preference preference) {

        String launchFragment = "";


        if (preference.getKey().equalsIgnoreCase("ftp_setup")) {
            launchFragment = MainPreferenceActivity.PREFERENCE_FRAGMENTS.FTP;
        }

        if (preference.getKey().equalsIgnoreCase("sftp_setup")) {
            launchFragment = MainPreferenceActivity.PREFERENCE_FRAGMENTS.SFTP;
        }


        if (!Strings.isNullOrEmpty(launchFragment)) {
            Intent intent = new Intent(getActivity(), MainPreferenceActivity.class);
            intent.putExtra("preference_fragment", launchFragment);
            startActivity(intent);
            return true;
        }

        return false;
    }
}
