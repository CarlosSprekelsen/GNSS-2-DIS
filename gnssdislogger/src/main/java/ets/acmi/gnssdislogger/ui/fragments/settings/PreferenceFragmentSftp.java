package ets.acmi.gnssdislogger.ui.fragments.settings;

import android.os.Bundle;

import androidx.core.content.ContextCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import org.slf4j.Logger;

import java.io.File;

import de.greenrobot.event.EventBus;
import ets.acmi.gnssdislogger.R;
import ets.acmi.gnssdislogger.common.EventBusHook;
import ets.acmi.gnssdislogger.common.PreferenceHelper;
import ets.acmi.gnssdislogger.common.Strings;
import ets.acmi.gnssdislogger.common.events.UploadEvents;
import ets.acmi.gnssdislogger.common.slf4j.Logs;
import ets.acmi.gnssdislogger.loggers.Files;
import ets.acmi.gnssdislogger.senders.PreferenceValidator;
import ets.acmi.gnssdislogger.senders.sftp.SFTPManager;
import ets.acmi.gnssdislogger.ui.Dialogs;

public class PreferenceFragmentSftp extends PreferenceFragmentCompat implements Preference.OnPreferenceClickListener, PreferenceValidator, Preference.OnPreferenceChangeListener {

    private static final Logger LOG = Logs.of(PreferenceFragmentSftp.class);
    private SFTPManager manager;
    private final PreferenceHelper preferenceHelper = PreferenceHelper.getInstance();


    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        LOG.debug("on create");

        // Load the preferences from an XML resource
        setPreferencesFromResource(R.xml.settings_sftp, rootKey);


        manager = new SFTPManager(preferenceHelper);

        findPreference("sftp_validateserver").setOnPreferenceClickListener(this);
        findPreference("sftp_reset_authorisation").setOnPreferenceClickListener(this);
        findPreference("sftp_private_key_path").setOnPreferenceChangeListener(this);
        findPreference("sftp_private_key_path").setSummary(preferenceHelper.getSFTPPrivateKeyFilePath());
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


        if (preference.getKey().equals("sftp_validateserver")) {
            uploadTestFile();
        } else if (preference.getKey().equals("sftp_reset_authorisation")) {
            preferenceHelper.setSFTPKnownHostKey("");
            preferenceHelper.setSFTPPrivateKeyFilePath("");
            getActivity().finish();
        }

        return false;
    }

    private void uploadTestFile() {
        Dialogs.progress(getActivity(), getString(R.string.please_wait), getString(R.string.please_wait));

        File testFile = null;
        try {
            testFile = Files.createTestFile();
        } catch (Exception ex) {
            LOG.error("Could not create local test file", ex);
            EventBus.getDefault().post(new UploadEvents.SFTP().failed("Could not create local test file", ex));
        }

        manager.uploadFile(testFile);
    }

    @Override
    public boolean isValid() {
        return !manager.hasUserAllowedAutoSending() || manager.isAvailable();
    }

    @EventBusHook
    public void onEventMainThread(final UploadEvents.SFTP o) {
        LOG.debug("SFTP Event completed, success: " + o.success);

        Dialogs.hideProgress();
        if (!o.success) {
            if (!Strings.isNullOrEmpty(o.hostKey)) {
                LOG.debug("SFTP HostKey " + o.hostKey);
                LOG.debug("SFTP Fingerprint " + o.fingerprint);
                String codeGreen = Integer.toHexString(ContextCompat.getColor(getActivity(), R.color.accentColorComplementary)).substring(2);
                String promptMessage = String.format("Fingerprint: <br /><font color='#%s' face='monospace'>%s</font> <br /><br /> Host Key: <br /><font color='#%s' face='monospace'>%s</font>",
                        codeGreen, o.fingerprint, codeGreen, o.hostKey);

                Dialogs.alert(getString(R.string.sftp_validate_accept_host_key), promptMessage, getActivity(), true, which -> {
                    if (which == Dialogs.MessageBoxCallback.OK) {
                        preferenceHelper.setSFTPKnownHostKey(o.hostKey);
                        uploadTestFile();
                    }
                });
            } else {
                Dialogs.error(getString(R.string.sorry), "SFTP Test Failed", o.message, o.throwable, getActivity());
            }
        } else {
            Dialogs.alert(getString(R.string.success), "SFTP Test Succeeded", getActivity());
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {

        if (preference.getKey().equals("sftp_private_key_path")) {
            findPreference("sftp_private_key_path").setSummary(newValue.toString());
            return true;
        }


        return false;
    }
}
