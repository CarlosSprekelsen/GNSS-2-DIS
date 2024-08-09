
package ets.acmi.gnssdislogger.common.events;

import android.location.Location;

import com.google.android.gms.location.ActivityRecognitionResult;

public class ServiceEvents {


    /**
     * New location
     */
    public static class LocationUpdate {
        public final Location location;

        public LocationUpdate(Location loc) {
            this.location = loc;
        }
    }

    /**
     * Number of Satellites
     */
    public static class SatellitesCount {
        public final int satellitesInView;
        public final int satellitesInFix;

        public SatellitesCount(int satellitesInView, int satellitesInFix) {
            this.satellitesInFix = satellitesInFix;
            this.satellitesInView = satellitesInView;
        }
    }

    /**
     * Whether the logging service is still waiting for a location fix
     */
    public static class WaitingForLocation {
        public final boolean waiting;

        public WaitingForLocation(boolean waiting) {
            this.waiting = waiting;
        }
    }

    /**
     * Indicates that GPS/Network location services have temporarily gone away
     */
    public static class LocationServicesUnavailable {
    }

    /**
     * Status of the user's annotation, whether it has been written or is pending
     */
    public static class AnnotationStatus {
        public final boolean annotationWritten;

        public AnnotationStatus(boolean written) {
            this.annotationWritten = written;
        }
    }

    /**
     * Whether GPS logging has started; raised after the start/stop button is pressed
     */
    public static class LoggingStatus {
        public final boolean loggingStarted;

        public LoggingStatus(boolean loggingStarted) {
            this.loggingStarted = loggingStarted;
        }
    }

    /**
     * The file name has been set
     */
    public static class FileNamed {
        public final String newFileName;

        public FileNamed(String newFileName) {
            this.newFileName = newFileName;
        }
    }

    public static class ActivityRecognitionEvent {
        public final ActivityRecognitionResult result;

        public ActivityRecognitionEvent(ActivityRecognitionResult arr) {
            this.result = arr;
        }
    }
}
