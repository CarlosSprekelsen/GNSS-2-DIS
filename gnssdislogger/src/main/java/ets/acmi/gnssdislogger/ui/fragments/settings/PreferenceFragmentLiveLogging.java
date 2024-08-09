package ets.acmi.gnssdislogger.ui.fragments.settings;

import android.content.Intent;
import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import org.slf4j.Logger;

import ets.acmi.gnssdislogger.R;
import ets.acmi.gnssdislogger.common.PreferenceHelper;
import ets.acmi.gnssdislogger.common.slf4j.Logs;
import ets.acmi.gnssdislogger.ui.activity.MainPreferenceActivity;

public class PreferenceFragmentLiveLogging extends PreferenceFragmentCompat
        implements
        Preference.OnPreferenceChangeListener {

    private static final Logger LOG = Logs.of(PreferenceFragmentLiveLogging.class);
    private static PreferenceHelper preferenceHelper = PreferenceHelper.getInstance();

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

        // Load the preferences from an XML resource
        setPreferencesFromResource(R.xml.settings_onlinelogging, rootKey);

        findPreference("log_customurl_enabled").setOnPreferenceChangeListener(this);
        findPreference("log_dis_enabled").setOnPreferenceChangeListener(this);
    }


    @Override
    public boolean onPreferenceChange(final Preference preference, Object newValue) {

        if (preference.getKey().equalsIgnoreCase("log_customurl_enabled")) {

            // Bug in SwitchPreference: http://stackoverflow.com/questions/19503931/switchpreferences-calls-multiple-times-the-onpreferencechange-method
            // Check if isChecked == false && newValue == true
            if (!((SwitchPreferenceCompat) preference).isChecked() && (Boolean) newValue) {
                Intent targetActivity = new Intent(getActivity(), MainPreferenceActivity.class);
                targetActivity.putExtra("preference_fragment", MainPreferenceActivity.PREFERENCE_FRAGMENTS.CUSTOMURL);
                startActivity(targetActivity);
            }

            return true;
        }

        if (preference.getKey().equalsIgnoreCase("log_dis_enabled")) {

            if (!((SwitchPreferenceCompat) preference).isChecked() && (Boolean) newValue) {
                Intent targetActivity = new Intent(getActivity(), MainPreferenceActivity.class);
                targetActivity.putExtra("preference_fragment", MainPreferenceActivity.PREFERENCE_FRAGMENTS.DIS);
                startActivity(targetActivity);
            }

            return true;
        }

        return false;
    }


}
