
package ets.acmi.gnssdislogger.common.network;

import java.security.cert.X509Certificate;


public class CertificateValidationException extends RuntimeException {

    private final X509Certificate certificate;

    CertificateValidationException(X509Certificate certificate, String message, Throwable t) {
        super(message, t);
        this.certificate = certificate;
    }

    X509Certificate getCertificate() {
        return certificate;
    }

}