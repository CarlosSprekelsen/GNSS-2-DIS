package ets.acmi.gnssdislogger.common;


import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.BatteryManager;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceFragmentCompat;

import org.slf4j.Logger;

import java.util.List;
import java.util.Locale;

import ets.acmi.gnssdislogger.R;
import ets.acmi.gnssdislogger.common.slf4j.Logs;
import ets.acmi.gnssdislogger.ui.Dialogs;

public class Systems {

    private final static int REQUEST_PERMISSION_CODE = 2191;
    private static final Logger LOG = Logs.of(Systems.class);

    public static int getBatteryLevel(Context context) {
        Intent batteryIntent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int level = batteryIntent != null ? batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) : 0;
        int scale = batteryIntent != null ? batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1) : 0;

        if (level == -1 || scale == -1) {
            return 50;
        }

        return (int) (((float) level / (float) scale) * 100.0f);
    }

    @SuppressLint("HardwareIds")
    public static String getAndroidId() {
        return Settings.Secure.getString(AppSettings.getInstance().getContentResolver(),
                Settings.Secure.ANDROID_ID);

    }

    public static boolean isPackageInstalled(String targetPackage, Context context) {
        List<ApplicationInfo> packages;
        PackageManager pm;
        pm = context.getPackageManager();
        packages = pm.getInstalledApplications(0);
        for (ApplicationInfo packageInfo : packages) {
            if (packageInfo.packageName.equals(targetPackage)) return true;
        }
        return false;
    }

    private static NetworkInfo getActiveNetworkInfo(Context context) {
        if (context == null) {
            return null;
        }
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) {
            return null;
        }
        // note that this may return null if no network is currently active
        return cm.getActiveNetworkInfo();
    }

    public static boolean isNetworkAvailable(Context context) {
        NetworkInfo info = getActiveNetworkInfo(context);
        return (info != null && info.isConnected());
    }

    /**
     * Returns true if the device is in Doze/Idle mode. Should be called before checking the network connection because
     * the ConnectionManager may report the device is connected when it isn't during Idle mode.
     * https://github.com/yigit/android-priority-jobqueue/blob/master/jobqueue/src/main/java/com/path/android/jobqueue/network/NetworkUtilImpl.java#L60
     */
    @TargetApi(23)
    public static boolean isDozing(Context context) {
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        return powerManager.isDeviceIdleMode() &&
                !powerManager.isIgnoringBatteryOptimizations(context.getPackageName());
    }


    public static boolean locationPermissionsGranted(Context context) {
        int fineCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION);
        int coarseCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION);

        return fineCheck == PackageManager.PERMISSION_GRANTED && coarseCheck == PackageManager.PERMISSION_GRANTED;
    }

    public static void setLocale(String userSpecifiedLocale, Context baseContext, Resources resources) {

        if (!Strings.isNullOrEmpty(userSpecifiedLocale)) {
            LOG.debug("Setting language to " + userSpecifiedLocale);

            String language, country="";

            if(userSpecifiedLocale.contains("-")){
                language = userSpecifiedLocale.split("-")[0];
                country = userSpecifiedLocale.split("-")[1];
            }
            else {
                language = userSpecifiedLocale;
            }

            Locale locale = new Locale(language, country);
            Locale.setDefault(locale);
            resources.getConfiguration().locale = locale;
            baseContext.getResources().updateConfiguration(resources.getConfiguration(), baseContext.getResources().getDisplayMetrics());

        }
    }

    /**
     * Whether the user has allowed the permissions absolutely required to run the app.
     * Currently this is location and file storage.
     */
    public static boolean hasUserGrantedAllNecessaryPermissions(Context context) {
        return hasUserGrantedPermission(Manifest.permission.ACCESS_COARSE_LOCATION, context)
                && hasUserGrantedPermission(Manifest.permission.ACCESS_FINE_LOCATION, context)
                && hasUserGrantedPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, context)
                && hasUserGrantedPermission(Manifest.permission.READ_EXTERNAL_STORAGE, context);
    }

    private static boolean hasUserGrantedPermission(String permissionName, Context context) {
        boolean granted = ContextCompat.checkSelfPermission(context, permissionName) == PackageManager.PERMISSION_GRANTED;
        LOG.debug("Permission " + permissionName + " : " + granted);
        return granted;
    }

    public static void askUserForPermissions(final Activity activity, final PreferenceFragmentCompat fragment) {

        LOG.debug("User has not granted necessary permissions for this app to run.");

        Dialogs.alert(activity.getString(R.string.gnsslogger_permissions_rationale_title), activity.getString(R.string.gnsslogger_permissions_rationale_message_basic)
                        + "<br /> <a href='https://gpslogger.app/privacypolicy.html'>" + activity.getString(R.string.privacy_policy) + "</a>",
                activity, new Dialogs.MessageBoxCallback() {

                    @Override
                    public void messageBoxResult(int which) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                            if (fragment != null) {
                                //From preference fragments, requestPermissions is called differently. WHY.
                                fragment.requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
                                                Manifest.permission.ACCESS_FINE_LOCATION,
                                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                                Manifest.permission.GET_ACCOUNTS},
                                        REQUEST_PERMISSION_CODE);
                            } else {
                                ActivityCompat.requestPermissions(activity,
                                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
                                                Manifest.permission.ACCESS_FINE_LOCATION,
                                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                                Manifest.permission.GET_ACCOUNTS},
                                        REQUEST_PERMISSION_CODE);
                            }
                        }
                    }
                });
    }

    public static void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults, Context context) {
        if (requestCode == Systems.REQUEST_PERMISSION_CODE) {
            if (grantResults.length <= 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Dialogs.alert(context.getString(R.string.gnsslogger_permissions_rationale_title),
                        context.getString(R.string.gnsslogger_permissions_permanently_denied), context);
            }
        }
    }
}
