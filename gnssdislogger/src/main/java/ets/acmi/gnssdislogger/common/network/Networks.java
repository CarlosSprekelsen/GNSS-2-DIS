package ets.acmi.gnssdislogger.common.network;


import android.content.Context;
import android.os.Handler;

import org.slf4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertStoreException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

import ets.acmi.gnssdislogger.R;
import ets.acmi.gnssdislogger.common.slf4j.Logs;
import ets.acmi.gnssdislogger.loggers.Files;
import ets.acmi.gnssdislogger.ui.Dialogs;

public class Networks {

    private static final Logger LOG = Logs.of(Networks.class);

    private static final String LOCAL_TRUSTSTORE_FILENAME = "knownservers.bks";
    private static final String LOCAL_TRUSTSTORE_PASSWORD = "politelemon";

    public static KeyStore getKnownServersStore(Context context)
            throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {

        KeyStore mKnownServersStore = KeyStore.getInstance(KeyStore.getDefaultType());
        File localTrustStoreFile = new File(Files.storageFolder(context), LOCAL_TRUSTSTORE_FILENAME);

        LOG.debug("Getting local truststore - " + localTrustStoreFile.getAbsolutePath());
        if (localTrustStoreFile.exists()) {
            try (InputStream in = new FileInputStream(localTrustStoreFile)) {
                mKnownServersStore.load(in, LOCAL_TRUSTSTORE_PASSWORD.toCharArray());
            }
        } else {
            // next is necessary to initialize an empty KeyStore instance
            mKnownServersStore.load(null, LOCAL_TRUSTSTORE_PASSWORD.toCharArray());
        }

        return mKnownServersStore;
    }


    public static void addCertToKnownServersStore(Certificate cert, Context context)
            throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {

        File localTrustStoreFile = new File(Files.storageFolder(context), LOCAL_TRUSTSTORE_FILENAME);

        KeyStore knownServers = Networks.getKnownServersStore(context);
        LOG.debug("Adding certificate - HashCode: " + cert.hashCode());
        knownServers.setCertificateEntry(Integer.toString(cert.hashCode()), cert);

        try (FileOutputStream fos = new FileOutputStream(localTrustStoreFile)) {
            //fos = context.openFileOutput(localTrustStoreFile.getName(), Context.MODE_PRIVATE);
            knownServers.store(fos, LOCAL_TRUSTSTORE_PASSWORD.toCharArray());
        } catch (Exception e) {
            LOG.error("Could not save certificate", e);
        }
    }

    public static CertificateValidationException extractCertificateValidationException(Exception e) {

        if (e == null) {
            return null;
        }

        CertificateValidationException result = null;

        if (e instanceof CertificateValidationException) {
            return (CertificateValidationException) e;
        }
        Throwable cause = e.getCause();
        Throwable previousCause = null;
        while (cause != null && cause != previousCause && !(cause instanceof CertificateValidationException)) {
            previousCause = cause;
            cause = cause.getCause();
        }
        if (cause != null && cause instanceof CertificateValidationException) {
            result = (CertificateValidationException) cause;
        }
        return result;
    }

    public static SSLSocketFactory getSocketFactory(Context context) {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            LocalX509TrustManager atm;

            atm = new LocalX509TrustManager(getKnownServersStore(context));

            TrustManager[] tms = new TrustManager[]{atm};
            sslContext.init(null, tms, null);
            return sslContext.getSocketFactory();
        } catch (Exception e) {
            LOG.error("Could not get SSL Socket factory ", e);
        }

        return null;
    }

    public static void beginCertificateValidationWorkflow(Context context, String host, int port, ServerType serverType) {
        Handler postValidationHandler = new Handler();
        Dialogs.progress(context, context.getString(R.string.please_wait), context.getString(R.string.please_wait));
        new Thread(new CertificateValidationWorkflow(context, host, port, serverType, postValidationHandler)).start();
    }


    public static TrustManager getTrustManager(Context context)
            throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException, CertStoreException {
        return new LocalX509TrustManager(getKnownServersStore(context));
    }
}
