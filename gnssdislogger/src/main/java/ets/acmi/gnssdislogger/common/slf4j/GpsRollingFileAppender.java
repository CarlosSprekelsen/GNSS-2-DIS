package ets.acmi.gnssdislogger.common.slf4j;

import ch.qos.logback.core.rolling.RollingFileAppender;
import ets.acmi.gnssdislogger.common.PreferenceHelper;


class GpsRollingFileAppender<E> extends RollingFileAppender<E> {

    @Override
    protected void subAppend(E e) {

        //This extends the RollingFileAppender.
        // It checks if the user has requested a
        // debug log file and only then writes
        // to a file.
        if (PreferenceHelper.getInstance().shouldDebugToFile()) {
            super.subAppend(e);
        }
    }
}
