package com.certificado;

import java.net.Socket;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.X509ExtendedKeyManager;
import javax.net.ssl.X509KeyManager;

public class SelecionadorDeCertificado extends X509ExtendedKeyManager {
    private final X509KeyManager keyManagerPadrao;
    private final String aliasParaUsar;

    public SelecionadorDeCertificado(X509KeyManager keyManagerPadrao, String aliasParaUsar) {
        this.keyManagerPadrao = keyManagerPadrao;
        this.aliasParaUsar = aliasParaUsar;
    }

    // --- A MÁGICA ACONTECE AQUI: Forçamos o retorno do nosso Alias ---
    @Override
    public String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket) {
        return aliasParaUsar;
    }

    @Override
    public String chooseEngineClientAlias(String[] keyType, Principal[] issuers, SSLEngine engine) {
        return aliasParaUsar;
    }
    // ------------------------------------------------------------------

    // Os métodos abaixo apenas repassam o trabalho para o gerente padrão do Java
    @Override
    public String[] getClientAliases(String keyType, Principal[] issuers) {
        return new String[]{aliasParaUsar};
    }

    @Override
    public String[] getServerAliases(String keyType, Principal[] issuers) {
        return keyManagerPadrao.getServerAliases(keyType, issuers);
    }

    @Override
    public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
        return keyManagerPadrao.chooseServerAlias(keyType, issuers, socket);
    }

    @Override
    public X509Certificate[] getCertificateChain(String alias) {
        return keyManagerPadrao.getCertificateChain(alias);
    }

    @Override
    public PrivateKey getPrivateKey(String alias) {
        return keyManagerPadrao.getPrivateKey(alias);
    }
}