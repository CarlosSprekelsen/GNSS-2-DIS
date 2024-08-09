package ets.acmi.gnssdislogger.loggers.customurl;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.birbit.android.jobqueue.Job;
import com.birbit.android.jobqueue.Params;
import com.birbit.android.jobqueue.RetryConstraint;

import org.slf4j.Logger;

import java.util.Map;

import de.greenrobot.event.EventBus;
import ets.acmi.gnssdislogger.common.AppSettings;
import ets.acmi.gnssdislogger.common.events.UploadEvents;
import ets.acmi.gnssdislogger.common.network.Networks;
import ets.acmi.gnssdislogger.common.slf4j.Logs;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


class CustomUrlJob extends Job {

    private static final Logger LOG = Logs.of(CustomUrlJob.class);

    private final UploadEvents.BaseUploadEvent callbackEvent;
    private final CustomUrlRequest urlRequest;

    public CustomUrlJob(CustomUrlRequest urlRequest, UploadEvents.BaseUploadEvent callbackEvent) {
        super(new Params(1).requireNetwork().persist());

        this.callbackEvent = callbackEvent;
        this.urlRequest = urlRequest;
    }

    @Override
    public void onAdded() {
    }

    @Override
    public void onRun() throws Throwable {

        LOG.info("HTTP Request - " + urlRequest.getLogURL());

        OkHttpClient.Builder okBuilder = new OkHttpClient.Builder();
        okBuilder.sslSocketFactory(Networks.getSocketFactory(AppSettings.getInstance()));
        Request.Builder requestBuilder = new Request.Builder().url(urlRequest.getLogURL());

        for (Map.Entry<String, String> header : urlRequest.getHttpHeaders().entrySet()) {
            requestBuilder.addHeader(header.getKey(), header.getValue());
        }

        if (!urlRequest.getHttpMethod().equalsIgnoreCase("GET")) {
            RequestBody body = RequestBody.create(urlRequest.getHttpBody(), null);
            requestBuilder = requestBuilder.method(urlRequest.getHttpMethod(), body);
        }

        Request request = requestBuilder.build();
        Response response = okBuilder.build().newCall(request).execute();

        if (response.isSuccessful()) {
            LOG.debug("HTTP request complete with successful response code " + response);
            EventBus.getDefault().post(callbackEvent.succeeded());
        } else {
            LOG.error("HTTP request complete with unexpected response code " + response);
            EventBus.getDefault().post(callbackEvent.failed("Unexpected code " + response, new Throwable(response.body().string())));
        }

        response.body().close();
    }

    @Override
    protected void onCancel(int cancelReason, @Nullable Throwable throwable) {
        EventBus.getDefault().post(callbackEvent.failed("Could not send to custom URL", throwable));
        LOG.error("Custom URL: maximum attempts failed, giving up", throwable);
    }

    @Override
    protected RetryConstraint shouldReRunOnThrowable(@NonNull Throwable throwable, int runCount, int maxRunCount) {
        LOG.warn(String.format("Custom URL: attempt %d failed, maximum %d attempts", runCount, maxRunCount));
        return RetryConstraint.createExponentialBackoff(runCount, 5000);
    }


    @Override
    protected int getRetryLimit() {
        return 5;
    }
}
