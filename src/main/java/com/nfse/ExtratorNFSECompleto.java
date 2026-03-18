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

    private static final Pattern PATTERN_NUMERO_NFSE = Pattern.compile("Número da NFS-e\\s+(\\d+)");
    private static final Pattern PATTERN_DATA_EMISSAO = Pattern.compile("Data e Hora da emissão da NFS-e\\s+(.*)");
    private static final Pattern PATTERN_VALOR_BRUTO = Pattern.compile("Valor do Serviço\\s+(.*)");
    private static final Pattern PATTERN_VALOR_LIQUIDO = Pattern.compile("Valor Líquido da NFS-e\\s+(.*)");
    private static final Pattern PATTERN_NOME_PRESTADOR = Pattern.compile("(?s)Prestador.*?Nome.*?Empresarial\\s+(.*?)\\s+(?:E-mail|Endere.o|Inscri..o|CNPJ)");
    private static final Pattern PATTERN_CNPJ_PRESTADOR = Pattern.compile("(?s)Prestador.*?(?:CNPJ|CPF|NIF|Insc.*?Federal).*?([\\d\\./-]{14,18})");
    private static final Pattern PATTERN_NOME_TOMADOR = Pattern.compile("(?s)TOMADOR.*?Nome.*?Empresarial\\s+(.*?)\\s+(?:E-mail|Endere.o|Inscri..o|CNPJ)");
    private static final Pattern PATTERN_CNPJ_TOMADOR = Pattern.compile("(?s)TOMADOR.*?(?:CNPJ|CPF|NIF|Insc.*?Federal).*?([\\d\\./-]{14,18})");
    private static final Pattern PATTERN_CODIGO_NACIONAL = Pattern.compile("(?s)C.digo de Tributa..o Nacional\\s+(.*?)(?=Descri..o|C.digo de Tributa..o Mun)");
    private static final Pattern PATTERN_ISSQN_RETIDO = Pattern.compile("ISSQN Retido\\s+(.*)");
    private static final Pattern PATTERN_ISSQN_APURADO = Pattern.compile("ISSQN Apurado\\s+(.*)");
    private static final Pattern PATTERN_ISSQN_RETENCAO = Pattern.compile("Retenção do ISSQN\\s+(.*)");
    private static final Pattern PATTERN_IRRF = Pattern.compile("IRRF\\s+(.*)");
    private static final Pattern PATTERN_CONTRIB_PREV_RET = Pattern.compile("Contribuição Previdenciária - Retida\\s+(.*)");
    private static final Pattern PATTERN_CONTRIB_SOC_RET = Pattern.compile("Contribuições Sociais - Retidas\\s+(.*)");
    private static final Pattern PATTERN_PIS_APURACAO = Pattern.compile("PIS - Débito Apuração Própria\\s+(.*)");
    private static final Pattern PATTERN_COFINS_APURACAO = Pattern.compile("COFINS - Débito Apuração Própria\\s+(.*)");
    private static final Pattern PATTERN_DESC_PIS_COFINS = Pattern.compile("Descrição Contrib. Sociais - Retidas\\s+(.*)");

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
        String numeroNFSe = "";
        String dataEmissao = "";
        String nomePrestador = "";

        try (PDDocument documento = Loader.loadPDF(arquivoPDF)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String textoCompleto = stripper.getText(documento);
            //System.out.println(textoCompleto);

            numeroNFSe       = extrairCampo(textoCompleto, PATTERN_NUMERO_NFSE, "TEXTO");
            dataEmissao      = extrairCampo(textoCompleto, PATTERN_DATA_EMISSAO, "TEXTO");
            String valorBruto       = extrairCampo(textoCompleto, PATTERN_VALOR_BRUTO, "DECIMAL");
            String valorLiquido     = extrairCampo(textoCompleto, PATTERN_VALOR_LIQUIDO, "DECIMAL");
            nomePrestador    = extrairCampo(textoCompleto, PATTERN_NOME_PRESTADOR, "TEXTO");
            String cnpjPrestador    = extrairCampo(textoCompleto, PATTERN_CNPJ_PRESTADOR, "TEXTO");
            String nomeTomador      = extrairCampo(textoCompleto, PATTERN_NOME_TOMADOR, "TEXTO");
            String cnpjTomador      = extrairCampo(textoCompleto, PATTERN_CNPJ_TOMADOR, "TEXTO");
            String codigoNacional   = extrairCampo(textoCompleto, PATTERN_CODIGO_NACIONAL, "TEXTO");
            String retencaoISSQN    = extrairCampo(textoCompleto, PATTERN_ISSQN_RETENCAO, "DECIMAL");
            String issqnRetido      = extrairCampo(textoCompleto, PATTERN_ISSQN_RETIDO, "DECIMAL");
            String issqnApurado     = extrairCampo(textoCompleto, PATTERN_ISSQN_APURADO, "DECIMAL");
            String irrf             = extrairCampo(textoCompleto, PATTERN_IRRF, "DECIMAL");
            String contribPrevRet   = extrairCampo(textoCompleto, PATTERN_CONTRIB_PREV_RET, "DECIMAL");
            String contribSocRet    = extrairCampo(textoCompleto, PATTERN_CONTRIB_SOC_RET, "DECIMAL");
            String pisApuracao      = extrairCampo(textoCompleto, PATTERN_PIS_APURACAO, "DECIMAL");
            String cofinsApuracao   = extrairCampo(textoCompleto, PATTERN_COFINS_APURACAO, "DECIMAL");
            String descricaoPisCofins = extrairCampo(textoCompleto, PATTERN_DESC_PIS_COFINS, "TEXTO");

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
                limpar(retencaoISSQN),
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

        // Renomear o arquivo PDF após fechar o documento para evitar erro de arquivo em uso
        if (nomePrestador != null && !nomePrestador.isEmpty() && 
            dataEmissao != null && !dataEmissao.isEmpty() && 
            numeroNFSe != null && !numeroNFSe.isEmpty()) {
            
            String nomePrestadorLimpo = nomePrestador.replaceAll("[^a-zA-Z0-9 ]", "").trim();
            
            String dataStr = dataEmissao.split(" ")[0];
            String[] partesData = dataStr.split("/");
            String dataEmissaoLimpa = partesData.length == 3 
                ? partesData[2] + " " + partesData[1] + " " + partesData[0] 
                : dataStr.replace("/", " ");
                
            String novoNome = String.format("%s %s NFSE %s.pdf", nomePrestadorLimpo, dataEmissaoLimpa, numeroNFSe);
            
            File novoArquivo = new File(arquivoPDF.getParent(), novoNome);
            if (!novoArquivo.exists()) {
                boolean renomeado = arquivoPDF.renameTo(novoArquivo);
                if (renomeado) {
                    System.out.printf("Arquivo renomeado para: %s%n", novoNome);
                } else {
                    System.err.printf("Falha ao renomear o arquivo: %s%n", arquivoPDF.getName());
                }
            }
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
    private String extrairCampo(String texto, Pattern pattern, String tipo) {
        if (texto == null) return tipagem(tipo);
        try {
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
        "Retenção ISSQN;" + 
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