package com.nfe.am;

import java.net.CookieManager;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Collectors;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.certificado.CertificadoEscolhido;
import com.web.WebScrapping;

public class BaixarNFEAM extends WebScrapping {
    
    private static final String URL_CONSULTA = "http://sistemas.sefaz.am.gov.br/nfe-consulta-ex/exibirListaXML.do?acao=submitConsultaXML";
    private static final String URL_PAGINACAO = "http://sistemas.sefaz.am.gov.br/nfe-consulta-ex/exibirListaXML.do?acao=paginarConsultaArquivo";
    private static final String URL_DOWNLOAD = "http://sistemas.sefaz.am.gov.br/nfe-consulta-ex/exibirListaXML.do?acao=download";

    private Scanner scanner;

    /**
     * Construtor que recebe o certificado escolhido e o cookie manager existente caso seja necessário.
     */
    public BaixarNFEAM(CertificadoEscolhido certificadoEscolhido, CookieManager cookieManager) throws Exception {
        super(certificadoEscolhido, cookieManager); 
        this.scanner = new Scanner(System.in);
    }

    /**
     * Função que exibe o menu e solicita ao usuário que escolha uma opção
     */
    @Override
    public void acessarSite() throws Exception {
        int opcao = -1;
        
        while (opcao != 0) {
            //Exibe o menu de opções
            menu();
            
            //Verifica se o usuário digitou um número inteiro
            if (scanner.hasNextInt()) {
                opcao = scanner.nextInt();
                scanner.nextLine();
 
                if (opcao == 0) {
                    System.out.println("Encerrando...");
                    break;
                } else if (opcao < 1 || opcao > 9) {
                    System.out.println("Opção inválida. Tente novamente.\n");
                    continue;
                }

                System.out.println("\n--- Período da Consulta ---");
                System.out.print("Digite a data de INÍCIO (ex: 01/01/2026): ");
                String dataInicio = scanner.nextLine();
                
                System.out.print("Digite a data de FIM (ex: 31/01/2026): ");
                String dataFim = scanner.nextLine();

                System.out.println("\nIniciando busca para o período de " + dataInicio + " até " + dataFim + "...");

                // Mapeia a escolha do usuário repassando as datas digitadas
                switch (opcao) {
                    case 1 -> // NF-e Autorizadas (Modelo 55)
                        consultarEBaixarNFE("55", "", "", "", dataInicio, dataFim, "", "EMITIDAS", "AUTORIZADAS", "TODAS", "", "", "");
                    case 2 -> // NF-e Canceladas
                        consultarEBaixarNFE("55", "", "", "", dataInicio, dataFim, "", "EMITIDAS", "CANCELADAS", "TODAS", "", "", "");
                    case 3 -> // NF-e TODAS
                        consultarEBaixarNFE("55", "", "", "", dataInicio, dataFim, "", "EMITIDAS", "TODAS", "TODAS", "", "", "");
                    case 4 -> // NFC-e Autorizadas (Modelo 65)
                        consultarEBaixarNFE("65", "", "", "", dataInicio, dataFim, "", "EMITIDAS", "AUTORIZADAS", "TODAS", "", "", "");
                    case 5 -> // NFC-e Canceladas
                        consultarEBaixarNFE("65", "", "", "", dataInicio, dataFim, "", "EMITIDAS", "CANCELADAS", "TODAS", "", "", "");
                    case 6 -> // NFC-e TODAS
                        consultarEBaixarNFE("65", "", "", "", dataInicio, dataFim, "", "EMITIDAS", "TODAS", "TODAS", "", "", "");
                    case 7 -> // CT-e Autorizadas (Modelo 57)
                        consultarEBaixarNFE("57", "", "", "", dataInicio, dataFim, "", "EMITIDAS", "AUTORIZADAS", "TODAS", "", "", "");
                    case 8 -> // CT-e Canceladas
                        consultarEBaixarNFE("57", "", "", "", dataInicio, dataFim, "", "EMITIDAS", "CANCELADAS", "TODAS", "", "", "");
                    case 9 -> // CT-e TODAS
                        consultarEBaixarNFE("57", "", "", "", dataInicio, dataFim, "", "EMITIDAS", "TODAS", "TODAS", "", "", "");
                }
            } else {
                System.out.println("Entrada inválida. Digite um número.\n");
                scanner.next();
            }
        }
    }

    /**
     * Função que exibe o menu de opções.
     */
    private void menu() {
        System.out.println("\n--- MENU DE DOWNLOAD SEFAZ-AM ---");
        System.out.println("1 - Baixar NF-e Autorizadas");
        System.out.println("2 - Baixar NF-e Canceladas");
        System.out.println("3 - Baixar NF-e TODAS");
        System.out.println("4 - Baixar NFC-e Autorizadas");
        System.out.println("5 - Baixar NFC-e Canceladas");
        System.out.println("6 - Baixar NFC-e TODAS");
        System.out.println("7 - Baixar CT-e Autorizadas");
        System.out.println("8 - Baixar CT-e Canceladas");
        System.out.println("9 - Baixar CT-e TODAS");
        System.out.println("0 - Sair");
        System.out.print("Escolha uma opção: ");
    }

