package com.certificado;

import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

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

    /**
     * Extrai o Common Name (CN) do certificado de forma segura.
     */
    private String getCommonName() throws Exception {
        String mensagem = "Erro no método getCommonName da classe CertificadoEscolhido: ";
        // Pega o nome completo do certificado (DN)
        String dn = certificate.getSubjectX500Principal().getName();
        LdapName ldapDN = new LdapName(dn);
        
        // Procura especificamente pelo atributo CN
        for (Rdn rdn : ldapDN.getRdns()) {
            if (rdn.getType().equalsIgnoreCase("CN")) {
                return rdn.getValue().toString();
            }
        }

        throw new Exception(mensagem + "Common Name não encontrado no certificado");
    }

    /**
     * Retorna a Razão Social da Empresa.
     */
    public String getNomeEmpresa() throws Exception {
        String cn = getCommonName();
        // O padrão ICP-Brasil costuma colocar o CNPJ após os ":"
        if (cn != null && cn.contains(":")) {
            return cn.split(":")[0].trim(); // Retorna só a parte antes dos ":"
        }
        return cn; // Se não tiver ":", retorna o CN inteiro
    }

    /**
     * Retorna apenas os 14 dígitos numéricos do CNPJ.
     */
    public String getCnpj() throws Exception {
        String cn = getCommonName();
        
        // 1ª Tentativa: Pelo padrão ICP-Brasil (NomeEmpresa:CNPJ) no CN
        if (cn != null && cn.contains(":")) {
            String possivelCnpj = cn.split(":")[1].trim();
            // Verifica se a parte após os ":" são exatamente 14 números
            if (possivelCnpj.matches("\\d{14}")) {
                return possivelCnpj;
            }
        }
        
        // 2ª Tentativa (Fallback): Procura qualquer bloco de 14 números seguidos no certificado inteiro
        String dn = certificate.getSubjectX500Principal().getName();
        Matcher matcher = Pattern.compile("\\b\\d{14}\\b").matcher(dn);
        if (matcher.find()) {
            return matcher.group();
        }
        
        return "CNPJ não encontrado";
    }

    public PrivateKey getPrivateKey() { return privateKey; }
    public X509Certificate getCertificate() { return certificate; }
    public String getAlias() { return alias; }
    public KeyStore getKeyStore() { return keyStore; }
}
