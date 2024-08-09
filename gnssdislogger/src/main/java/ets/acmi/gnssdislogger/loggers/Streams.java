package ets.acmi.gnssdislogger.loggers;


import org.slf4j.Logger;
import org.w3c.dom.Document;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import ets.acmi.gnssdislogger.common.slf4j.Logs;
import ets.acmi.gnssdislogger.common.slf4j.SessionLogcatAppender;

class Streams {

    private static final Logger LOG = Logs.of(Streams.class);

    public static byte[] getByteArrayFromInputStream(InputStream is) {

        try {
            int length;
            int size = 1024;
            byte[] buffer;

            if (is instanceof ByteArrayInputStream) {
                size = is.available();
                buffer = new byte[size];
                is.read(buffer, 0, size);
            } else {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                buffer = new byte[size];
                while ((length = is.read(buffer, 0, size)) != -1) {
                    outputStream.write(buffer, 0, length);
                }

                buffer = outputStream.toByteArray();
            }
            return buffer;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (Exception e) {
                LOG.warn(SessionLogcatAppender.MARKER_INTERNAL, "getStringFromInputStream - could not close stream");
            }
        }

        return null;

    }

    /**
     * Loops through an input stream and converts it into a string, then closes the input stream
     *
     * @param is
     * @return
     */
    public static String getStringFromInputStream(InputStream is) {
        String line;
        StringBuilder total = new StringBuilder();

        // Wrap a BufferedReader around the InputStream
        BufferedReader rd = new BufferedReader(new InputStreamReader(is));

        // Read response until the end
        try {
            while ((line = rd.readLine()) != null) {
                total.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (Exception e) {
                LOG.warn(SessionLogcatAppender.MARKER_INTERNAL, "getStringFromInputStream - could not close stream");
            }
        }

        // Return full string
        return total.toString();
    }


    /**
     * Converts an input stream containing an XML response into an XML Document object
     *
     * @param stream
     * @return
     */
    public static Document getDocumentFromInputStream(InputStream stream) {
        Document doc;

        try {
            DocumentBuilderFactory xmlFactory = DocumentBuilderFactory.newInstance();
            xmlFactory.setNamespaceAware(true);
            DocumentBuilder builder = xmlFactory.newDocumentBuilder();
            doc = builder.parse(stream);
        } catch (Exception e) {
            doc = null;
        }

        return doc;
    }

    public static long copyIntoStream(InputStream inputStream, OutputStream outputStream) {
        try {
            long res = 0;
            byte[] buffer = new byte[1024];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
                outputStream.flush();
                res += read;
            }

            inputStream.close();
            outputStream.close();
            return res;
        } catch (Exception ex) {

            LOG.error("Could not close a stream properly", ex);
        }

        return 0;

    }
}
