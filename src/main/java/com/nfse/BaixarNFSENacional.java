package com.nfse;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.certificado.CertificadoEscolhido;
import com.web.WebScrapping;

public class BaixarNFSENacional extends WebScrapping {
    
    private static final String URL_BASE = "https://www.nfse.gov.br";
    
    @SuppressWarnings("FieldMayBeFinal")
    private Scanner scanner;

    public BaixarNFSENacional(CertificadoEscolhido certificadoEscolhido) throws Exception {
        super(certificadoEscolhido, HttpClient.Version.HTTP_1_1);
        this.scanner = new Scanner(System.in);
    }

    @Override
    public void acessarSite() throws Exception {
        getPagina(URL_BASE + "/EmissorNacional/Certificado");

        // 2. Inicia o loop do menu do terminal
        while (true) {
            menuPrincipal();
            System.out.print("Escolha uma opção: ");
            
            int opcao = lerInteiro();
            if (opcao == 0) {
                System.out.println("Encerrando módulo NFS-e Nacional...");
                break;
            }
            if (opcao < 1 || opcao > 3) {
                System.out.println("Opção inválida!");
                continue;
            }

            System.out.println("\n--- PERÍODO DA CONSULTA ---");
            System.out.print("Digite a data de INÍCIO (ex: 01/02/2026): ");
            String dataInicio = scanner.nextLine().trim();
            
            System.out.print("Digite a data de FIM (ex: 28/02/2026): ");
            String dataFim = scanner.nextLine().trim();

            // Executa a busca baseada na opção escolhida
            if (opcao == 1 || opcao == 3) {
                consultarEBaixar("Emitidas", dataInicio, dataFim);
            }
            
            if (opcao == 2 || opcao == 3) {
                consultarEBaixar("Recebidas", dataInicio, dataFim);
            }
        }
    }

    /**
     * Função que exibe o menu principal.
     */
    private void menuPrincipal() {
        System.out.println("\n==================================");
        System.out.println("     DOWNLOAD NFS-e NACIONAL      ");
        System.out.println("==================================");
        System.out.println("1 - Baixar Notas EMITIDAS");
        System.out.println("2 - Baixar Notas RECEBIDAS");
        System.out.println("3 - Baixar AMBAS (Emitidas e Recebidas)");
        System.out.println("0 - Voltar / Sair");
    }

