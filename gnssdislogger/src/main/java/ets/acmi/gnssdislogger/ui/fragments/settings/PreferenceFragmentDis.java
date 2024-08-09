package ets.acmi.gnssdislogger.ui.fragments.settings;

import android.os.Bundle;

import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import de.greenrobot.event.EventBus;
import ets.acmi.gnssdislogger.R;
import ets.acmi.gnssdislogger.common.PreferenceHelper;
import ets.acmi.gnssdislogger.common.events.CommandEvents;
import ets.acmi.gnssdislogger.senders.PreferenceValidator;
import ets.acmi.gnssdislogger.ui.Dialogs;

public class PreferenceFragmentDis extends PreferenceFragmentCompat implements
        PreferenceValidator,
        Preference.OnPreferenceChangeListener,
        Preference.OnPreferenceClickListener {

    private final PreferenceHelper preferenceHelper = PreferenceHelper.getInstance();


    private static boolean isNumeric(String str) {
        for (char c : str.toCharArray()) {
            if (!Character.isDigit(c)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        // Load the preferences from an XML resource
        setPreferencesFromResource(R.xml.settings_dis, rootKey);

        EditTextPreference serveraddressPreference = findPreference("dis_udp_host");
        assert serveraddressPreference != null;
        serveraddressPreference.setSummary(preferenceHelper.getDISServer());
        serveraddressPreference.setText(preferenceHelper.getDISServer());
        serveraddressPreference.setOnPreferenceChangeListener(this);

        EditTextPreference serverportPreference = findPreference("dis_udp_port");
        assert serverportPreference != null;
        serverportPreference.setSummary(preferenceHelper.getDISServerPort());
        serverportPreference.setText(preferenceHelper.getDISServerPort());
        serverportPreference.setOnPreferenceChangeListener(this);

        EditTextPreference sitePreference = findPreference("dis_site");
        assert sitePreference != null;
        sitePreference.setSummary(preferenceHelper.getDISSite());
        sitePreference.setText(preferenceHelper.getDISSite());
        sitePreference.setOnPreferenceChangeListener(this);

        EditTextPreference applicationPreference = findPreference("dis_application");
        assert applicationPreference != null;
        applicationPreference.setSummary(preferenceHelper.getDISApplication());
        applicationPreference.setText(preferenceHelper.getDISApplication());
        applicationPreference.setOnPreferenceChangeListener(this);

        EditTextPreference exercisePreference = findPreference("dis_exercise");
        assert exercisePreference != null;
        exercisePreference.setSummary(preferenceHelper.getDISExercise());
        exercisePreference.setText(preferenceHelper.getDISExercise());
        exercisePreference.setOnPreferenceChangeListener(this);

        EditTextPreference markingPreference = findPreference("dis_marking");
        assert markingPreference != null;
        markingPreference.setSummary(preferenceHelper.getDISMarking());
        markingPreference.setText(preferenceHelper.getDISMarking());
        markingPreference.setOnPreferenceChangeListener(this);

        EditTextPreference entityIdPreference = findPreference("dis_entity_id");
        assert entityIdPreference != null;
        entityIdPreference.setSummary(preferenceHelper.getDISEntityid());
        entityIdPreference.setText(preferenceHelper.getDISEntityid());
        entityIdPreference.setOnPreferenceChangeListener(this);

        ListPreference forceIdPreference = findPreference("dis_force_id");
        assert forceIdPreference != null;
        forceIdPreference.setOnPreferenceChangeListener(this);

        ListPreference enumerationPreference = findPreference("dis_enumeration");
        assert enumerationPreference != null;
        enumerationPreference.setOnPreferenceChangeListener(this);
    }

    // onPreferenceChange we send an EventBus to inform other process tu update Icons and call signs
    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (!((preference.getKey().equals("dis_force_id")) || (preference.getKey().equals("dis_enumeration"))))
        {
            preference.setSummary(newValue.toString());
        }
        EventBus.getDefault().post(new CommandEvents.disPreferenceChanged(preference.getKey()));
        return true;
    }

    public boolean onPreferenceClick(Preference preference) {
        if (!isFormValid()) {
            Dialogs.alert(getString(R.string.ftp_invalid_settings),
                    getString(R.string.ftp_invalid_summary),
                    getActivity());
            return false;
        }
        return true;
    }

    private boolean isFormValid() {
//TODO: Review logic to validate the form without crashing it.
        EditTextPreference txtDisServer = findPreference("dis_udp_host");
        EditTextPreference txtDisServerPort =  findPreference("dis_udp_port");
        EditTextPreference txtDisSite =  findPreference("dis_site");
        EditTextPreference txtDisApplication =  findPreference("dis_application");
        EditTextPreference txtDisExercise =  findPreference("dis_exercise");
        ListPreference txtDisForceId =  findPreference("dis_force_id");
        ListPreference txtDisEnumeration =  findPreference("dis_enumeration");
        EditTextPreference txtDisMarking =  findPreference("dis_marking");

        assert txtDisServer != null;
        assert txtDisForceId != null;
        assert txtDisMarking != null;
        assert txtDisEnumeration != null;
        assert txtDisSite != null;
        assert txtDisServerPort != null;
        assert txtDisExercise != null;
        assert txtDisApplication != null;
        return  txtDisServer.getText() != null && txtDisServer.getText().length() > 0
                && txtDisServerPort.getText() != null && isNumeric(txtDisServerPort.getText())
                && txtDisSite.getText() != null && isNumeric(txtDisSite.getText())
                && txtDisApplication.getText() != null && isNumeric(txtDisApplication.getText())
                && txtDisExercise.getText() != null && isNumeric(txtDisExercise.getText())
                && txtDisForceId.getEntry() != null
                && txtDisEnumeration.getEntry() != null && txtDisEnumeration.getEntry().length() > 0
                && txtDisMarking.getText() != null && txtDisMarking.getText().length() > 0;
    }

    @Override
    public boolean isValid() {
        return isFormValid();
    }
}
