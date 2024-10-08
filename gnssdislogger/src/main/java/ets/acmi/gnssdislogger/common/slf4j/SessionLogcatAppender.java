package ets.acmi.gnssdislogger.common.slf4j;


import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;


public class SessionLogcatAppender extends AppenderBase<ILoggingEvent> {

    /**
     * Marker to indicate that this logger entry is special
     */
    public static final Marker MARKER_LOCATION = MarkerFactory.getMarker("LOCATION");
    /**
     * Marker to indicate that this logger entry is for debug log files only
     */
    public static final Marker MARKER_INTERNAL = MarkerFactory.getMarker("INTERNAL");
    public static final FifoDeque<ILoggingEvent> Statuses = new FifoDeque<>(325);

    public void close() {
    }

    public boolean requiresLayout() {
        return false;
    }

    @Override
    protected void append(ILoggingEvent eventObject) {
        //Prevents certain entries from appearing in GPS FragmentLogView View screen
        if (eventObject.getLevel().toInt() < Level.INFO.toInt()) {
            return;
        }

        //Prevents certain entries from appearing in device logcat
        if (eventObject.getMarker() == MARKER_INTERNAL) {
            return;
        }

        Statuses.add(eventObject);
    }
}
