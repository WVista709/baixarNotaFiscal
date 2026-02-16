package com.program;

import com.certificado.CertificadoEscolhido;
import com.certificado.Certificados;
import com.nfe.am.SiteSefazAM;

public class Main {
    public static void main(String[] args) {
        try {
        Certificados certificados = new Certificados();
        certificados.mostrarCertificados();
        CertificadoEscolhido certificadoEscolhido = certificados.escolherCertificadoPorIndex(1);
        SiteSefazAM siteSefazAM = new SiteSefazAM(certificadoEscolhido);
        siteSefazAM.acessarSite();
        } catch (Exception e) {
            System.out.println("Erro ao acessar site: " + e.getMessage());
        }
    }
}