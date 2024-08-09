package ets.acmi.gnssdislogger.senders.sftp;

import com.birbit.android.jobqueue.JobManager;
import com.birbit.android.jobqueue.TagConstraint;

import java.io.File;
import java.util.List;

import ets.acmi.gnssdislogger.common.AppSettings;
import ets.acmi.gnssdislogger.common.PreferenceHelper;
import ets.acmi.gnssdislogger.common.Strings;
import ets.acmi.gnssdislogger.senders.FileSender;

public class SFTPManager extends FileSender {

    private final PreferenceHelper preferenceHelper;

    public SFTPManager(PreferenceHelper preferenceHelper) {
        this.preferenceHelper = preferenceHelper;
    }

    @Override
    public void uploadFile(List<File> files) {
        for (File f : files) {
            uploadFile(f);
        }
    }

    public void uploadFile(final File file) {
        final JobManager jobManager = AppSettings.getJobManager();
        jobManager.cancelJobsInBackground(cancelResult -> jobManager.addJobInBackground(new SFTPJob(file, preferenceHelper.getSFTPRemoteServerPath(), preferenceHelper.getSFTPHost(), preferenceHelper.getSFTPPort(), preferenceHelper.getSFTPPrivateKeyFilePath(),
                preferenceHelper.getSFTPPrivateKeyPassphrase(), preferenceHelper.getSFTPUser(), preferenceHelper.getSFTPPassword(), preferenceHelper.getSFTPKnownHostKey())), TagConstraint.ANY, SFTPJob.getJobTag(file));
    }

    @Override
    public boolean isAvailable() {
        return validSettings(preferenceHelper.getSFTPRemoteServerPath(), preferenceHelper.getSFTPHost(), preferenceHelper.getSFTPPort(), preferenceHelper.getSFTPPrivateKeyFilePath(),
                preferenceHelper.getSFTPPrivateKeyPassphrase(), preferenceHelper.getSFTPUser(), preferenceHelper.getSFTPPassword(), preferenceHelper.getSFTPKnownHostKey());
    }

    private boolean validSettings(String sftpRemoteServerPath, String sftpHost, int sftpPort, String sftpPrivateKeyFilePath, String sftpPrivateKeyPassphrase, String sftpUser, String sftpPassword, String sftpKnownHostKey) {
        return !Strings.isNullOrEmpty(sftpRemoteServerPath)
                && !Strings.isNullOrEmpty(sftpHost)
                && sftpPort > 0
                && (!Strings.isNullOrEmpty(sftpPrivateKeyFilePath) || !Strings.isNullOrEmpty(sftpPassword));
    }

    @Override
    public boolean hasUserAllowedAutoSending() {
        return preferenceHelper.isSFTPEnabled();
    }

    @Override
    public boolean accept(File file, String s) {
        return true;
    }
}
