package ets.acmi.gnssdislogger.ui.fragments.settings;

import android.os.Bundle;
import android.text.InputType;

import androidx.core.text.HtmlCompat;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import com.afollestad.materialdialogs.MaterialDialog;

import org.slf4j.Logger;

import java.io.File;

import ets.acmi.gnssdislogger.R;
import ets.acmi.gnssdislogger.common.PreferenceHelper;
import ets.acmi.gnssdislogger.common.Strings;
import ets.acmi.gnssdislogger.common.slf4j.Logs;
import ets.acmi.gnssdislogger.loggers.Files;
import ets.acmi.gnssdislogger.ui.Dialogs;

public class PreferenceFragmentFileLogging extends PreferenceFragmentCompat
        implements
        Preference.OnPreferenceClickListener,
        Preference.OnPreferenceChangeListener {

    private static final Logger LOG = Logs.of(PreferenceFragmentFileLogging.class);
    private static final PreferenceHelper preferenceHelper = PreferenceHelper.getInstance();

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

        // Load the preferences from an XML resource
        setPreferencesFromResource(R.xml.settings_filelogging, rootKey);


        EditTextPreference gpsloggerFolder = findPreference("gnsslogger_folder");

        String gpsLoggerFolderPath = preferenceHelper.getGpsLoggerFolder();
        gpsloggerFolder.setDefaultValue(gpsLoggerFolderPath);
        gpsloggerFolder.setText(gpsLoggerFolderPath);
        gpsloggerFolder.setSummary(gpsLoggerFolderPath);
        gpsloggerFolder.setOnPreferenceChangeListener(this);

        if (!(new File(gpsLoggerFolderPath)).canWrite()) {
            gpsloggerFolder.setSummary(HtmlCompat.fromHtml("<font color='red'>" + gpsLoggerFolderPath + "</font>", HtmlCompat.FROM_HTML_MODE_LEGACY));
        }

        SwitchPreferenceCompat logGpx = findPreference("log_gpx");
        SwitchPreferenceCompat logGpx11 = findPreference("log_gpx_11");
        logGpx11.setTitle("      " + logGpx11.getTitle());
        logGpx11.setSummary("      " + logGpx11.getSummary());


        logGpx.setOnPreferenceChangeListener(this);
        logGpx11.setEnabled(logGpx.isChecked());

        SwitchPreferenceCompat logEag = findPreference("log_eag_enabled");
        logEag.setEnabled(logEag.isChecked());

        /*
          Logging Details - New file creation
         */
        ListPreference newFilePref = findPreference("new_file_creation");
        newFilePref.setOnPreferenceChangeListener(this);
        /* Trigger artificially the listener and perform validations. */
        newFilePref.getOnPreferenceChangeListener()
                .onPreferenceChange(newFilePref, newFilePref.getValue());

        SwitchPreferenceCompat chkfile_prefix_serial = findPreference("new_file_prefix_serial");
        if (Strings.isNullOrEmpty(Strings.getBuildSerial())) {
            chkfile_prefix_serial.setEnabled(false);
            chkfile_prefix_serial.setSummary("This option not available on older phones or if a serial id is not present");
        } else {
            chkfile_prefix_serial.setEnabled(true);
            chkfile_prefix_serial.setSummary(chkfile_prefix_serial.getSummary().toString() + "(" + Strings.getBuildSerial() + ")");
        }


        findPreference("new_file_custom_name").setOnPreferenceClickListener(this);
    }


    @Override
    public void onResume() {
        super.onResume();

        setPreferencesEnabledDisabled();
    }


    @Override
    public boolean onPreferenceClick(Preference preference) {


        if (preference.getKey().equalsIgnoreCase("new_file_custom_name")) {


            new MaterialDialog.Builder(getActivity())
                    .title(R.string.new_file_custom_title)
                    .content(R.string.new_file_custom_message)
                    .positiveText(R.string.ok)
                    .negativeText(R.string.cancel)
                    .inputType(InputType.TYPE_CLASS_TEXT)
                    .negativeText(R.string.cancel)
                    .input(getString(R.string.letters_numbers), preferenceHelper.getCustomFileName(), (materialDialog, input) -> preferenceHelper.setCustomFileName(input.toString()))
                    .show();

        }

        return false;
    }


    @Override
    public boolean onPreferenceChange(final Preference preference, Object newValue) {

        if (preference.getKey().equalsIgnoreCase("gnsslogger_folder")) {

            LOG.debug("Directory chosen - " + newValue);

            if (Strings.isNullOrEmpty(newValue.toString())) {
                //Special case, reset the preference if value is empty
                newValue = Files.storageFolder(getActivity()).getAbsolutePath();
                ((EditTextPreference) findPreference("gnsslogger_folder")).setText(newValue.toString());
                findPreference("gnsslogger_folder").setSummary(newValue.toString());
                preferenceHelper.setGpsLoggerFolder(newValue.toString());
                return false;
            }

            try {
                File chosenDirectory = new File(newValue.toString());
                chosenDirectory.mkdirs();
            } catch (Exception e) {
                LOG.error("Could not create chosen directory path", e);
                Dialogs.error(getString(R.string.error), getString(R.string.pref_logging_file_no_permissions), e.getMessage(), e, getActivity());
                return false;
            }

            if (!Files.isAllowedToWriteTo(newValue.toString())) {
                Dialogs.alert(getString(R.string.error), getString(R.string.pref_logging_file_no_permissions), getActivity());
                return false;
            } else {
                findPreference("gnsslogger_folder").setSummary(newValue.toString());
                return true;
            }

        }

        if (preference.getKey().equalsIgnoreCase("log_gpx")) {
            SwitchPreferenceCompat logGpx11 = findPreference("log_gpx_11");
            logGpx11.setEnabled((Boolean) newValue);
            return true;
        }

        if (preference.getKey().equals("new_file_creation")) {

            findPreference("new_file_custom_each_time").setEnabled(newValue.equals("custom"));
            findPreference("new_file_custom_name").setEnabled(newValue.equals("custom"));
            findPreference("new_file_custom_keep_changing").setEnabled(newValue.equals("custom"));
            findPreference("new_file_prefix_serial").setEnabled(!newValue.equals("custom"));


            return true;
        }
        return false;
    }

    private void setPreferencesEnabledDisabled() {

        Preference prefFileCustomName = findPreference("new_file_custom_name");
        Preference prefAskEachTime = findPreference("new_file_custom_each_time");
        Preference prefSerialPrefix = findPreference("new_file_prefix_serial");
        Preference prefDynamicFileName = findPreference("new_file_custom_keep_changing");

        prefFileCustomName.setEnabled(preferenceHelper.shouldCreateCustomFile());
        prefAskEachTime.setEnabled(preferenceHelper.shouldCreateCustomFile());
        prefSerialPrefix.setEnabled(!preferenceHelper.shouldCreateCustomFile());
        prefDynamicFileName.setEnabled(preferenceHelper.shouldCreateCustomFile());
    }


}
