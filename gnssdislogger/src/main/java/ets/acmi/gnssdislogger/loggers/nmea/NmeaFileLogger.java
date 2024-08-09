package ets.acmi.gnssdislogger.loggers.nmea;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import ets.acmi.gnssdislogger.common.PreferenceHelper;
import ets.acmi.gnssdislogger.common.RejectionHandler;
import ets.acmi.gnssdislogger.common.Session;
import ets.acmi.gnssdislogger.common.Strings;
import ets.acmi.gnssdislogger.loggers.Files;

public class NmeaFileLogger {

    final static Object lock = new Object();
    private final static ThreadPoolExecutor EXECUTOR = new ThreadPoolExecutor(1, 1, 60, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(10), new RejectionHandler());
    private final String fileName;
    private final PreferenceHelper preferenceHelper = PreferenceHelper.getInstance();
    private Session session = Session.getInstance();

    public NmeaFileLogger(String fileName) {
        this.fileName = fileName;
    }

    public void write(long timestamp, String nmeaSentence) {

        File gpxFolder = new File(preferenceHelper.getGpsLoggerFolder());
        if (!gpxFolder.exists()) {
            gpxFolder.mkdirs();
        }

        File nmeaFile = new File(gpxFolder.getPath(), Strings.getFormattedFileName() + ".nmea");

        if (!nmeaFile.exists()) {
            try {
                nmeaFile.createNewFile();
            } catch (IOException e) {

            }
        }

        NmeaWriteHandler writeHandler = new NmeaWriteHandler(nmeaFile, nmeaSentence);
        EXECUTOR.execute(writeHandler);
    }
}

class NmeaWriteHandler implements Runnable {

    private final File gpxFile;
    private final String nmeaSentence;

    NmeaWriteHandler(File gpxFile, String nmeaSentence) {
        this.gpxFile = gpxFile;
        this.nmeaSentence = nmeaSentence;
    }

    @Override
    public void run() {

        synchronized (NmeaFileLogger.lock) {

            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter(gpxFile, true));
                writer.write(nmeaSentence);
                writer.newLine();
                writer.close();
                Files.addToMediaDatabase(gpxFile, "text/plain");

            } catch (IOException e) {

            }
        }

    }
}