    /**
     * Função que realiza a consulta e baixa as NF-e, NFC-e e CT-e.
     */
    private void consultarEBaixarNFE(String modelo, String serie, String numero, String cfop, 
        String emitidasPeriodoDe, String emitidasPeriodoAte, String codUf, String origemNFe, 
        String situacaoNFe, String notaRejeitada, String cnpjRemetente, String cnpjCpfDestinatario, 
        String cnpjTomador) throws Exception {
        
        Map<String, String> formData = new HashMap<>();
        formData.put("modelo", modelo); 
        formData.put("serie", serie);
        formData.put("numero", numero);
        formData.put("cfop", cfop);
        formData.put("emitidasPeriodoDe", emitidasPeriodoDe); 
        formData.put("emitidasPeriodoAte", emitidasPeriodoAte); 
        formData.put("codUf", codUf);
        formData.put("origemNFe", origemNFe); 
        formData.put("situacaoNFe", situacaoNFe); 
        formData.put("notaRejeitada", notaRejeitada);
        formData.put("cnpjRemetente", cnpjRemetente);
        formData.put("cnpjCpfDestinatario", cnpjCpfDestinatario);
        formData.put("cnpjTomador", cnpjTomador);

        String formUrlEncoded = formData.entrySet().stream()
            .map(entry -> URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8) + "=" +
                          URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
            .collect(Collectors.joining("&"));
        
        System.out.println("Realizando consulta inicial na SEFAZ...");
        Document resultado = postPagina(URL_CONSULTA, formUrlEncoded);
        gerarTXT(resultado, "BaixarNFE_resultado.txt");

        int totalPaginas = 1;
        Elements linksPaginacao = resultado.select("span.pagelinks a");
        for (Element link : linksPaginacao) {
            String titulo = link.attr("title");
            if (titulo != null && titulo.startsWith("Ir para a página")) {
                int numPagina = Integer.parseInt(titulo.replaceAll("\\D", ""));
                if (numPagina > totalPaginas) {
                    totalPaginas = numPagina;
                }
            }
        }
        System.out.println("Total de páginas detectadas: " + totalPaginas);

        for (int pagina = 1; pagina <= totalPaginas; pagina++) {
            System.out.println("--- Processando página " + pagina + " de " + totalPaginas + " ---");

            if (pagina > 1) {
                formData.put("d-49612-p", String.valueOf(pagina));
                String formPaginadoEncoded = formData.entrySet().stream()
                        .map(entry -> URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8) + "=" +
                                      URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
                        .collect(Collectors.joining("&"));
                
                resultado = postPagina(URL_PAGINACAO, formPaginadoEncoded);
            }

            Elements notasHtml = resultado.select("input[type=checkbox][name=chaves]");
            List<String> chaves = new ArrayList<>();

            for (Element nota : notasHtml) {
                chaves.add(nota.attr("value"));
            }

            if (chaves.isEmpty()) {
                System.out.println("Nenhuma chave encontrada na página " + pagina + ".");
                continue; 
            }

            StringBuilder sb = new StringBuilder();
            for (String chave : chaves) {
                if (sb.length() > 0) sb.append("&");
                sb.append("chaves=").append(URLEncoder.encode(chave, StandardCharsets.UTF_8));
            }
            sb.append("&d-49612-p=");

            System.out.println("Baixando " + chaves.size() + " arquivos XML...");
            
            byte[] arquivoZip = postPaginaBytes(URL_DOWNLOAD, sb.toString());
            
            String nomeArquivo = "notas_modelo_" + modelo + "_pag" + pagina + ".zip";
            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(nomeArquivo)) {
                fos.write(arquivoZip);
            }
            
            System.out.println("Sucesso! Arquivo salvo como: " + nomeArquivo);
        }
        System.out.println("Download finalizado para esta consulta!\n");
    }

    // Método que faz o POST e retorna um array de bytes (necessário para baixar arquivos ZIP)
    private byte[] postPaginaBytes(String url, String parametros) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0.0.0 Safari/537.36")
                .POST(HttpRequest.BodyPublishers.ofString(parametros))
                .build();
    
        // "cliente" vem por herança da WebScrapping e já tem SSL/Cookies configurados
        HttpResponse<byte[]> response = cliente.send(request, HttpResponse.BodyHandlers.ofByteArray());
    
        if (response.statusCode() >= 400) {
            throw new Exception("Erro HTTP " + response.statusCode() + " ao baixar arquivo de " + url);
        }
    
        return response.body();
    }
}