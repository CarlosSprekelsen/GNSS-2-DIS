package ets.acmi.gnssdislogger.senders;

import org.slf4j.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ets.acmi.gnssdislogger.common.PreferenceHelper;
import ets.acmi.gnssdislogger.common.slf4j.Logs;
import ets.acmi.gnssdislogger.loggers.Files;
import ets.acmi.gnssdislogger.senders.ftp.FtpManager;
import ets.acmi.gnssdislogger.senders.sftp.SFTPManager;

public class FileSenderFactory {

    private static final Logger LOG = Logs.of(FileSenderFactory.class);


    public static FileSender getFtpSender() {
        return new FtpManager(PreferenceHelper.getInstance());
    }


    public static FileSender getSFTPSender() {
        return new SFTPManager(PreferenceHelper.getInstance());
    }

    public static void autoSendFiles(final String fileToSend) {

        PreferenceHelper preferenceHelper = PreferenceHelper.getInstance();
        LOG.info("Auto-sending file " + fileToSend);

        File gpxFolder = new File(preferenceHelper.getGpsLoggerFolder());

        if (Files.fromFolder(gpxFolder).length < 1) {
            LOG.warn("No files found to send.");
            return;
        }

        List<File> files = new ArrayList<>(Arrays.asList(Files.fromFolder(gpxFolder, (file, s) -> s.contains(fileToSend) && !s.contains("zip"))));

        List<File> zipFiles = new ArrayList<>();

        if (files.size() == 0) {
            LOG.warn("No files found to send after filtering.");
            return;
        }

        List<FileSender> senders = getFileAutosenders();

        if (!senders.isEmpty() && preferenceHelper.shouldSendZipFile()) {
            File zipFile = new File(gpxFolder.getPath(), fileToSend + ".zip");
            ArrayList<String> filePaths = new ArrayList<>();

            for (File f : files) {
                filePaths.add(f.getAbsolutePath());
            }

            LOG.info("Zipping file");
            ZipHelper zh = new ZipHelper(filePaths.toArray(new String[0]), zipFile.getAbsolutePath());
            zh.zipFiles();

            zipFiles.clear();
            zipFiles.add(zipFile);
        }


        for (FileSender sender : senders) {
            LOG.debug("Sender: " + sender.getClass().getName());
            //Special case for OSM Uploader
            if (!sender.accept(null, ".zip")) {
                sender.uploadFile(files);
                continue;
            }

            if (preferenceHelper.shouldSendZipFile()) {
                sender.uploadFile(zipFiles);
            } else {
                sender.uploadFile(files);
            }

        }
    }


    private static List<FileSender> getFileAutosenders() {

        List<FileSender> senders = new ArrayList<>();


        if (getFtpSender().isAutoSendAvailable()) {
            senders.add(getFtpSender());
        }

        if (getSFTPSender().isAutoSendAvailable()) {
            senders.add(getSFTPSender());
        }

        return senders;

    }
}
