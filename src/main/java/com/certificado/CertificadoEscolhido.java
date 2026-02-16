package com.certificado;

import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

public class CertificadoEscolhido {
    private final PrivateKey privateKey;
    private final X509Certificate certificate;
    private final String alias;
    private final KeyStore keyStore;

    /**
     * Construtor da classe CertificadoEscolhido, recebe o private key, o certificate e o alias do certificado escolhido.<p>
     * motivo de ser final é para que o private key, o certificate e o alias do certificado escolhido não possam ser alterados.<p>
     * Precisava ser um classe separada para que o certificado escolhido possa ser usado em outras classes sem precisar carregar o KeyStore novamente.<p>
     */
    public CertificadoEscolhido(PrivateKey privateKey, X509Certificate certificate, String alias, KeyStore keyStore) {
        this.privateKey = privateKey;
        this.certificate = certificate;
        this.alias = alias;
        this.keyStore = keyStore;
    }

    public PrivateKey getPrivateKey() { return privateKey; }
    public X509Certificate getCertificate() { return certificate; }
    public String getAlias() { return alias; }
    public KeyStore getKeyStore() { return keyStore; }
}
