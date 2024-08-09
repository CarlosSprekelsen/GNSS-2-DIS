package ets.acmi.gnssdislogger.common;

import android.app.Application;

import com.birbit.android.jobqueue.JobManager;
import com.birbit.android.jobqueue.config.Configuration;
import com.birbit.android.jobqueue.log.CustomLogger;

import org.slf4j.Logger;

import de.greenrobot.event.EventBus;
import ets.acmi.gnssdislogger.BuildConfig;
import ets.acmi.gnssdislogger.common.slf4j.Logs;

public class AppSettings extends Application {

    private static JobManager jobManager;
    private static AppSettings instance;
    private static Logger LOG;
    private final CustomLogger jobQueueLogger = new CustomLogger() {
        @Override
        public boolean isDebugEnabled() {
            return BuildConfig.DEBUG;
        }

        @Override
        public void d(String text, Object... args) {

            LOG.debug(String.format(text, args));
        }

        @Override
        public void e(Throwable t, String text, Object... args) {
            LOG.error(String.format(text, args), t);
        }

        @Override
        public void e(String text, Object... args) {

            LOG.error(String.format(text, args));
        }

        @Override
        public void v(String text, Object... args) {
            LOG.debug(String.format(text, args));
        }
    };

    public AppSettings() {
        instance = this;
    }

    /**
     * Returns a configured Job Queue Manager
     */
    public static JobManager getJobManager() {
        return jobManager;
    }

    /**
     * Returns a singleton instance of this class
     */
    public static AppSettings getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        //Configure the slf4j logger
        Logs.configure();
        LOG = Logs.of(this.getClass());
        LOG.debug("Log4J configured");

        //Configure the Event Bus
        EventBus.builder().logNoSubscriberMessages(false).sendNoSubscriberEvent(false).installDefaultEventBus();
        LOG.debug("EventBus configured");

        //Configure the Job Queue
        Configuration config = new Configuration.Builder(getInstance())
                .networkUtil(new WifiNetworkUtil(getInstance()))
                .consumerKeepAlive(60)
                .minConsumerCount(0)
                .maxConsumerCount(1)
                .customLogger(jobQueueLogger)
                .build();
        jobManager = new JobManager(config);
        LOG.debug("Job Queue configured");
    }


}
