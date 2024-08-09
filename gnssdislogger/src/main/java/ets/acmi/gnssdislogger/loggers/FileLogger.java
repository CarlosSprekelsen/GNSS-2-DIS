package ets.acmi.gnssdislogger.loggers;

import android.location.Location;

public interface FileLogger {

    void write(Location loc) throws Exception;

    void annotate(String description, Location loc) throws Exception;

    String getName();

}
