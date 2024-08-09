package ets.acmi.gnssdislogger.ui.activity;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceFragmentCompat;

import org.slf4j.Logger;

import ets.acmi.gnssdislogger.R;
import ets.acmi.gnssdislogger.common.PreferenceHelper;
import ets.acmi.gnssdislogger.common.Systems;
import ets.acmi.gnssdislogger.common.slf4j.Logs;
import ets.acmi.gnssdislogger.senders.PreferenceValidator;
import ets.acmi.gnssdislogger.ui.Dialogs;
import ets.acmi.gnssdislogger.ui.fragments.settings.PreferenceFragmentAutoUpload;
import ets.acmi.gnssdislogger.ui.fragments.settings.PreferenceFragmentCustomUrl;
import ets.acmi.gnssdislogger.ui.fragments.settings.PreferenceFragmentDis;
import ets.acmi.gnssdislogger.ui.fragments.settings.PreferenceFragmentFileLogging;
import ets.acmi.gnssdislogger.ui.fragments.settings.PreferenceFragmentFtp;
import ets.acmi.gnssdislogger.ui.fragments.settings.PreferenceFragmentGeneral;
import ets.acmi.gnssdislogger.ui.fragments.settings.PreferenceFragmentLiveLogging;
import ets.acmi.gnssdislogger.ui.fragments.settings.PreferenceFragmentSftp;
import ets.acmi.gnssdislogger.ui.fragments.settings.SettingsFragmentPerformance;


public class MainPreferenceActivity extends AppCompatActivity {

    private static final Logger LOG = Logs.of(MainPreferenceActivity.class);

    private PreferenceFragmentCompat preferenceFragmentCompat = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Systems.setLocale(PreferenceHelper.getInstance().getUserSpecifiedLocale(), getBaseContext(),getResources());
        setContentView(R.layout.activity_preferences);


        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }


        String whichFragment = PREFERENCE_FRAGMENTS.GENERAL;

        if (getIntent().getExtras() != null) {
            whichFragment = getIntent().getExtras().getString("preference_fragment");
        }


        switch (whichFragment) {
            case PREFERENCE_FRAGMENTS.GENERAL:
                setTitle(R.string.settings_screen_name);
                preferenceFragmentCompat = new PreferenceFragmentGeneral();
                break;
            case PREFERENCE_FRAGMENTS.LOGGING:
                setTitle(R.string.file_format_settings);
                preferenceFragmentCompat = new PreferenceFragmentFileLogging();
                break;
            case PREFERENCE_FRAGMENTS.LIVE:
                setTitle(R.string.live_format_settings);
                preferenceFragmentCompat = new PreferenceFragmentLiveLogging();
                break;
            case PREFERENCE_FRAGMENTS.PERFORMANCE:
                setTitle(R.string.pref_performance_title);
                preferenceFragmentCompat = new SettingsFragmentPerformance();
                break;
            case PREFERENCE_FRAGMENTS.UPLOAD:
                setTitle(R.string.title_drawer_uploadsettings);
                preferenceFragmentCompat = new PreferenceFragmentAutoUpload();
                break;
            case PREFERENCE_FRAGMENTS.FTP:
                setTitle(R.string.ftp_setup_title);
                preferenceFragmentCompat = new PreferenceFragmentFtp();
                break;
            case PREFERENCE_FRAGMENTS.CUSTOMURL:
                setTitle(R.string.log_customurl_title);
                preferenceFragmentCompat = new PreferenceFragmentCustomUrl();
                break;
            case PREFERENCE_FRAGMENTS.SFTP:
                setTitle(R.string.sftp_setup_title);
                preferenceFragmentCompat = new PreferenceFragmentSftp();
                break;
            case PREFERENCE_FRAGMENTS.DIS:
                setTitle(R.string.dis_setup_title);
                preferenceFragmentCompat = new PreferenceFragmentDis();
                break;
        }

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.content_frame, preferenceFragmentCompat)
                .commit();

    }


    @Override
    public void onBackPressed() {

        if (isFormValid()) {
            super.onBackPressed();
        }
    }

    private boolean isFormValid() {
        if (preferenceFragmentCompat instanceof PreferenceValidator) {
            if (!((PreferenceValidator) preferenceFragmentCompat).isValid()) {
                Dialogs.alert(getString(R.string.ftp_invalid_settings),
                        getString(R.string.ftp_invalid_summary),
                        this);
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        final int id = item.getItemId();
        return id == android.R.id.home && !isFormValid();

    }

    public static class PREFERENCE_FRAGMENTS {
        public static final String GENERAL = "PreferenceFragmentGeneral";
        public static final String LOGGING = "PreferenceFragmentFileLogging";
        public static final String LIVE = "PreferenceFragmentLiveLogging";
        public static final String PERFORMANCE = "SettingsFragmentPerformance";
        public static final String UPLOAD = "PreferenceFragmentAutoUpload";
        public static final String FTP = "PreferenceFragmentFtp";
        public static final String CUSTOMURL = "PreferenceFragmentCustomUrl";
        public static final String SFTP = "PreferenceFragmentSftp";
        public static final String DIS = "PreferenceFragmentDis";
    }

}
