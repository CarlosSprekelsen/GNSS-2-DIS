package ets.acmi.gnssdislogger.ui.fragments.settings;

import android.os.Bundle;
import android.view.WindowManager;
import android.widget.EditText;

import androidx.appcompat.widget.AppCompatEditText;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.afollestad.materialdialogs.MaterialDialog;

import org.slf4j.Logger;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;

import ets.acmi.gnssdislogger.R;
import ets.acmi.gnssdislogger.common.PreferenceHelper;
import ets.acmi.gnssdislogger.common.network.Networks;
import ets.acmi.gnssdislogger.common.network.ServerType;
import ets.acmi.gnssdislogger.common.slf4j.Logs;
import ets.acmi.gnssdislogger.senders.PreferenceValidator;
import ets.acmi.gnssdislogger.ui.Dialogs;


public class PreferenceFragmentCustomUrl extends PreferenceFragmentCompat implements
        PreferenceValidator,
        Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {

    private static final Logger LOG = Logs.of(PreferenceFragmentCustomUrl.class);


    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

        // Load the preferences from an XML resource
        setPreferencesFromResource(R.xml.settings_customurl, rootKey);

        EditTextPreference urlPathPreference = findPreference("log_customurl_url");
        urlPathPreference.setSummary(PreferenceHelper.getInstance().getCustomLoggingUrl());
        urlPathPreference.setText(PreferenceHelper.getInstance().getCustomLoggingUrl());
        urlPathPreference.setOnPreferenceChangeListener(this);

        findPreference("customurl_legend_1").setOnPreferenceClickListener(this);
        findPreference("customurl_validatecustomsslcert").setOnPreferenceClickListener(this);
        findPreference("log_customurl_basicauth").setOnPreferenceClickListener(this);

    }


    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference.getKey().equals("log_customurl_url")) {
            preference.setSummary(newValue.toString());
        }
        return true;
    }


    @Override
    public boolean isValid() {
        return true;
    }


    @Override
    public boolean onPreferenceClick(Preference preference) {
        switch (preference.getKey()) {
            case "customurl_legend_1":

                String legend1 = MessageFormat.format("{0} %LAT<br />{1} %LON<br />{2} %DESC<br />{3} %SAT<br />{4} %ALT<br />" +
                                "{5} %SPD<br />{6} %ACC<br />{7} %DIR<br />{8} %PROV<br />{9} %TIMESTAMP<br />" +
                                "{10} %TIME<br />{11} %DATE<br />{12} %STARTTIMESTAMP<br />{13} %BATT<br />{14} %AID<br />{15} %SER<br />" +
                                "{16} %ACT<br />{17} %FILENAME<br />{18} %PROFILE<br />" +
                                "{19} %HDOP<br />{20} %VDOP<br />{21} %PDOP<br />{22} %DIST",
                        getString(R.string.txt_latitude), getString(R.string.txt_longitude), getString(R.string.txt_annotation),
                        getString(R.string.txt_satellites), getString(R.string.txt_altitude), getString(R.string.txt_speed),
                        getString(R.string.txt_accuracy), getString(R.string.txt_direction), getString(R.string.txt_provider),
                        getString(R.string.txt_timestamp_epoch),
                        getString(R.string.txt_time_isoformat),
                        getString(R.string.txt_date_isoformat),
                        getString(R.string.txt_starttimestamp_epoch),
                        getString(R.string.txt_battery), "Android ID ", "Serial ", getString(R.string.txt_activity), getString(R.string.summary_current_filename), "Profile:", "HDOP:", "VDOP:", "PDOP:", getString(R.string.txt_travel_distance));
                Dialogs.alert(getString(R.string.parameters), legend1, getActivity());

                break;
            case "customurl_validatecustomsslcert":

                try {
                    URL u = new URL(PreferenceHelper.getInstance().getCustomLoggingUrl());
                    Networks.beginCertificateValidationWorkflow(getActivity(), u.getHost(), u.getPort() < 0 ? u.getDefaultPort() : u.getPort(), ServerType.HTTPS);
                } catch (MalformedURLException e) {
                    LOG.error("Could not start certificate validation", e);
                }

                return true;
            case "log_customurl_basicauth":
                MaterialDialog alertDialog = new MaterialDialog.Builder(getActivity())
                        .title(R.string.customurl_http_basicauthentication)
                        .customView(R.layout.customurl_basicauthview, true)

                        .autoDismiss(false)
                        .negativeText(R.string.cancel)
                        .positiveText(R.string.ok)
                        .onPositive((materialDialog, dialogAction) -> {
                            String basicAuthUsername = ((EditText) materialDialog.getView().findViewById(R.id.basicauth_username)).getText().toString();
                            PreferenceHelper.getInstance().setCustomLoggingBasicAuthUsername(basicAuthUsername);

                            String basicAuthPassword = ((EditText) materialDialog.getView().findViewById(R.id.basicauth_pwd)).getText().toString();
                            PreferenceHelper.getInstance().setCustomLoggingBasicAuthPassword(basicAuthPassword);

                            materialDialog.dismiss();
                        })
                        .onNegative((materialDialog, dialogAction) -> materialDialog.dismiss())
                        .build();


                final AppCompatEditText bauthUsernameText = alertDialog.getCustomView().findViewById(R.id.basicauth_username);
                bauthUsernameText.setText(PreferenceHelper.getInstance().getCustomLoggingBasicAuthUsername());
                final AppCompatEditText bauthPwdText = alertDialog.getCustomView().findViewById(R.id.basicauth_pwd);
                bauthPwdText.setText(PreferenceHelper.getInstance().getCustomLoggingBasicAuthPassword());

                alertDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
                alertDialog.show();
                return true;
        }

        return false;
    }


}
