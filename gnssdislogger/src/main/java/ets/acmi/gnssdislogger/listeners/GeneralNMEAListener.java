package ets.acmi.gnssdislogger.listeners;

import android.location.OnNmeaMessageListener;
import android.os.Build;

import androidx.annotation.RequiresApi;

import ets.acmi.gnssdislogger.common.Strings;
import ets.acmi.gnssdislogger.loggers.nmea.NmeaSentence;


class GeneralNMEAListener {

    private static void processNMEASentence(String nmeaSentence, long timestamp, GeneralGnssListener listener, GnssLoggingService loggingService) {

        loggingService.onNmeaSentence(timestamp, nmeaSentence);

        if (Strings.isNullOrEmpty(nmeaSentence)) {
            return;
        }

        NmeaSentence nmea = new NmeaSentence(nmeaSentence);

        if (nmea.isLocationSentence()) {
            if (nmea.getLatestPdop() != null) {
                listener.latestPdop = nmea.getLatestPdop();
            }

            if (nmea.getLatestHdop() != null) {
                listener.latestHdop = nmea.getLatestHdop();
            }

            if (nmea.getLatestVdop() != null) {
                listener.latestVdop = nmea.getLatestVdop();
            }

            if (nmea.getGeoIdHeight() != null) {
                listener.geoIdHeight = nmea.getGeoIdHeight();
            }

            if (nmea.getAgeOfDgpsData() != null) {
                listener.ageOfDgpsData = nmea.getAgeOfDgpsData();
            }

            if (nmea.getDgpsId() != null) {
                listener.dgpsId = nmea.getDgpsId();
            }

        }

    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public static class NMEAListener24 implements android.location.OnNmeaMessageListener {

        private final GeneralGnssListener listener;
        private final GnssLoggingService loggingService;

        NMEAListener24(GeneralGnssListener listener, GnssLoggingService loggingService) {
            this.listener = listener;
            this.loggingService = loggingService;
        }

        @Override
        public void onNmeaMessage(String message, long timestamp) {
            processNMEASentence(message, timestamp, listener, loggingService);
        }
    }

    static class NMEAListenerLegacy implements OnNmeaMessageListener {

        private final GeneralGnssListener listener;
        private final GnssLoggingService loggingService;

        public NMEAListenerLegacy(GeneralGnssListener listener, GnssLoggingService loggingService) {
            this.listener = listener;
            this.loggingService = loggingService;
        }

        @Override
        public void onNmeaMessage(String nmea, long timestamp) {
            processNMEASentence(nmea, timestamp, listener, loggingService);
        }
    }


}
