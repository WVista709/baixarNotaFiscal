package com.certificado;

import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;

public class Certificados {
    private KeyStore ks;
    private List<DetalheCertificado> listaCertificados;

    public Certificados() throws Exception {
        this.keyStore();
        this.listarCertificados();
    }

    /**
     * Carrega o KeyStore
     */
    private void keyStore() throws Exception {
        ks = KeyStore.getInstance("Windows-MY");
        ks.load(null, null);
    }

    /**
     * Lista os certificados no KeyStore
     */
    private void listarCertificados() throws Exception {
        Enumeration<String> aliases = ks.aliases();
        listaCertificados = new ArrayList<>();

        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            X509Certificate certificado = (X509Certificate) ks.getCertificate(alias);
            Date dataValidade = certificado.getNotAfter();
            boolean valido = dataValidade.after(new Date());
            listaCertificados.add(new DetalheCertificado(alias, dataValidade, valido));
        }
    }
    
    /**
     * Mostra os certificados na tela
     */
    public void mostrarCertificados() {
        int i = 1;
        for (DetalheCertificado detalhe : listaCertificados) {
            System.out.println(i + " - " + detalhe.toString());
            i++;
        }
    }

    /**
     * Escolhe um certificado por índice
     */
    public CertificadoEscolhido escolherCertificadoPorIndex(int index) throws Exception {
        if (listaCertificados.size() <= 0) {
            throw new Exception("Não há certificados para escolher");
        }

        if (index < 1 || index > listaCertificados.size()) {
            throw new Exception("Índice inválido");
        }

        //Escolhendo o certificado que foi armazendado na lista de certificados
        DetalheCertificado certificado = listaCertificados.get(index - 1);

        if (!certificado.isValido()) {
            throw new Exception("Certificado expirado");
        }

        //O motivo da classe CertificadoEscolhido ser uma classe separada é para que o certificado escolhido possa ser usado em outras classes sem precisar carregar o KeyStore novamente
        PrivateKey privateKey = (PrivateKey) ks.getKey(certificado.getAlias(), null);
        X509Certificate certificate = (X509Certificate) ks.getCertificate(certificado.getAlias());
        String alias = certificado.getAlias();

        //Retornando o certificado escolhido
        return new CertificadoEscolhido(privateKey, certificate, alias, this.ks);
    }
}
