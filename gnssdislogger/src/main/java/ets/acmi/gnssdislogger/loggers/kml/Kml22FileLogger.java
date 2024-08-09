package ets.acmi.gnssdislogger.loggers.kml;

import android.location.Location;

import org.slf4j.Logger;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.Date;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import ets.acmi.gnssdislogger.common.RejectionHandler;
import ets.acmi.gnssdislogger.common.Strings;
import ets.acmi.gnssdislogger.common.slf4j.Logs;
import ets.acmi.gnssdislogger.loggers.FileLogger;
import ets.acmi.gnssdislogger.loggers.Files;

public class Kml22FileLogger implements FileLogger {
    final static Object lock = new Object();
    private final static ThreadPoolExecutor EXECUTOR = new ThreadPoolExecutor(1, 1, 60, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(10), new RejectionHandler());
    private final String name = "KML";
    private final boolean addNewTrackSegment;
    private final File kmlFile;


    public Kml22FileLogger(File kmlFile, boolean addNewTrackSegment) {
        this.kmlFile = kmlFile;
        this.addNewTrackSegment = addNewTrackSegment;
    }


    public void write(Location loc) throws Exception {
        Kml22WriteHandler writeHandler = new Kml22WriteHandler(loc, kmlFile, addNewTrackSegment);
        EXECUTOR.execute(writeHandler);
    }

    public void annotate(String description, Location loc) throws Exception {

        description = Strings.cleanDescriptionForXml(description);

        Kml22AnnotateHandler annotateHandler = new Kml22AnnotateHandler(kmlFile, description, loc);
        EXECUTOR.execute(annotateHandler);
    }

    @Override
    public String getName() {
        return name;
    }
}

class Kml22AnnotateHandler implements Runnable {
    private static final Logger LOG = Logs.of(Kml22AnnotateHandler.class);
    private final File kmlFile;
    private final String description;
    private final Location loc;
    private final int kmlAnnotationOffset = 258;

    public Kml22AnnotateHandler(File kmlFile, String description, Location loc) {
        this.kmlFile = kmlFile;
        this.description = description;
        this.loc = loc;
    }


    @Override
    public void run() {
        if (!Files.reallyExists(kmlFile)) {
            return;
        }

        try {
            synchronized (Kml22FileLogger.lock) {

                String descriptionNode = getPlacemarkXml(description, loc);


                RandomAccessFile r = new RandomAccessFile(kmlFile, "rw");
                File tmpFile = new File(kmlFile.getAbsolutePath() + "~");
                tmpFile.createNewFile();
                RandomAccessFile rtemp = new RandomAccessFile(tmpFile, "rw");
                long fileSize = r.length();
                FileChannel sourceChannel = r.getChannel();
                FileChannel targetChannel = rtemp.getChannel();
                sourceChannel.transferTo(kmlAnnotationOffset, (fileSize - kmlAnnotationOffset), targetChannel);
                sourceChannel.truncate(kmlAnnotationOffset);
                r.seek(kmlAnnotationOffset);
                r.write(descriptionNode.getBytes());
                long newOffset = r.getFilePointer();
                targetChannel.position(0L);
                sourceChannel.transferFrom(targetChannel, newOffset, (fileSize - kmlAnnotationOffset));
                sourceChannel.close();
                targetChannel.close();
                tmpFile.delete();


            }
        } catch (Exception e) {
            LOG.error("Kml22FileLogger.annotate", e);
        }
    }

    private String getPlacemarkXml(String description, Location loc) {
        StringBuilder descriptionNode = new StringBuilder();
        descriptionNode.append("\n<Placemark><name>");
        descriptionNode.append(description);
        descriptionNode.append("</name><Point><coordinates>");
        descriptionNode.append(loc.getLongitude());
        descriptionNode.append(",");
        descriptionNode.append(loc.getLatitude());
        descriptionNode.append(",");
        descriptionNode.append(loc.getAltitude());
        descriptionNode.append("</coordinates></Point></Placemark>\n");

        return descriptionNode.toString();
    }
}

class Kml22WriteHandler implements Runnable {

    private static final Logger LOG = Logs.of(Kml22WriteHandler.class);
    private boolean addNewTrackSegment;
    private final File kmlFile;
    private final Location loc;


    public Kml22WriteHandler(Location loc, File kmlFile, boolean addNewTrackSegment) {

        this.loc = loc;
        this.kmlFile = kmlFile;
        this.addNewTrackSegment = addNewTrackSegment;
    }


    @Override
    public void run() {
        try {

            RandomAccessFile raf;

            String dateTimeString = Strings.getIsoDateTime(new Date(loc.getTime()));
            String placemarkHead = "<Placemark>\n<gx:Track>\n";
            String placemarkTail = "</gx:Track>\n</Placemark></Document></kml>\n";

            synchronized (Kml22FileLogger.lock) {

                if (!Files.reallyExists(kmlFile)) {
                    kmlFile.createNewFile();

                    FileOutputStream initialWriter = new FileOutputStream(kmlFile, true);
                    BufferedOutputStream initialOutput = new BufferedOutputStream(initialWriter);

                    StringBuilder initialXml = new StringBuilder();
                    initialXml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
                    initialXml.append("<kml xmlns=\"http://www.opengis.net/kml/2.2\" ");
                    initialXml.append("xmlns:gx=\"http://www.google.com/kml/ext/2.2\" ");
                    initialXml.append("xmlns:kml=\"http://www.opengis.net/kml/2.2\" ");
                    initialXml.append("xmlns:atom=\"http://www.w3.org/2005/Atom\">");
                    initialXml.append("<Document>");
                    initialXml.append("<name>").append(dateTimeString).append("</name>\n");

                    initialXml.append("</Document></kml>\n");
                    initialOutput.write(initialXml.toString().getBytes());
                    initialOutput.flush();
                    initialOutput.close();

                    //New file, so new track segment
                    addNewTrackSegment = true;
                }


                if (addNewTrackSegment) {
                    raf = new RandomAccessFile(kmlFile, "rw");
                    raf.seek(kmlFile.length() - 18);
                    raf.write((placemarkHead + placemarkTail).getBytes());
                    raf.close();

                }

                StringBuilder coords = new StringBuilder();
                coords.append("\n<when>");
                coords.append(dateTimeString);
                coords.append("</when>\n<gx:coord>");
                coords.append(loc.getLongitude());
                coords.append(" ");
                coords.append(loc.getLatitude());
                coords.append(" ");
                coords.append(loc.getAltitude());
                coords.append("</gx:coord>\n");
                coords.append(placemarkTail);

                raf = new RandomAccessFile(kmlFile, "rw");
                raf.seek(kmlFile.length() - 42);
                raf.write(coords.toString().getBytes());
                raf.close();
                Files.addToMediaDatabase(kmlFile, "text/xml");
                LOG.debug("Finished writing to KML22 File");
            }

        } catch (Exception e) {
            LOG.error("Kml22FileLogger.write", e);
        }
    }
}
