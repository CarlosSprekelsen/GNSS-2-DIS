package ets.acmi.gnssdislogger.loggers;

import android.content.Context;
import android.location.Location;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import ets.acmi.gnssdislogger.common.PreferenceHelper;
import ets.acmi.gnssdislogger.common.Session;
import ets.acmi.gnssdislogger.common.Strings;
import ets.acmi.gnssdislogger.common.Systems;
import ets.acmi.gnssdislogger.loggers.csv.CSVFileLogger;
import ets.acmi.gnssdislogger.loggers.customurl.CustomUrlLogger;
import ets.acmi.gnssdislogger.loggers.eag.EagFileLogger;
import ets.acmi.gnssdislogger.loggers.geojson.GeoJSONLogger;
import ets.acmi.gnssdislogger.loggers.gpx.Gpx10FileLogger;
import ets.acmi.gnssdislogger.loggers.gpx.Gpx11FileLogger;
import ets.acmi.gnssdislogger.loggers.ieee1278.DisLogger;
import ets.acmi.gnssdislogger.loggers.kml.Kml22FileLogger;

public class FileLoggerFactory {

    private static final PreferenceHelper preferenceHelper = PreferenceHelper.getInstance();
    private static final Session session = Session.getInstance();

    public static List<FileLogger> getFileLoggers(Context context) {

        List<FileLogger> loggers = new ArrayList<>();

        if (Strings.isNullOrEmpty(preferenceHelper.getGpsLoggerFolder())) {
            return loggers;
        }

        File gnssLoggerFolder = new File(preferenceHelper.getGpsLoggerFolder());
        if (!gnssLoggerFolder.exists()) {
            gnssLoggerFolder.mkdirs();
        }

        int batteryLevel = Systems.getBatteryLevel(context);

        if (preferenceHelper.shouldLogToGpx()) {
            File gpxFile = new File(gnssLoggerFolder.getPath(), Strings.getFormattedFileName() + ".gpx");
            if (preferenceHelper.shouldLogAsGpx11()) {
                loggers.add(new Gpx11FileLogger(gpxFile, session.shouldAddNewTrackSegment()));
            } else {
                loggers.add(new Gpx10FileLogger(gpxFile, session.shouldAddNewTrackSegment()));
            }
        }

        if (preferenceHelper.shouldLogToKml()) {
            File kmlFile = new File(gnssLoggerFolder.getPath(), Strings.getFormattedFileName() + ".kml");
            loggers.add(new Kml22FileLogger(kmlFile, session.shouldAddNewTrackSegment()));
        }

        if (preferenceHelper.shouldLogToCSV()) {
            File file = new File(gnssLoggerFolder.getPath(), Strings.getFormattedFileName() + ".csv");
            loggers.add(new CSVFileLogger(file, batteryLevel));
        }

        if (preferenceHelper.shouldLogToEag()) {
            File file = new File(gnssLoggerFolder.getPath(), Strings.getFormattedFileName() + ".eag");
            loggers.add(new EagFileLogger(file));
        }

        if (preferenceHelper.shouldLogToCustomUrl()) {

            loggers.add(new CustomUrlLogger(preferenceHelper.getCustomLoggingUrl(),
                    batteryLevel,
                    preferenceHelper.getCustomLoggingHTTPMethod(),
                    preferenceHelper.getCustomLoggingHTTPBody(),
                    preferenceHelper.getCustomLoggingHTTPHeaders(),
                    preferenceHelper.getCustomLoggingBasicAuthUsername(),
                    preferenceHelper.getCustomLoggingBasicAuthPassword()));
        }

        if (preferenceHelper.shouldLogToDIS()) {
            loggers.add(new DisLogger());
        }

        if (preferenceHelper.shouldLogToGeoJSON()) {
            File file = new File(gnssLoggerFolder.getPath(), Strings.getFormattedFileName() + ".geojson");
            loggers.add(new GeoJSONLogger(file, session.shouldAddNewTrackSegment()));
        }


        return loggers;
    }

    public static void write(Context context, Location loc) throws Exception {
        for (FileLogger logger : getFileLoggers(context)) {
            logger.write(loc);
        }
    }

    public static void annotate(Context context, String description, Location loc) throws Exception {
        for (FileLogger logger : getFileLoggers(context)) {
            logger.annotate(description, loc);
        }
    }
}
