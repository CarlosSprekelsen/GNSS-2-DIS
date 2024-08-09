package ets.acmi.gnssdislogger.common.events;


import androidx.annotation.Nullable;

public class CommandEvents {
    /**
     * Requests starting or stopping the logging service.
     * Called from the fragment button click events
     */
    public static class RequestToggle {
    }

    /**
     * Requests starting the logging service
     */
    public static class RequestStartStop {
        public final boolean start;

        public RequestStartStop(boolean start) {
            this.start = start;
        }
    }

    /**
     * Requests to get status of Logger
     */
    public static class GetStatus {
    }


    /**
     * Requests auto sending to targets
     */
    public static class AutoSend {
        public final String formattedFileName;

        public AutoSend(@Nullable String formattedFileName) {
            this.formattedFileName = formattedFileName;
        }
    }

    /**
     * Set a description for the next point
     */
    public static class Annotate {
        public final String annotation;

        public Annotate(String annotation) {
            this.annotation = annotation;
        }
    }

    /**
     * FragmentLogView once and stop
     */
    public static class LogOnce {
    }

    /**
     * BottomNavigationDrawerView commands
     */
    public static class MenuOptionClicked {
        public final Integer itemId;

        public MenuOptionClicked(Integer itemId) {
            this.itemId = itemId;
        }
    }

    /**
     * DIS Preference changed
     */

    public static class disPreferenceChanged {
        final String preferenceKey;
        public disPreferenceChanged(String preferenceKey) { this.preferenceKey = preferenceKey; }
    }
}
