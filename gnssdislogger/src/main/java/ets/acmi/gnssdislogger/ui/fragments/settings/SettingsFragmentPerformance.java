package ets.acmi.gnssdislogger.ui.fragments.settings;


import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;

import ets.acmi.gnssdislogger.R;

public class SettingsFragmentPerformance extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        // Load the preferences from an XML resource
        setPreferencesFromResource(R.xml.settings_performance, rootKey);
    }


}