    /**
     * Consulta as notas no portal Nacional e faz o download de XML e PDF lidando com a paginação JS
     */
    private void consultarEBaixar(String origem, String dataInicio, String dataFim) {
        System.out.printf("\nIniciando busca de NFS-e [%s] de %s a %s...\n", origem.toUpperCase(), dataInicio, dataFim);
        
        try {
            // Prepara a URL de busca codificando as datas
            String dataInicioEncoded = URLEncoder.encode(dataInicio, StandardCharsets.UTF_8);
            String dataFimEncoded = URLEncoder.encode(dataFim, StandardCharsets.UTF_8);
            
            int pagina = 1;
            int totalNotasBaixadas = 0;
            Path pastaDestino = null;
            
            // Variável para guardar a primeira chave da página e evitar loop infinito
            String primeiraChaveDaPaginaAnterior = "";

            while (true) {
                String urlConsulta;
                
                if (pagina == 1) {
                    urlConsulta = String.format("%s/EmissorNacional/Notas/%s?executar=1&busca=&datainicio=%s&datafim=%s", 
                            URL_BASE, origem, dataInicioEncoded, dataFimEncoded);
                } else {
                    // Para as páginas seguintes, injetamos os parâmetros mais comuns usados pelo Serpro/ASP.NET
                    urlConsulta = String.format("%s/EmissorNacional/Notas/%s?executar=1&busca=&datainicio=%s&datafim=%s&page=%d&p=%d&pg=%d", 
                            URL_BASE, origem, dataInicioEncoded, dataFimEncoded, pagina, pagina, pagina);
                }

                Document resultado = getPagina(urlConsulta);

                // Captura todos os links de XML na tabela DESTA página
                Elements linksDeDownloadXML = resultado.select("a[href*=/Download/NFSe/]");

                if (linksDeDownloadXML.isEmpty()) {
                    if (pagina == 1) {
                        System.out.println(">>> Nenhuma nota " + origem.toLowerCase() + " encontrada para este período.");
                    }
                    break; // Sai do laço se a página vier vazia
                }

                // SISTEMA ANTI-LOOP: Verifica se o servidor nos mandou a mesma página de novo
                String primeiraChaveAtual = linksDeDownloadXML.first().attr("href");
                if (primeiraChaveAtual.equals(primeiraChaveDaPaginaAnterior)) {
                    System.out.println(">>> Leitura concluída. Todas as páginas foram verificadas.");
                    break; // Encerra o laço!
                }
                primeiraChaveDaPaginaAnterior = primeiraChaveAtual;

                // Na primeira página, cria a pasta da empresa
                if (pagina == 1) {
                    Path pastaEmpresa = Path.of(certificadoEscolhido.getNomeEmpresa() + "-" + certificadoEscolhido.getCnpj());
                    pastaDestino = pastaEmpresa.resolve(origem.toLowerCase());
                    Files.createDirectories(pastaDestino);
                }

                System.out.println(">>> [Página " + pagina + "] Encontradas " + linksDeDownloadXML.size() + " notas. Baixando...");

                int contador = 1;
                for (Element linkXml : linksDeDownloadXML) {
                    String hrefXml = linkXml.attr("href");
                    String chaveAcesso = hrefXml.substring(hrefXml.lastIndexOf('/') + 1);
                    
                    String urlDownloadXml = URL_BASE + hrefXml;
                    String urlDownloadPdf = URL_BASE + "/EmissorNacional/Notas/Download/DANFSe/" + chaveAcesso;

                    Path arquivoXml = pastaDestino.resolve(chaveAcesso + "-nfse.xml");
                    Path arquivoPdf = pastaDestino.resolve(chaveAcesso + "-nfse.pdf");

                    System.out.printf("    [%d/%d] Salvando chave: %s...\n", contador, linksDeDownloadXML.size(), chaveAcesso);

                    // Baixa o XML se não existir
                    if (Files.notExists(arquivoXml)) {
                        byte[] bytesXml = getPaginaBytes(urlDownloadXml);
                        Files.write(arquivoXml, bytesXml);
                    }

                    // Baixa o PDF se não existir
                    if (Files.notExists(arquivoPdf)) {
                        byte[] bytesPdf = getPaginaBytes(urlDownloadPdf);
                        Files.write(arquivoPdf, bytesPdf);
                    }
                    
                    contador++;
                    totalNotasBaixadas++;
                }
                
                // Vai para a próxima página
                pagina++;
            }
            
            if (totalNotasBaixadas > 0) {
                System.out.println("\n>>> SUCESSO! Um total de " + totalNotasBaixadas + " notas foram salvas na pasta: " + pastaDestino.toAbsolutePath());
            }

        } catch (Exception e) {
            System.err.println(">>> Erro ao processar as notas " + origem + " na página atual: " + e.getMessage());
        }
    }

    /**
     * Método auxiliar para fazer requisições GET e retornar bytes puros (para baixar arquivos)
     */
    private byte[] getPaginaBytes(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .GET()
                .build();

        // O HttpClient "cliente" já possui a configuração do Certificado e os Cookies gravados!
        HttpResponse<byte[]> response = cliente.send(request, HttpResponse.BodyHandlers.ofByteArray());

        if (response.statusCode() >= 400) {
            throw new Exception("Falha no download (Erro " + response.statusCode() + "): " + url);
        }

        return response.body();
    }

    /**
     * Auxiliar para leitura segura de números no scanner
     */
    private int lerInteiro() {
        if (scanner.hasNextInt()) {
            int val = scanner.nextInt();
            scanner.nextLine();
            return val;
        } else {
            scanner.nextLine();
            return -1;
        }
    }
}