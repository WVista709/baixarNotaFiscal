package com.nfe.am;

import java.io.IOException;
import java.net.CookieManager;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    private static final String URL_EXPORTAR = "http://sistemas.sefaz.am.gov.br/nfe-consulta-ex/exibirListaXML.do?acao=exportar";

    @SuppressWarnings("FieldMayBeFinal")
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
        while (true) {
            // --- PASSO 1: ORIGEM ---
            menu(); // Chama a sua função de menu principal
            
            int opOrigem = lerInteiro();
            if (opOrigem == 0) {
                System.out.println("Encerrando o módulo de download...");
                break;
            }
            if (opOrigem < 1 || opOrigem > 2) {
                System.out.println("Opção inválida! Tente novamente.");
                continue;
            }
            String origemStr = (opOrigem == 1) ? "EMITIDAS" : "RECEBIDAS";

            // --- PASSO 2: TIPO (MODELO) ---
            menuModelo();
            
            int opModelo = lerInteiro();
            if (opModelo == 0) continue;
            if (opModelo < 1 || opModelo > 3) {
                System.out.println("Opção inválida!");
                continue;
            }
            String modeloStr = switch (opModelo) {
                case 1 -> "55";
                case 2 -> "65";
                case 3 -> "57";
                default -> "55";
            };

            // --- PASSO 3: SITUAÇÃO ---
            menuSituacao();
            
            int opSituacao = lerInteiro();
            if (opSituacao == 0) continue;
            if (opSituacao < 1 || opSituacao > 3) {
                System.out.println("Opção inválida!");
                continue;
            }
            String situacaoStr = switch (opSituacao) {
                case 1 -> "AUTORIZADAS";
                case 2 -> "CANCELADAS";
                case 3 -> "TODAS";
                default -> "TODAS";
            };

            // --- PASSO 4: DATAS ---
            System.out.println("\n--- 4. PERÍODO DA CONSULTA ---");
            System.out.print("Digite a data de INÍCIO (ex: 01/01/2026): ");
            String dataInicio = scanner.nextLine().trim();
            
            System.out.print("Digite a data de FIM (ex: 31/01/2026): ");
            String dataFim = scanner.nextLine().trim();

            // --- PASSO 5: FORMATO DE DOWNLOAD ---
            menuFormato();
            
            int opFormato = lerInteiro();
            if (opFormato < 1 || opFormato > 3) {
                System.out.println("Opção inválida, assumindo 'Ambos' (Opção 3).");
                opFormato = 3;
            }

            // --- PASSO 6: EXECUÇÃO ---
            System.out.printf("\nIniciando processamento: Origem=%s, Modelo=%s, Situação=%s, Período=%s a %s...\n", 
                              origemStr, modeloStr, situacaoStr, dataInicio, dataFim);
            
            // Executa o CSV se o usuário escolheu 1 ou 3
            if (opFormato == 1 || opFormato == 3) {
                baixarCSV(modeloStr, dataInicio, dataFim, origemStr, situacaoStr);
            }
            
            // Executa o XML se o usuário escolheu 2 ou 3
            if (opFormato == 2 || opFormato == 3) {
                consultarEBaixarNFE(modeloStr, "", "", "", dataInicio, dataFim, "", origemStr, situacaoStr, "TODAS", "", "", "");
            }
        }
    }

    /**
     * Função que exibe o menu principal (Origem dos Documentos).
     */
    private void menu() {
        System.out.println("\n==================================");
        System.out.println("     DOWNLOAD SEFAZ-AM (XML)      ");
        System.out.println("==================================");
        System.out.println("\n--- 1. ORIGEM DOS DOCUMENTOS ---");
        System.out.println("1 - Emitidas");
        System.out.println("2 - Recebidas");
        System.out.println("0 - Sair");
        System.out.print("Escolha a origem: ");
    }

    /**
     * Função que exibe o submenu de Formato de Download.
     */
    private void menuFormato() {
        System.out.println("\n--- 5. FORMATO DE DOWNLOAD ---");
        System.out.println("1 - Apenas Relatório (CSV)");
        System.out.println("2 - Apenas Notas Fiscais (XML em ZIP)");
        System.out.println("3 - Ambos (CSV e XML)");
        System.out.print("Escolha uma opção: ");
    }

    /**
     * Função que exibe o submenu de Modelos.
     */
    private void menuModelo() {
        System.out.println("\n--- 2. TIPO DE DOCUMENTO ---");
        System.out.println("1 - NF-e (Modelo 55)");
        System.out.println("2 - NFC-e (Modelo 65)");
        System.out.println("3 - CT-e (Modelo 57)");
        System.out.println("0 - Voltar/Cancelar");
        System.out.print("Escolha o tipo: ");
    }

    /**
     * Função que exibe o submenu de Situação.
     */
    private void menuSituacao() {
        System.out.println("\n--- 3. SITUAÇÃO DO DOCUMENTO ---");
        System.out.println("1 - Autorizadas");
        System.out.println("2 - Canceladas");
        System.out.println("3 - Todas");
        System.out.println("0 - Voltar/Cancelar");
        System.out.print("Escolha a situação: ");
    }

    /**
     * Método auxiliar para ler opções numéricas de forma segura sem dar crash
     */
    private int lerInteiro() {
        if (scanner.hasNextInt()) {
            int valor = scanner.nextInt();
            scanner.nextLine(); // Consome a quebra de linha do Enter
            return valor;
        } else {
            scanner.nextLine(); // Limpa o texto inválido que o usuário digitou
            return -1;
        }
    }

    /**
     * Faz o download do relatório em formato CSV da consulta atual gravada na sessão.
     */
    private Path baixarCSV(String modelo, String emitidasPeriodoDe, String emitidasPeriodoAte, String origemNFe, String situacaoNFe) throws Exception {
        System.out.println("\nPreparando consulta para exportação do CSV...");
        
        // 1. Prepara a consulta para a Sefaz (mesmo payload usado na busca de XML)
        Map<String, String> formData = new HashMap<>();
        formData.put("modelo", modelo); 
        formData.put("serie", "");
        formData.put("numero", "");
        formData.put("cfop", "");
        formData.put("emitidasPeriodoDe", emitidasPeriodoDe); 
        formData.put("emitidasPeriodoAte", emitidasPeriodoAte); 
        formData.put("codUf", "");
        formData.put("origemNFe", origemNFe); 
        formData.put("situacaoNFe", situacaoNFe); 
        formData.put("notaRejeitada", "TODAS");
        formData.put("cnpjRemetente", "");
        formData.put("cnpjCpfDestinatario", "");
        formData.put("cnpjTomador", "");

        String formUrlEncoded = formData.entrySet().stream()
            .map(entry -> URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8) + "=" +
                          URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
            .collect(Collectors.joining("&"));
        
        // 2. Faz o POST inicial para a Sefaz registrar a busca na sessão
        postPagina(URL_CONSULTA, formUrlEncoded);

        // 3. Agora sim, faz o POST pedindo o CSV dessa busca!
        System.out.println("Solicitando arquivo CSV ao servidor...");
        byte[] arquivoCsv = postPaginaBytes(URL_EXPORTAR, "d-49612-p=");

        // 4. Cria a pasta final com o nome da empresa
        Path pastaFinal = Path.of(certificadoEscolhido.getNomeEmpresa() + "-" + certificadoEscolhido.getCnpj());
        Files.createDirectories(pastaFinal);

        // 5. Define o NOME DO ARQUIVO CSV
        MenuItem menuItem = new MenuItem(modelo, emitidasPeriodoDe, emitidasPeriodoAte, origemNFe, situacaoNFe);
        Path arquivoCsvFinal = pastaFinal.resolve(menuItem.getOrigemNFe() 
                + "_" + menuItem.getSituacaoNFe() 
                + "_" + menuItem.getModelo() 
                + "_" + menuItem.getEmitidasPeriodoDe() 
                + "_" + menuItem.getEmitidasPeriodoAte() 
                + ".csv");

        // 6. Grava no disco
        Files.write(arquivoCsvFinal, arquivoCsv);
        System.out.println(">>> SUCESSO! Relatório CSV salvo em: " + arquivoCsvFinal.toAbsolutePath());
        
        return arquivoCsvFinal;
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
            
            Path pastaTMP = Paths.get("tmp");
            Files.createDirectories(pastaTMP);

            Path criandoZipTMP = pastaTMP.resolve("notas_modelo_" + modelo + "_pag" + pagina + ".zip");
            Files.write(criandoZipTMP, arquivoZip);
        }
        
        // 1. Cria a pasta final com o nome da empresa
        Path pastaFinal = Path.of(certificadoEscolhido.getNomeEmpresa() + "-" + certificadoEscolhido.getCnpj());
        Files.createDirectories(pastaFinal);

        // 2. Define o NOME DO ARQUIVO ZIP dentro dessa pasta final
        MenuItem menuItem = new MenuItem(modelo, emitidasPeriodoDe, emitidasPeriodoAte, origemNFe, situacaoNFe);
        Path arquivoZipFinal = pastaFinal.resolve(menuItem.getOrigemNFe() 
        + "_" + menuItem.getSituacaoNFe() 
        + "_" + menuItem.getModelo() 
        + "_" + menuItem.getEmitidasPeriodoDe() 
        + "_" + menuItem.getEmitidasPeriodoAte() 
        + ".zip");

        // 3. Passa a pasta temporária (origem) e o ARQUIVO final (destino)
        unificarZipsModerno(Path.of("tmp"), arquivoZipFinal);
        
        // 4. (Opcional) Apaga a pasta "tmp" no final, já que ela estará vazia
        Files.deleteIfExists(Path.of("tmp"));

        System.out.println("Consulta e download finalizados com sucesso!\n");
    }

    /**
     * Unifica os Zips usando a API moderna NIO.2 (Zip File System)
     */
    private void unificarZipsModerno(Path pastaOrigem, Path arquivoZipFinal) throws Exception {
        System.out.println("Unificando todos os XMLs em um único arquivo...");

        // Configuração do Java 11+ (Map.of) para criar o ZIP final se não existir
        Map<String, String> propriedades = Map.of("create", "true");
        
        // No NIO.2, tratamos o ZIP como um sistema de arquivos acessado via URI "jar:file:..."
        URI uriZipFinal = URI.create("jar:" + arquivoZipFinal.toUri().toString());

        // Abre (ou cria) o ZIP final como uma "pasta virtual"
        try (FileSystem zipFinalFS = FileSystems.newFileSystem(uriZipFinal, propriedades)) {

            // Lê todos os zips parciais da pasta
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(pastaOrigem, "*.zip")) {
                for (Path zipParcial : stream) {
                    
                    // Proteção: não processar o zip final caso ele tenha sido salvo na mesma pasta
                    if (zipParcial.equals(arquivoZipFinal)) continue;

                    URI uriZipParcial = URI.create("jar:" + zipParcial.toUri().toString());

                    // Abre o ZIP parcial como outra "pasta virtual" (somente leitura)
                    try (FileSystem zipParcialFS = FileSystems.newFileSystem(uriZipParcial, Map.of())) {
                        
                        // Pega a raiz ("/") do ZIP parcial
                        Path raizZipParcial = zipParcialFS.getPath("/");

                        // Varre todos os arquivos lá dentro usando Streams do Java 8+
                        Files.walk(raizZipParcial)
                             .filter(Files::isRegularFile)
                             .filter(p -> p.toString().toLowerCase().endsWith(".xml"))
                             .forEach(arquivoXml -> {
                                 try {
                                     // Pega apenas o nome do arquivo (ex: nota.xml)
                                     String nomeArquivo = arquivoXml.getFileName().toString();
                                     
                                     // Define o destino na raiz do ZIP final
                                     Path destinoXml = zipFinalFS.getPath("/" + nomeArquivo);
                                     
                                     // Copia de um ZIP para o outro, se ainda não existir
                                     if (Files.notExists(destinoXml)) {
                                         Files.copy(arquivoXml, destinoXml);
                                     }
                                 } catch (IOException e) {
                                     System.err.println("Erro ao copiar XML: " + e.getMessage());
                                 }
                             });
                    }
                    // Opcional: Apaga o ZIP parcial após extrair os dados
                    Files.deleteIfExists(zipParcial);
                }
            }
        }
        System.out.println("Unificação concluída! ZIP final gerado em: " + arquivoZipFinal.toAbsolutePath());
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

    private class MenuItem {
        @SuppressWarnings("FieldMayBeFinal")
        private String modelo;
        @SuppressWarnings("FieldMayBeFinal")
        private String emitidasPeriodoDe;
        @SuppressWarnings("FieldMayBeFinal")
        private String emitidasPeriodoAte;
        @SuppressWarnings("FieldMayBeFinal")
        private String origemNFe;
        @SuppressWarnings("FieldMayBeFinal")
        private String situacaoNFe;

        public MenuItem(String modelo, String emitidasPeriodoDe, String emitidasPeriodoAte, String origemNFe, String situacaoNFe) {
            this.modelo = modelo;
            this.emitidasPeriodoDe = emitidasPeriodoDe;
            this.emitidasPeriodoAte = emitidasPeriodoAte;
            this.origemNFe = origemNFe;
            this.situacaoNFe = situacaoNFe;
        }

        public String getModelo() {
            return switch (this.modelo) {
                case "55" -> "NF-e";
                case "65" -> "NFC-e";
                case "57" -> "CT-e";
                default -> "Modelo_invalido";
            };
        }

        public String getEmitidasPeriodoDe() {
            // Substitui as barras por traços para não quebrar o caminho do arquivo no Windows/Linux
            return this.emitidasPeriodoDe.replace("/", "-");
        }

        public String getEmitidasPeriodoAte() {
            return this.emitidasPeriodoAte.replace("/", "-");
        }

        public String getOrigemNFe() {
            return switch (this.origemNFe) {
                case "EMITIDAS" -> "Emitidas";
                case "RECEBIDAS" -> "Recebidas";
                default -> "Origem_invalida";
            };
        }

        public String getSituacaoNFe() {
            return switch (this.situacaoNFe) {
                case "AUTORIZADAS" -> "Autorizadas";
                case "CANCELADAS" -> "Canceladas";
                case "TODAS" -> "Todas";
                default -> "Situacao_invalida";
            };
        }
    }
}