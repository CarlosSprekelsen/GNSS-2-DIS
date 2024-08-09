package ets.acmi.gnssdislogger.common.network;


import org.slf4j.Logger;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertStoreException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import ets.acmi.gnssdislogger.common.slf4j.Logs;


class LocalX509TrustManager implements X509TrustManager {

    private static final Logger LOG = Logs.of(LocalX509TrustManager.class);

    private X509TrustManager standardTrustManager;
    private KeyStore knownServersKeyStore;

    /**
     * Constructor for LocalX509TrustManager
     *
     * @param knownServersKeyStore Local certificates store with server certificates explicitly trusted by the user.
     * @throws CertStoreException When no default X509TrustManager instance was found in the system.
     */
    public LocalX509TrustManager(KeyStore knownServersKeyStore)
            throws NoSuchAlgorithmException, KeyStoreException, CertStoreException {
        super();
        TrustManagerFactory factory = TrustManagerFactory
                .getInstance(TrustManagerFactory.getDefaultAlgorithm());
        factory.init((KeyStore) null);
        standardTrustManager = findX509TrustManager(factory);

        this.knownServersKeyStore = knownServersKeyStore;
    }


    /**
     * Locates the first X509TrustManager provided by a given TrustManagerFactory
     *
     * @param factory TrustManagerFactory to inspect in the search for a X509TrustManager
     * @return The first X509TrustManager found in factory.
     * @throws CertStoreException When no X509TrustManager instance was found in factory
     */
    private X509TrustManager findX509TrustManager(TrustManagerFactory factory) throws CertStoreException {
        TrustManager[] tms = factory.getTrustManagers();
        for (TrustManager tm : tms) {
            if (tm instanceof X509TrustManager) {
                return (X509TrustManager) tm;
            }
        }
        return null;
    }


    /**
     * @see javax.net.ssl.X509TrustManager#checkClientTrusted(X509Certificate[],
     * String authType)
     */
    public void checkClientTrusted(X509Certificate[] certificates, String authType) throws CertificateException {
        standardTrustManager.checkClientTrusted(certificates, authType);
    }


    /**
     * @see javax.net.ssl.X509TrustManager#checkServerTrusted(X509Certificate[],
     * String authType)
     */
    public void checkServerTrusted(X509Certificate[] certificates, String authType) throws CertificateException {

        if (!isKnownServer(certificates[0])) {

            try {
                certificates[0].checkValidity();
            } catch (CertificateExpiredException c) {
                throw new CertificateValidationException(certificates[0], "Certificate is expired", c);
            } catch (CertificateNotYetValidException c) {
                throw new CertificateValidationException(certificates[0], "Certificates is not yet valid", c);
            }

            try {
                standardTrustManager.checkServerTrusted(certificates, authType);
            } catch (CertificateException c) {
                Throwable cause = c.getCause();
                Throwable previousCause = null;
                while (cause != null && cause != previousCause && !(cause instanceof CertPathValidatorException)) {
                    previousCause = cause;
                    cause = cause.getCause();
                }

                if (cause != null && cause instanceof CertPathValidatorException) {
                    throw new CertificateValidationException(certificates[0], "Certificate path validation error", c);
                } else {
                    throw new CertificateValidationException(certificates[0], "Certificate is not valid, unknown reason", c);
                }
            }

        }
    }


    /**
     * @see javax.net.ssl.X509TrustManager#getAcceptedIssuers()
     */
    public X509Certificate[] getAcceptedIssuers() {
        return standardTrustManager.getAcceptedIssuers();
    }


    private boolean isKnownServer(X509Certificate cert) {
        try {
            LOG.debug("Checking for certificate - HashCode: " + cert.hashCode() + ", Subject: " + cert.getSubjectDN().getName() + ", in keystore: " + knownServersKeyStore.isCertificateEntry(Integer.toString(cert.hashCode())));
            return (knownServersKeyStore.isCertificateEntry(Integer.toString(cert.hashCode())));
        } catch (KeyStoreException e) {
            LOG.error("Fail while checking certificate in the known-servers store", e);
            return false;
        }
    }


}