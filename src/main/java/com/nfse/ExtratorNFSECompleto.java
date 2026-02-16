package com.nfse;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

/**
 * Classe responsável por extrair informações de arquivos PDF de NFS-e e gerar um relatório CSV.
 */
public class ExtratorNFSECompleto {

    private String caminhoPDF;

    public ExtratorNFSECompleto(String caminhoPDF) {
        this.caminhoPDF = caminhoPDF;
    }

    public String getCaminhoPDF() {
        return caminhoPDF;
    }

    public void setCaminhoPDF(String caminhoPDF) {
        this.caminhoPDF = caminhoPDF;
    }

    /**
     * Processa todos os arquivos PDF na pasta especificada e gera um arquivo CSV de relatório.
     */
    public void processar() {
        List<File> arquivosPDF = listarArquivosPDF();

        File arquivoSaida = new File("Relatorio_NFSE.csv");
        System.out.printf("Encontrados %d arquivos PDF.%n", arquivosPDF.size());

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(arquivoSaida, StandardCharsets.UTF_8))) {
            escreverCabecalhoCSV(writer);
            for (File arquivoPDF : arquivosPDF) {
                System.out.printf("Processando: %s%n", arquivoPDF.getName());
                try {
                    extrairEGravar(arquivoPDF, writer);
                } catch (IOException e) {
                    System.err.printf("Erro ao processar %s: %s%n", arquivoPDF.getName(), e.getMessage());
                }
            }
            System.out.println("Processo concluído!");
        } catch (IOException e) {
            System.err.printf("Erro ao criar ou escrever no CSV: %s%n", e.getMessage());
        }
    }

    /**
     * Processa um único PDF, extrai os campos desejados e grava uma linha no CSV.
     */
    private void extrairEGravar(File arquivoPDF, BufferedWriter writer) throws IOException {
        try (PDDocument documento = Loader.loadPDF(arquivoPDF)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String textoCompleto = stripper.getText(documento);

            String numeroNFSe       = extrairCampo(textoCompleto, "(?s)N.mero da NFS-e\\s+(\\d+)", "TEXTO");
            String dataEmissao      = extrairCampo(textoCompleto, "(?s)Data e Hora.*?(\\d{2}/\\d{2}/\\d{4})", "TEXTO");
            String valorBruto       = extrairCampo(textoCompleto, "(?s)Valor do Servi.o.*?R\\$\\s+([\\d\\.,]+)", "DECIMAL");
            String valorLiquido     = extrairCampo(textoCompleto, "(?s)Valor L.quido.*?R\\$\\s+([\\d\\.,]+)", "DECIMAL");
            String nomePrestador    = extrairCampo(textoCompleto, "(?s)Prestador.*?Nome.*?Empresarial\\s+(.*?)\\s+(?:E-mail|Endere.o|Inscri..o|CNPJ)", "TEXTO");
            String cnpjPrestador    = extrairCampo(textoCompleto, "(?s)Prestador.*?(?:CNPJ|CPF|NIF|Insc.*?Federal).*?([\\d\\./-]{14,18})", "TEXTO");
            String nomeTomador      = extrairCampo(textoCompleto, "(?s)TOMADOR.*?Nome.*?Empresarial\\s+(.*?)\\s+(?:E-mail|Endere.o|Inscri..o|CNPJ)", "TEXTO");
            String cnpjTomador      = extrairCampo(textoCompleto, "(?s)TOMADOR.*?(?:CNPJ|CPF|NIF|Insc.*?Federal).*?([\\d\\./-]{14,18})", "TEXTO");
            String codigoNacional   = extrairCampo(textoCompleto, "(?s)C.digo de Tributa..o Nacional\\s+(.*?)(?=Descri..o|C.digo de Tributa..o Mun)", "TEXTO");
            String issqnRetido      = extrairCampo(textoCompleto, "(?s)ISSQN Retido\\s+(?:R\\$\\s*)?([\\d\\.,-]+|N.o Retido)", "DECIMAL");
            String issqnApurado     = extrairCampo(textoCompleto, "(?s)ISSQN Apurado\\s+(?:R\\$\\s*)?([\\d\\.,-]+)", "DECIMAL");
            String irrf             = extrairCampo(textoCompleto, "(?s)IRRF\\s+(?:R\\$\\s*)?([\\d\\.,-]+)", "DECIMAL");
            String contribPrevRet   = extrairCampo(textoCompleto, "(?s)Contribui..o Previdenci.ria.*?Retida\\s+(?:R\\$\\s*)?([\\d\\.,-]+)", "DECIMAL");
            String contribSocRet    = extrairCampo(textoCompleto, "(?s)Contribui..es Sociais.*?Retidas\\s+(?:R\\$\\s*)?([\\d\\.,-]+)", "DECIMAL");
            String pisApuracao      = extrairCampo(textoCompleto, "(?s)PIS\\s*[-–]?\\s*D.bito Apura..o Pr.pria\\s+(?:R\\$\\s*)?([\\d\\.,-]+)", "DECIMAL");
            String cofinsApuracao   = extrairCampo(textoCompleto, "(?s)COFINS\\s*[-–]?\\s*D.bito Apura..o Pr.pria\\s+(?:R\\$\\s*)?([\\d\\.,-]+)", "DECIMAL");
            String descricaoPisCofins = extrairCampo(textoCompleto, "((?mi)^[12]\\s*-\\s*PIS/COFINS\\s*(?:Não\\s*)?Retidos.*)", "TEXTO");

            String linhaCSV = String.join(";",
                //limpar(arquivoPDF.getName()),
                limpar(dataEmissao),
                limpar(nomePrestador),
                limpar(cnpjPrestador),
                limpar(nomeTomador),
                limpar(cnpjTomador),
                limpar(numeroNFSe),
                limpar(codigoNacional),
                limpar(valorBruto),
                limpar(issqnRetido),
                limpar(issqnApurado),
                limpar(irrf),
                limpar(contribPrevRet),
                limpar(contribSocRet),
                limpar(pisApuracao),
                limpar(cofinsApuracao),
                limpar(valorLiquido),
                limpar(descricaoPisCofins)
            );
            writer.write(linhaCSV);
            writer.newLine();
            writer.flush();
        }
    }

    /**
     * Lista todos os arquivos PDF no diretório definido.
     */
    private List<File> listarArquivosPDF() {
        if (caminhoPDF == null || caminhoPDF.isEmpty()) {
            throw new IllegalArgumentException("Caminho do diretório não pode ser vazio");
        }

        if (!new File(caminhoPDF).exists()) {
            throw new IllegalArgumentException("Diretório não encontrado: " + caminhoPDF);
        }

        File diretorio = new File(caminhoPDF);
        List<File> arquivosPDF = new ArrayList<>();
        
        for (File arquivo : diretorio.listFiles()) {
            if (arquivo.isFile() && arquivo.getName().toLowerCase().endsWith(".pdf")) {
                arquivosPDF.add(arquivo);
            }
        }
        return arquivosPDF;        
    }

    /**
     * Remove quebras de linha e valores nulos, limpa para exportação CSV.
     */
    private static String limpar(String valor) {
        if (valor == null) return "";
        return valor.replace("\n", " ").replace("\r", "").trim();
    }

    /**
     * Tenta extrair o valor usando a regex fornecida. Retorna o tipo se não achar.
     */
    private String extrairCampo(String texto, String regex, String tipo) {
        if (texto == null) return tipagem(tipo);
        try {
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(texto);
            if (matcher.find()) {
                String resultado = matcher.group(1).trim();
                if (resultado.equals("-") || resultado.equals(".")) return tipagem(tipo);
                return resultado;
            }
        } catch (Exception ignored) {

        }
        return tipagem(tipo);
    }

    /**
     * Retorna o tipo de acordo com o valor fornecido.
     */
    private String tipagem(String tipo) {
        return switch (tipo.toUpperCase()) {
            case "INTEIRO" -> "0";
            case "DECIMAL" -> "0,00";
            case "TEXTO" -> "";
            default -> "";
        };
    }
    
    /**
     * Escreve o cabeçalho de colunas do CSV.
     */
    private void escreverCabecalhoCSV(BufferedWriter writer) throws IOException {
        writer.write("\ufeff"); // BOM para Excel abrir UTF-8 direto
        writer.write(
        "Data Emissao;" +
        "Prestador Nome;" + 
        "Prestador CNPJ;" + 
        "Tomador Nome;" + 
        "Tomador CNPJ;" +  
        "Numero NFS-e;" + 
        "Cod. Trib. Nacional;" + 
        "Valor Bruto;" + 
        "ISSQN Retido;" + 
        "ISSQN Apurado;" + 
        "IRRF;" + 
        "INSS;" + 
        "CSLL;" + 
        "PIS;" + 
        "COFINS;" + 
        "Valor Liquido;" + 
        "Descrição Pis/Cofins");
        writer.newLine();
    }
}