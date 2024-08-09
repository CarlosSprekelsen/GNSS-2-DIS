package ets.acmi.gnssdislogger.common;

import androidx.core.content.FileProvider;

//This class exists purely to avoid collisions with any
//third-party libraries that might also have a
//file provider.
public class GnssLoggerFileProvider extends FileProvider {
}
