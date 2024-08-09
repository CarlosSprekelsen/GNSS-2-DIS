package ets.acmi.gnssdislogger.ui.fragments.settings;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceActivity;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import org.slf4j.Logger;

import java.util.Map;

import ets.acmi.gnssdislogger.R;
import ets.acmi.gnssdislogger.common.PreferenceHelper;
import ets.acmi.gnssdislogger.common.PreferenceNames;
import ets.acmi.gnssdislogger.common.Strings;
import ets.acmi.gnssdislogger.common.Systems;
import ets.acmi.gnssdislogger.common.slf4j.Logs;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p/>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */

public class PreferenceFragmentGeneral extends PreferenceFragmentCompat implements Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener {

    private final Logger LOG = Logs.of(PreferenceFragmentGeneral.class);
    int aboutClickCounter = 0;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

        // Load the preferences from an XML resource
        setPreferencesFromResource(R.xml.settings_general, rootKey);


        findPreference("enableDisableGps").setOnPreferenceClickListener(this);
        //findPreference("license_credits").setOnPreferenceClickListener(this);
        findPreference("permissions_required").setOnPreferenceClickListener(this);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Preference hideNotificationPreference = findPreference("hide_notification_from_status_bar");
            hideNotificationPreference.setEnabled(false);
            hideNotificationPreference.setDefaultValue(false);
            ((SwitchPreferenceCompat) hideNotificationPreference).setChecked(false);
            hideNotificationPreference.setSummary(getString(R.string.hide_notification_from_status_bar_disallowed));
        }

        setCoordinatesFormatPreferenceItem();
        setLanguagesPreferenceItem();

        Preference aboutInfo = findPreference("about_version_info");
        try {

            aboutInfo.setTitle("GPSLogger version " + getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0).versionName);
        } catch (PackageManager.NameNotFoundException e) {
        }
    }

    private void setLanguagesPreferenceItem() {
        ListPreference languages = findPreference("change_language");

        Map<String, String> localeDisplayNames = Strings.getAvailableLocales(getActivity());

        String[] locales = localeDisplayNames.keySet().toArray(new String[0]);
        String[] displayValues = localeDisplayNames.values().toArray(new String[0]);

        languages.setEntries(displayValues);
        languages.setEntryValues(locales);
        languages.setDefaultValue("en");
        languages.setOnPreferenceChangeListener(this);
    }

    private void setCoordinatesFormatPreferenceItem() {
        ListPreference coordinatesFormats = findPreference("coordinate_display_format");
        String[] coordinateDisplaySamples = new String[]{"12° 34' 56.7890\" S", "12° 34.5678' S", "-12.345678"};
        coordinatesFormats.setEntries(coordinateDisplaySamples);
        coordinatesFormats.setEntryValues(new String[]{PreferenceNames.DegreesDisplayFormat.DEGREES_MINUTES_SECONDS.toString(), PreferenceNames.DegreesDisplayFormat.DEGREES_DECIMAL_MINUTES.toString(), PreferenceNames.DegreesDisplayFormat.DECIMAL_DEGREES.toString()});
        coordinatesFormats.setDefaultValue("0");
        coordinatesFormats.setOnPreferenceChangeListener(this);
        coordinatesFormats.setSummary(coordinateDisplaySamples[PreferenceHelper.getInstance().getDisplayLatLongFormat().ordinal()]);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        Systems.onRequestPermissionsResult(requestCode, permissions, grantResults, getActivity());

    }

    @Override
    public boolean onPreferenceClick(Preference preference) {

        if (preference.getKey().equals("permissions_required")) {
            if (!Systems.hasUserGrantedAllNecessaryPermissions(getActivity())) {
                Systems.askUserForPermissions(getActivity(), this);
            }

            return true;
        }

        if (preference.getKey().equals("enableDisableGps")) {
            startActivity(new Intent("android.settings.LOCATION_SOURCE_SETTINGS"));
            return true;
        }

        return false;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {

        if (preference.getKey().equals("change_language")) {
            PreferenceHelper.getInstance().setUserSpecifiedLocale((String) newValue);
            LOG.debug("Language chosen: " + PreferenceHelper.getInstance().getUserSpecifiedLocale());
            return true;
        }
        if (preference.getKey().equals("coordinate_display_format")) {
            PreferenceHelper.getInstance().setDisplayLatLongFormat(PreferenceNames.DegreesDisplayFormat.valueOf(newValue.toString()));
            LOG.debug("Coordinate format chosen: " + PreferenceHelper.getInstance().getDisplayLatLongFormat());
            setCoordinatesFormatPreferenceItem();
            return true;
        }
        return false;
    }
}
