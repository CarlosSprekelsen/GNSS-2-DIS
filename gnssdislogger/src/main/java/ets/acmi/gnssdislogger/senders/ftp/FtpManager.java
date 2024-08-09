package ets.acmi.gnssdislogger.senders.ftp;


import com.birbit.android.jobqueue.JobManager;
import com.birbit.android.jobqueue.TagConstraint;

import org.slf4j.Logger;

import java.io.File;
import java.util.List;

import de.greenrobot.event.EventBus;
import ets.acmi.gnssdislogger.common.AppSettings;
import ets.acmi.gnssdislogger.common.PreferenceHelper;
import ets.acmi.gnssdislogger.common.events.UploadEvents;
import ets.acmi.gnssdislogger.common.slf4j.Logs;
import ets.acmi.gnssdislogger.loggers.Files;
import ets.acmi.gnssdislogger.senders.FileSender;

public class FtpManager extends FileSender {
    private static final Logger LOG = Logs.of(FtpManager.class);

    private final PreferenceHelper preferenceHelper;

    public FtpManager(PreferenceHelper preferenceHelper) {
        this.preferenceHelper = preferenceHelper;
    }

    public void testFtp(final String servername, final String username, final String password, final String directory, final int port, final boolean useFtps, final String protocol, final boolean implicit) {


        try {
            final File testFile = Files.createTestFile();

            final JobManager jobManager = AppSettings.getJobManager();

            jobManager.cancelJobsInBackground(cancelResult -> jobManager.addJobInBackground(new FtpJob(servername, port, username, password, directory,
                    useFtps, protocol, implicit, testFile, testFile.getName())), TagConstraint.ANY, FtpJob.getJobTag(testFile));

        } catch (Exception ex) {
            EventBus.getDefault().post(new UploadEvents.Ftp().failed(ex.getMessage(), ex));
        }


    }

    @Override
    public void uploadFile(List<File> files) {
        if (!validSettings(preferenceHelper.getFtpServerName(), preferenceHelper.getFtpUsername(), preferenceHelper.getFtpPassword(),
                preferenceHelper.getFtpPort(), preferenceHelper.shouldFtpUseFtps(), preferenceHelper.getFtpProtocol(), preferenceHelper.isFtpImplicit())) {
            EventBus.getDefault().post(new UploadEvents.Ftp().failed());
        }

        for (File f : files) {
            uploadFile(f);
        }
    }

    @Override
    public boolean isAvailable() {
        return validSettings(preferenceHelper.getFtpServerName(), preferenceHelper.getFtpUsername(),
                preferenceHelper.getFtpPassword(), preferenceHelper.getFtpPort(), preferenceHelper.shouldFtpUseFtps(),
                preferenceHelper.getFtpProtocol(), preferenceHelper.isFtpImplicit());
    }

    @Override
    public boolean hasUserAllowedAutoSending() {
        return preferenceHelper.isFtpAutoSendEnabled();
    }

    private void uploadFile(final File f) {

        final JobManager jobManager = AppSettings.getJobManager();
        jobManager.cancelJobsInBackground(cancelResult -> jobManager.addJobInBackground(new FtpJob(preferenceHelper.getFtpServerName(), preferenceHelper.getFtpPort(),
                preferenceHelper.getFtpUsername(), preferenceHelper.getFtpPassword(), preferenceHelper.getFtpDirectory(),
                preferenceHelper.shouldFtpUseFtps(), preferenceHelper.getFtpProtocol(), preferenceHelper.isFtpImplicit(),
                f, f.getName())), TagConstraint.ANY, FtpJob.getJobTag(f));

    }

    @Override
    public boolean accept(File file, String s) {
        return true;
    }


    public boolean validSettings(String servername, String username, String password, Integer port, boolean useFtps,
                                 String sslTls, boolean implicit) {
        boolean retVal = servername != null && servername.length() > 0 && port != null && port > 0;

        if (useFtps && (sslTls == null || sslTls.length() <= 0)) {
            retVal = false;
        }

        return retVal;
    }
}

