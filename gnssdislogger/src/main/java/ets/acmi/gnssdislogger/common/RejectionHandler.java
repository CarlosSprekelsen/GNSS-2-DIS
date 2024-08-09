package ets.acmi.gnssdislogger.common;

import org.slf4j.Logger;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

import ets.acmi.gnssdislogger.common.slf4j.Logs;
import ets.acmi.gnssdislogger.common.slf4j.SessionLogcatAppender;

public class RejectionHandler implements RejectedExecutionHandler {

    private static final Logger LOG = Logs.of(RejectionHandler.class);

    @Override
    public void rejectedExecution(Runnable runnable, ThreadPoolExecutor threadPoolExecutor) {
        LOG.warn(SessionLogcatAppender.MARKER_INTERNAL, "Could not queue task, some points may not be logged.");
    }
}

