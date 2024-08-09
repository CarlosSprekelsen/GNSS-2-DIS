package ets.acmi.gnssdislogger.ui.fragments.views;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;

import androidx.fragment.app.Fragment;

import com.afollestad.materialdialogs.MaterialDialog;

import org.slf4j.Logger;

import java.util.Objects;

import de.greenrobot.event.EventBus;
import ets.acmi.gnssdislogger.R;
import ets.acmi.gnssdislogger.common.EventBusHook;
import ets.acmi.gnssdislogger.common.PreferenceHelper;
import ets.acmi.gnssdislogger.common.Session;
import ets.acmi.gnssdislogger.common.Systems;
import ets.acmi.gnssdislogger.common.events.CommandEvents;
import ets.acmi.gnssdislogger.common.events.ServiceEvents;
import ets.acmi.gnssdislogger.common.slf4j.Logs;
import ets.acmi.gnssdislogger.loggers.Files;
import ets.acmi.gnssdislogger.ui.Dialogs;


public abstract class FragmentGenericView extends Fragment {

    private static final Logger LOG = Logs.of(FragmentGenericView.class);
    private final PreferenceHelper preferenceHelper = PreferenceHelper.getInstance();
    private final Session session = Session.getInstance();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        registerEventBus();
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
    public void onDestroy() {
        unregisterEventBus();
        super.onDestroy();
    }


    @EventBusHook
    public void onEventMainThread(ServiceEvents.LocationServicesUnavailable locationServicesUnavailable) {
        new MaterialDialog.Builder(Objects.requireNonNull(getActivity()))
                //.title("Location services unavailable")
                .content(R.string.gpsprovider_unavailable)
                .positiveText(R.string.ok)
                .negativeText(R.string.cancel)
                .onPositive((materialDialog, dialogAction) -> {
                    if (getActivity() != null) {
                        Intent settingsIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        getActivity().startActivity(settingsIntent);
                    }

                })
                .show();
    }


    public void requestToggleLogging() {

        if (!Systems.locationPermissionsGranted(getActivity())) {
            Dialogs.alert(getString(R.string.gnsslogger_permissions_rationale_title),
                    getString(R.string.gnsslogger_permissions_permanently_denied), getActivity());
            return;
        }

        if (session.isStarted()) {
            toggleLogging();
            return;
        }

        if (!Files.isAllowedToWriteTo(preferenceHelper.getGpsLoggerFolder())) {
            Dialogs.alert(getString(R.string.error), getString(R.string.pref_logging_file_no_permissions) + "<br />" + preferenceHelper.getGpsLoggerFolder(), getActivity());
            return;
        }

        if (preferenceHelper.shouldCreateCustomFile() && preferenceHelper.shouldAskCustomFileNameEachTime()) {

            Dialogs.autoCompleteText(getActivity(), "customfilename",
                    getString(R.string.new_file_custom_title), "ets.acmi.gnssdislogger",
                    preferenceHelper.getCustomFileName(), (which, dialog, enteredText) -> {

                        if (which == Dialogs.AutoCompleteCallback.CANCEL) {
                            return;
                        }

                        String originalFileName = preferenceHelper.getCustomFileName();

                        if (!originalFileName.equalsIgnoreCase(enteredText)) {
                            preferenceHelper.setCustomFileName(enteredText);
                        }

                        toggleLogging();
                    });


        } else {
            toggleLogging();
        }
    }

    private void toggleLogging() {
        EventBus.getDefault().post(new CommandEvents.RequestToggle());
    }

}
