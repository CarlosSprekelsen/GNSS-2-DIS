package ets.acmi.gnssdislogger.loggers.csv;

import android.location.Location;

import androidx.annotation.Nullable;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Date;
import java.util.Locale;

import ets.acmi.gnssdislogger.common.BundleConstants;
import ets.acmi.gnssdislogger.common.Maths;
import ets.acmi.gnssdislogger.common.Session;
import ets.acmi.gnssdislogger.common.Strings;
import ets.acmi.gnssdislogger.loggers.FileLogger;
import ets.acmi.gnssdislogger.loggers.Files;


public class CSVFileLogger implements FileLogger {

    private final String name = "TXT";
    private final Integer batteryLevel;
    private final File file;

    public CSVFileLogger(File file, @Nullable Integer batteryLevel) {
        this.file = file;
        this.batteryLevel = batteryLevel;
    }

    @Override
    public void write(Location loc) throws Exception {
        if (!Session.getInstance().hasDescription()) {
            annotate("", loc);
        }
    }

    String getCsvLine(Location loc, String dateTimeString) {
        return getCsvLine("", loc, dateTimeString);
    }

    private String getCsvLine(String description, Location loc, String dateTimeString) {

        if (description.length() > 0) {
            description = "\"" + description.replaceAll("\"", "\"\"") + "\"";
        }

        return String.format(Locale.US, "%s,%s,%s,%s,%s,%s,%s,%d,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s\n", dateTimeString,
                loc.getLatitude(),
                loc.getLongitude(),
                loc.hasAltitude() ? loc.getAltitude() : "",
                loc.hasAccuracy() ? loc.getAccuracy() : "",
                loc.hasBearing() ? loc.getBearing() : "",
                loc.hasSpeed() ? loc.getSpeed() : "",
                Maths.getBundledSatelliteCount(loc),
                loc.getProvider(),
                (loc.getExtras() != null && !Strings.isNullOrEmpty(loc.getExtras().getString(BundleConstants.HDOP))) ? loc.getExtras().getString(BundleConstants.HDOP) : "",
                (loc.getExtras() != null && !Strings.isNullOrEmpty(loc.getExtras().getString(BundleConstants.VDOP))) ? loc.getExtras().getString(BundleConstants.VDOP) : "",
                (loc.getExtras() != null && !Strings.isNullOrEmpty(loc.getExtras().getString(BundleConstants.PDOP))) ? loc.getExtras().getString(BundleConstants.PDOP) : "",
                (loc.getExtras() != null && !Strings.isNullOrEmpty(loc.getExtras().getString(BundleConstants.GEOIDHEIGHT))) ? loc.getExtras().getString(BundleConstants.GEOIDHEIGHT) : "",
                (loc.getExtras() != null && !Strings.isNullOrEmpty(loc.getExtras().getString(BundleConstants.AGEOFDGPSDATA))) ? loc.getExtras().getString(BundleConstants.AGEOFDGPSDATA) : "",
                (loc.getExtras() != null && !Strings.isNullOrEmpty(loc.getExtras().getString(BundleConstants.DGPSID))) ? loc.getExtras().getString(BundleConstants.DGPSID) : "",
                (loc.getExtras() != null && !Strings.isNullOrEmpty(loc.getExtras().getString(BundleConstants.DETECTED_ACTIVITY))) ? loc.getExtras().getString(BundleConstants.DETECTED_ACTIVITY) : "",
                (batteryLevel != null) ? batteryLevel : "",
                description
        );
    }

    @Override
    public void annotate(String description, Location loc) throws Exception {
        if (!Files.reallyExists(file)) {
            file.createNewFile();

            FileOutputStream writer = new FileOutputStream(file, true);
            BufferedOutputStream output = new BufferedOutputStream(writer);
            String header = "time,lat,lon,elevation,accuracy,bearing,speed,satellites,provider,hdop,vdop,pdop,geoidheight,ageofdgpsdata,dgpsid,activity,battery,annotation\n";
            output.write(header.getBytes());
            output.flush();
            output.close();

        }

        FileOutputStream writer = new FileOutputStream(file, true);
        BufferedOutputStream output = new BufferedOutputStream(writer);

        String dateTimeString = Strings.getIsoDateTime(new Date(loc.getTime()));
        String csvLine = getCsvLine(description, loc, dateTimeString);


        output.write(csvLine.getBytes());
        output.flush();
        output.close();
        Files.addToMediaDatabase(file, "text/csv");
    }

    @Override
    public String getName() {
        return name;
    }

}
