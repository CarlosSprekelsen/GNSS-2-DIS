package ets.acmi.gnssdislogger.ui.fragments.settings;

import android.os.Bundle;
import android.text.TextUtils;

import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import org.slf4j.Logger;

import de.greenrobot.event.EventBus;
import ets.acmi.gnssdislogger.R;
import ets.acmi.gnssdislogger.common.EventBusHook;
import ets.acmi.gnssdislogger.common.PreferenceHelper;
import ets.acmi.gnssdislogger.common.Strings;
import ets.acmi.gnssdislogger.common.events.UploadEvents;
import ets.acmi.gnssdislogger.common.network.Networks;
import ets.acmi.gnssdislogger.common.network.ServerType;
import ets.acmi.gnssdislogger.common.slf4j.Logs;
import ets.acmi.gnssdislogger.senders.PreferenceValidator;
import ets.acmi.gnssdislogger.senders.ftp.FtpManager;
import ets.acmi.gnssdislogger.ui.Dialogs;


public class PreferenceFragmentFtp
        extends PreferenceFragmentCompat implements Preference.OnPreferenceClickListener, PreferenceValidator {
    private static final Logger LOG = Logs.of(PreferenceFragmentFtp.class);
    private static final PreferenceHelper preferenceHelper = PreferenceHelper.getInstance();


    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

        setPreferencesFromResource(R.xml.settings_ftp, rootKey);

        findPreference("ftp_test").setOnPreferenceClickListener(this);
        findPreference("ftp_validatecustomsslcert").setOnPreferenceClickListener(this);

        registerEventBus();
    }

    @Override
    public void onDestroy() {

        unregisterEventBus();
        super.onDestroy();
    }

    private void registerEventBus() {
        EventBus.getDefault().register(this);
    }

    private void unregisterEventBus() {
        try {
            EventBus.getDefault().unregister(this);
        } catch (Throwable t) {
            //this may crash if registration did not go through. just be safe
        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {

        if (preference.getKey().equals("ftp_validatecustomsslcert")) {

            Networks.beginCertificateValidationWorkflow(getActivity(), preferenceHelper.getFtpServerName(), preferenceHelper.getFtpPort(), ServerType.FTP);

        } else {
            FtpManager helper = new FtpManager(preferenceHelper);

            EditTextPreference servernamePreference = findPreference("ftp_server");
            EditTextPreference usernamePreference = findPreference("ftp_username");
            EditTextPreference passwordPreference = findPreference("ftp_password");
            EditTextPreference portPreference = findPreference("ftp_port");
            SwitchPreferenceCompat useFtpsPreference = findPreference("ftp_useftps");
            EditTextPreference sslTlsPreference = findPreference("ftp_ssltls");
            SwitchPreferenceCompat implicitPreference = findPreference("ftp_implicit");
            EditTextPreference directoryPreference = findPreference("ftp_directory");

            if (!helper.validSettings(servernamePreference.getText(), usernamePreference.getText(), passwordPreference.getText(),
                    Strings.toInt(portPreference.getText(), 21),
                    useFtpsPreference.isChecked(), sslTlsPreference.getText(),
                    implicitPreference.isChecked())) {
                Dialogs.alert(getString(R.string.ftp_invalid_settings),
                        getString(R.string.ftp_invalid_summary),
                        getActivity());
                return false;
            }

            Dialogs.progress(getActivity(), getString(R.string.ftp_testing),
                    getString(R.string.please_wait));


            helper.testFtp(servernamePreference.getText(), usernamePreference.getText(), passwordPreference.getText(),
                    directoryPreference.getText(), Strings.toInt(portPreference.getText(), 21), useFtpsPreference.isChecked(),
                    sslTlsPreference.getText(), implicitPreference.isChecked());
        }


        return true;
    }


    @Override
    public boolean isValid() {
        FtpManager manager = new FtpManager(preferenceHelper);

        return !manager.hasUserAllowedAutoSending() || manager.isAvailable();
    }

    @EventBusHook
    public void onEventMainThread(UploadEvents.Ftp o) {
        LOG.debug("FTP Event completed, success: " + o.success);
        Dialogs.hideProgress();
        if (!o.success) {
            String ftpMessages = (o.ftpMessages == null) ? "" : TextUtils.join("", o.ftpMessages);
            Dialogs.error(getString(R.string.sorry), "FTP Test Failed", o.message + "\r\n" + ftpMessages, o.throwable, getActivity());
        } else {
            Dialogs.alert(getString(R.string.success), "FTP Test Succeeded", getActivity());
        }
    }
}