package ets.acmi.gnssdislogger.loggers.eag;

import android.location.Location;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Date;
import java.util.Locale;

import edu.nps.moves.dis.Orientation;
import edu.nps.moves.dis.Vector3Double;
import ets.acmi.gnssdislogger.common.PreferenceHelper;
import ets.acmi.gnssdislogger.common.Session;
import ets.acmi.gnssdislogger.common.Strings;
import ets.acmi.gnssdislogger.loggers.FileLogger;
import ets.acmi.gnssdislogger.loggers.Files;

public class EagFileLogger implements FileLogger {

    private final String name = "EAG";
    private final File file;
    private Session session = Session.getInstance();
    private static final PreferenceHelper preferenceHelper = PreferenceHelper.getInstance();

    public EagFileLogger(File file) {
        this.file = file;
    }

    @Override
    public void write(Location loc) throws Exception {
        if (!Session.getInstance().hasDescription()) {
            annotate("", loc);
        }
    }

    private String getEagLine(String dateTimeString) {
        Vector3Double EcefLocation = session.getPreviousEntityStatePdu().getEntityLocation();
        Orientation orientation = session.getPreviousEntityStatePdu().getEntityOrientation();


        // EAG file format
        // Header: playerId ?   ?   ACTYPE
        // Rows: GPSseconds,id, ?,?,ecefx,ecefy,ecefz,phsi,theta,rho, timestring
        return String.format(Locale.US, "%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n",
                Math.round(session.getPreviousSeconds()),
                preferenceHelper.getDISEntityid(),
                "1",
                "1",
                Math.round(EcefLocation.getX()),
                Math.round(EcefLocation.getY()),
                Math.round(EcefLocation.getZ()),
                orientation.getTheta(),
                orientation.getPhi(),
                orientation.getPsi(),
                dateTimeString
        );
    }

    @Override
    public void annotate(String description, Location loc) throws Exception {
        if (!Files.reallyExists(file)) {
            file.createNewFile();

            FileOutputStream writer = new FileOutputStream(file, true);
            BufferedOutputStream output = new BufferedOutputStream(writer);
            String header = preferenceHelper.getDISEntityid() + "\t1\t1\n#GpsSeconds,id,1,1,ecefx,ecefy,ecefz,theta,phi, psi, timestring\n";
            output.write(header.getBytes());
            output.flush();
            output.close();

        } else {

            FileOutputStream writer = new FileOutputStream(file, true);
            BufferedOutputStream output = new BufferedOutputStream(writer);

            String dateTimeString = Strings.getIsoDateTime(new Date(loc.getTime()));
            String eagLine = getEagLine(dateTimeString);

            output.write(eagLine.getBytes());
            output.flush();
            output.close();
        }

        Files.addToMediaDatabase(file, "text/csv");
    }

    @Override
    public String getName() {
        return name;
    }

}
