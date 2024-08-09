package ets.acmi.gnssdislogger.senders;

import java.io.File;
import java.io.FilenameFilter;
import java.util.List;

public abstract class FileSender implements FilenameFilter {
    /**
     * Upload or send these given files
     */
    public abstract void uploadFile(List<File> files);

    /**
     * Whether the sender is enabled and ready to be used for manual uploads
     */
    public abstract boolean isAvailable();

    /**
     * Whether the user has enabled this preference for automatic sending
     */
    protected abstract boolean hasUserAllowedAutoSending();

    /**
     * Whether this sender is available and allowed to automatically send files.
     * It checks both {@link #isAvailable()} and {@link #hasUserAllowedAutoSending()}
     */
    public boolean isAutoSendAvailable() {
        return hasUserAllowedAutoSending() && isAvailable();
    }


}
