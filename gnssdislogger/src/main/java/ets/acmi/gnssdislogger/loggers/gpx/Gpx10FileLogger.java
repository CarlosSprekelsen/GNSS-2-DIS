package ets.acmi.gnssdislogger.loggers.gpx;

import android.location.Location;

import org.slf4j.Logger;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.util.Date;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import ets.acmi.gnssdislogger.BuildConfig;
import ets.acmi.gnssdislogger.common.BundleConstants;
import ets.acmi.gnssdislogger.common.Maths;
import ets.acmi.gnssdislogger.common.RejectionHandler;
import ets.acmi.gnssdislogger.common.Strings;
import ets.acmi.gnssdislogger.common.slf4j.Logs;
import ets.acmi.gnssdislogger.loggers.FileLogger;
import ets.acmi.gnssdislogger.loggers.Files;


public class Gpx10FileLogger implements FileLogger {
    final static Object lock = new Object();

    private final static ThreadPoolExecutor EXECUTOR = new ThreadPoolExecutor(1, 1, 60, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(10), new RejectionHandler());
    private final String name = "GPX";
    private final boolean addNewTrackSegment;
    private final File gpxFile;

    public Gpx10FileLogger(File gpxFile, boolean addNewTrackSegment) {
        this.gpxFile = gpxFile;
        this.addNewTrackSegment = addNewTrackSegment;
    }

    public void write(Location loc) throws Exception {
        long time = loc.getTime();
        if (time <= 0) {
            time = System.currentTimeMillis();
        }
        String dateTimeString = Strings.getIsoDateTime(new Date(time));

        Runnable writeHandler = getWriteHandler(dateTimeString, gpxFile, loc, addNewTrackSegment);
        EXECUTOR.execute(writeHandler);
    }

    Runnable getWriteHandler(String dateTimeString, File gpxFile, Location loc, boolean addNewTrackSegment) {
        return new Gpx10WriteHandler(dateTimeString, gpxFile, loc, addNewTrackSegment);
    }

    public void annotate(String description, Location loc) throws Exception {

        description = Strings.cleanDescriptionForXml(description);

        long time = loc.getTime();
        if (time <= 0) {
            time = System.currentTimeMillis();
        }
        String dateTimeString = Strings.getIsoDateTime(new Date(time));

        Runnable annotateHandler = getAnnotateHandler(description, gpxFile, loc, dateTimeString);
        EXECUTOR.execute(annotateHandler);
    }

    private Runnable getAnnotateHandler(String description, File gpxFile, Location loc, String dateTimeString) {
        //Use the writer to calculate initial XML length, use that as offset for annotations
        Gpx10WriteHandler writer = (Gpx10WriteHandler) getWriteHandler(dateTimeString, gpxFile, loc, true);
        return new Gpx10AnnotateHandler(description, gpxFile, loc, dateTimeString, writer.getBeginningXml(dateTimeString).length());
    }

    @Override
    public String getName() {
        return name;
    }


}

class Gpx10AnnotateHandler implements Runnable {
    private static final Logger LOG = Logs.of(Gpx10AnnotateHandler.class);
    private final String description;
    private final File gpxFile;
    private final Location loc;
    private final String dateTimeString;
    private final int annotateOffset;

    public Gpx10AnnotateHandler(String description, File gpxFile, Location loc, String dateTimeString, int annotateOffset) {
        this.description = description;
        this.gpxFile = gpxFile;
        this.loc = loc;
        this.dateTimeString = dateTimeString;
        this.annotateOffset = annotateOffset;
    }

    @Override
    public void run() {

        synchronized (Gpx10FileLogger.lock) {
            if (!Files.reallyExists(gpxFile)) {
                return;
            }

            String wpt = getWaypointXml(loc, dateTimeString, description);

            try {

                //write to a temp file, delete original file, move temp to original
                File gpxTempFile = new File(gpxFile.getAbsolutePath() + ".tmp");

                BufferedInputStream bis = new BufferedInputStream(new FileInputStream(gpxFile));
                BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(gpxTempFile));

                int written = 0;
                int readSize;
                byte[] buffer = new byte[annotateOffset];
                while ((readSize = bis.read(buffer)) > 0) {
                    bos.write(buffer, 0, readSize);
                    written += readSize;

                    System.out.println(written);

                    if (written == annotateOffset) {
                        bos.write(wpt.getBytes());
                        buffer = new byte[20480];
                    }

                }

                bis.close();
                bos.close();

                gpxFile.delete();
                gpxTempFile.renameTo(gpxFile);

                LOG.debug("Finished annotation to GPX10 File");
            } catch (Exception e) {
                LOG.error("Gpx10FileLogger.annotate", e);
            }

        }
    }

    private String getWaypointXml(Location loc, String dateTimeString, String description) {

        StringBuilder waypoint = new StringBuilder();

        waypoint.append("\n<wpt lat=\"")
                .append(loc.getLatitude())
                .append("\" lon=\"")
                .append(loc.getLongitude())
                .append("\">");

        if (loc.hasAltitude()) {
            waypoint.append("<ele>").append(loc.getAltitude()).append("</ele>");
        }

        waypoint.append("<time>").append(dateTimeString).append("</time>");
        waypoint.append("<name>").append(description).append("</name>");

        waypoint.append("<src>").append(loc.getProvider()).append("</src>");
        waypoint.append("</wpt>\n");

        return waypoint.toString();
    }
}


