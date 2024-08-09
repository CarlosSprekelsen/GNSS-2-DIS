package ets.acmi.gnssdislogger.common;


import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.os.PowerManager;

import com.birbit.android.jobqueue.network.NetworkEventProvider;
import com.birbit.android.jobqueue.network.NetworkUtil;

/**
 * default implementation for network Utility to observe network events
 */
class WifiNetworkUtil implements NetworkUtil, NetworkEventProvider {
    private Listener listener;

    public WifiNetworkUtil(Context context) {
        context = context.getApplicationContext();
        listenForIdle(context);
        listenNetworkViaConnectivityManager(context);
    }

    @TargetApi(23)
    private static IntentFilter getNetworkIntentFilter() {
        IntentFilter networkIntentFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        networkIntentFilter.addAction(PowerManager.ACTION_DEVICE_IDLE_MODE_CHANGED);
        return networkIntentFilter;
    }

    @TargetApi(23)
    private void listenNetworkViaConnectivityManager(final Context context) {
        ConnectivityManager cm = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkRequest request = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)
                .build();
        cm.registerNetworkCallback(request, new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                dispatchNetworkChange(context);
            }
        });
    }

    @TargetApi(23)
    private void listenForIdle(Context context) {
        context.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                dispatchNetworkChange(context);
            }
        }, new IntentFilter(PowerManager.ACTION_DEVICE_IDLE_MODE_CHANGED));
    }

    private void dispatchNetworkChange(Context context) {
        if (listener == null) {//shall not be but just be safe
            return;
        }
        //http://developer.android.com/reference/android/net/ConnectivityManager.html#EXTRA_NETWORK_INFO
        //Since NetworkInfo can vary based on UID, applications should always obtain network information
        // through getActiveNetworkInfo() or getAllNetworkInfo().
        listener.onNetworkChange(getNetworkStatus(context));
    }

    @Override
    public int getNetworkStatus(Context context) {
        if (Systems.isDozing(context)) {
            return NetworkUtil.DISCONNECTED;
        }
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo == null) {
            return NetworkUtil.DISCONNECTED;
        }

        boolean isWifiRequired = PreferenceHelper.getInstance().shouldAutoSendOnWifiOnly();
        boolean isDeviceOnWifi = true;

        if (isWifiRequired) {
            isDeviceOnWifi = (netInfo.getType() == ConnectivityManager.TYPE_WIFI);
        }

        if (netInfo.isConnected() && isDeviceOnWifi) {
            return NetworkUtil.UNMETERED;
        }

        return NetworkUtil.METERED;
    }

    @Override
    public void setListener(Listener listener) {
        this.listener = listener;
    }
}