package com.certificado;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DetalheCertificado {
    @SuppressWarnings("FieldMayBeFinal")
    private String alias;
    @SuppressWarnings("FieldMayBeFinal")
    private Date dataValidade;
    @SuppressWarnings("FieldMayBeFinal")
    private boolean valido;

    /**
     * Construtor da classe DetalheCertificado, recebe o alias, a data de validade e se o certificado é válido.<p>
     * motivo de ser final é para que o alias, a data de validade e se o certificado é válido não possam ser alterados.<p>
     * Precisava ser um classe separada para ser armazenado na lista de certificados.<p>
     */
    public DetalheCertificado(String alias, Date dataValidade, boolean valido) {
        this.alias = alias;
        this.dataValidade = dataValidade;
        this.valido = valido;
    }

    public String getAlias() { return alias; }
    public boolean isValido() { return valido; }
    public Date getDataValidade() { return dataValidade; }
    public String getDataValidadeFormatada() { return new SimpleDateFormat("dd/MM/yyyy").format(dataValidade); }
    
    @Override
    public String toString() {
        return "Certificado " + getAlias() + " - Data de validade: " + getDataValidadeFormatada() + " - " + (isValido() ? "Válido" : "Inválido");
    }
}