class Gpx10WriteHandler implements Runnable {
    private static final Logger LOG = Logs.of(Gpx10WriteHandler.class);
    private final String dateTimeString;
    private final Location loc;
    private final File gpxFile;
    private boolean addNewTrackSegment;

    public Gpx10WriteHandler(String dateTimeString, File gpxFile, Location loc, boolean addNewTrackSegment) {
        this.dateTimeString = dateTimeString;
        this.addNewTrackSegment = addNewTrackSegment;
        this.gpxFile = gpxFile;
        this.loc = loc;
    }

    @Override
    public void run() {
        synchronized (Gpx10FileLogger.lock) {

            try {
                if (!Files.reallyExists(gpxFile)) {
                    gpxFile.createNewFile();

                    FileOutputStream initialWriter = new FileOutputStream(gpxFile, true);
                    BufferedOutputStream initialOutput = new BufferedOutputStream(initialWriter);

                    initialOutput.write(getBeginningXml(dateTimeString).getBytes());
                    initialOutput.write("<trk>".getBytes());
                    initialOutput.write(getEndXml().getBytes());
                    initialOutput.flush();
                    initialOutput.close();

                    //New file, so new segment.
                    addNewTrackSegment = true;
                }

                int offsetFromEnd = (addNewTrackSegment) ? getEndXml().length() : getEndXmlWithSegment().length();
                long startPosition = gpxFile.length() - offsetFromEnd;
                String trackPoint = getTrackPointXml(loc, dateTimeString);

                RandomAccessFile raf = new RandomAccessFile(gpxFile, "rw");
                raf.seek(startPosition);
                raf.write(trackPoint.getBytes());
                raf.close();
                Files.addToMediaDatabase(gpxFile, "text/plain");
                LOG.debug("Finished writing to GPX10 file");

            } catch (Exception e) {
                LOG.error("Gpx10FileLogger.write", e);
            }

        }

    }

    String getBeginningXml(String dateTimeString) {
        StringBuilder initialXml = new StringBuilder();
        initialXml.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>");
        initialXml.append("<gpx version=\"1.0\" creator=\"GNSSLogger " + BuildConfig.VERSION_CODE);
        initialXml.append("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" ");
        initialXml.append("xmlns=\"http://www.topografix.com/GPX/1/0\" ");
        initialXml.append("xsi:schemaLocation=\"http://www.topografix.com/GPX/1/0 ");
        initialXml.append("http://www.topografix.com/GPX/1/0/gpx.xsd\">");
        initialXml.append("<time>").append(dateTimeString).append("</time>");
        return initialXml.toString();
    }

    private String getEndXml() {
        return "</trk></gpx>";
    }

    private String getEndXmlWithSegment() {
        return "</trkseg></trk></gpx>";
    }

    private String getTrackPointXml(Location loc, String dateTimeString) {

        StringBuilder track = new StringBuilder();

        if (addNewTrackSegment) {
            track.append("<trkseg>");
        }

        track.append("<trkpt lat=\"")
                .append(loc.getLatitude())
                .append("\" lon=\"")
                .append(loc.getLongitude())
                .append("\">");

        if (loc.hasAltitude()) {
            track.append("<ele>").append(loc.getAltitude()).append("</ele>");
        }

        track.append("<time>").append(dateTimeString).append("</time>");

        appendCourseAndSpeed(track, loc);

        if (loc.getExtras() != null) {
            String geoidheight = loc.getExtras().getString(BundleConstants.GEOIDHEIGHT);

            if (!Strings.isNullOrEmpty(geoidheight)) {
                track.append("<geoidheight>").append(geoidheight).append("</geoidheight>");
            }
        }

        track.append("<src>").append(loc.getProvider()).append("</src>");

        if (loc.getExtras() != null) {

            int sat = Maths.getBundledSatelliteCount(loc);

            if (sat > 0) {
                track.append("<sat>").append(sat).append("</sat>");
            }


            String hdop = loc.getExtras().getString(BundleConstants.HDOP);
            String pdop = loc.getExtras().getString(BundleConstants.PDOP);
            String vdop = loc.getExtras().getString(BundleConstants.VDOP);
            String ageofdgpsdata = loc.getExtras().getString(BundleConstants.AGEOFDGPSDATA);
            String dgpsid = loc.getExtras().getString(BundleConstants.DGPSID);

            if (!Strings.isNullOrEmpty(hdop)) {
                track.append("<hdop>").append(hdop).append("</hdop>");
            }

            if (!Strings.isNullOrEmpty(vdop)) {
                track.append("<vdop>").append(vdop).append("</vdop>");
            }

            if (!Strings.isNullOrEmpty(pdop)) {
                track.append("<pdop>").append(pdop).append("</pdop>");
            }

            if (!Strings.isNullOrEmpty(ageofdgpsdata)) {
                track.append("<ageofdgpsdata>").append(ageofdgpsdata).append("</ageofdgpsdata>");
            }

            if (!Strings.isNullOrEmpty(dgpsid)) {
                track.append("<dgpsid>").append(dgpsid).append("</dgpsid>");
            }
        }


        track.append("</trkpt>\n");

        track.append("</trkseg></trk></gpx>");

        return track.toString();
    }

    void appendCourseAndSpeed(StringBuilder track, Location loc) {
        if (loc.hasBearing()) {
            track.append("<course>").append(loc.getBearing()).append("</course>");
        }

        if (loc.hasSpeed()) {
            track.append("<speed>").append(loc.getSpeed()).append("</speed>");
        }
    }
}