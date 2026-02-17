package com.nfe.am;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.certificado.CertificadoEscolhido;
import com.web.WebScrapping;

public class SiteSefazAM extends WebScrapping {

    private static final String BASE_URL = "https://online.sefaz.am.gov.br/dte/";

    @SuppressWarnings("FieldMayBeFinal")
    private Scanner scanner;

    public SiteSefazAM(CertificadoEscolhido certificadoEscolhido) throws Exception {
        super(certificadoEscolhido);
        this.scanner = new Scanner(System.in);
    }

    public Scanner getScanner() {
        return scanner;
    }

    /**
     * Acessa o site da Sefaz AM e executa a rotina de scraping
     */
    @Override
    public void acessarSite() throws Exception {
        System.out.println("---- Iniciando rotina do site Sefaz AM ----");
        String url = BASE_URL + "loginSSL.asp";
        Document documento = getPagina(url);
        
        // Verifica se caiu na tela de seleção de filiais
        Elements linhasFiliais = documento.select("table.dg_table tbody tr");
        
        if (!linhasFiliais.isEmpty()) {
            System.out.println("Seleção de filiais detectada.");
            processarFiliais(documento);
        } else {
            System.out.println("Login direto efetuado (sem seleção de filiais).");
            System.out.println("Título da página: " + documento.title());
            exibirMenu(documento);
        }
    }

    /**
     * Exibe o menu de opções da página principal.
     *
     */
    public Integer exibirMenu(Document documento) {
        System.out.println("---- Página Principal Carregada ----");

        Elements itensMenu = documento.select("div.menuDte_itemMenu");
        System.out.println("Encontradas " + itensMenu.size() + " opções de menu.");

        if (itensMenu.isEmpty()) {
            System.out.println("Nenhum item de menu encontrado.");
            return null;
        }

        List<String> titulos = new ArrayList<>();
        List<String> links = new ArrayList<>();
        List<String> idLinks = new ArrayList<>();

        System.out.println("Opções disponíveis (idlink - Título):");
        for (Element item : itensMenu) {
            String titulo = item.select("div.menuDte_itemMenu_titulo").text();
            String idLink = item.attr("idlink");
            String href = item.attr("href");

            if (!idLink.isEmpty()) {
                links.add("https://online.sefaz.am.gov.br/redirect2.asp?id=" + idLink);
                titulos.add(titulo);
                idLinks.add(idLink);
                System.out.println(idLink + " - " + titulo);
            } else if (!href.isEmpty()) {
                // Para as opções sem idlink, gere um id "pseudo" para permitir seleção, se necessário
                // Se não quiser permitir seleção sem idlink, apenas ignore este bloco
                if (!href.startsWith("http")) {
                    href = BASE_URL + href;
                }
                links.add(href);
                titulos.add(titulo);
                idLinks.add(""); // idlink vazio para manter indices alinhados
                System.out.println("(sem idlink) - " + titulo);
            }
        }

        System.out.print("Escolha uma opção informando o idlink: ");
        String escolhaIdLink = scanner.nextLine().trim();
        int idxEscolhido = -1;

        // Encontrar pelo idlink informado
        for (int i = 0; i < idLinks.size(); i++) {
            if (escolhaIdLink.equals(idLinks.get(i))) {
                idxEscolhido = i;
                break;
            }
        }

        if (idxEscolhido != -1) {
            String urlEscolhida = links.get(idxEscolhido);
            System.out.println("Navegando para: " + titulos.get(idxEscolhido));
            try {
                Document proximaPagina = getPagina(urlEscolhida);
                System.out.println("Página acessada: " + proximaPagina.title());
                return idxEscolhido;
            } catch (Exception e) {
                System.out.println("Erro ao acessar opção: " + e.getMessage());
                return null;
            }
        } else {
            System.out.println("idlink não encontrado entre as opções.");
            return null;
        }
    }

    /**
     * Processa a listagem de filiais (inscrições) encontradas na página.
     */
    private void processarFiliais(Document documento) {
        Elements linhasFiliais = documento.select("table.dg_table tbody tr");
        int qtdInscricoes = linhasFiliais.size();
        System.out.println("Encontradas " + qtdInscricoes + " inscrições.");

        List<String> links = new ArrayList<>();
        int index = 1;
        for (Element linha : linhasFiliais) {
            Elements colunas = linha.select("td");
            if (colunas.size() > 2) {
                // Coluna 1: CNPJ
                String cnpj = colunas.get(1).text();
                // Coluna 2: Link (Inscrição Estadual)
                Element linkElement = colunas.get(2).selectFirst("a");
                
                String situacao = "";
                // Coluna 3: Situação (se existir)
                if (colunas.size() > 3) {
                    situacao = colunas.get(3).text();
                }

                if (linkElement != null) {
                    String inscricaoEstadual = linkElement.text();
                    String href = linkElement.attr("href");
                    links.add(href);
                    
                    StringBuilder sb = new StringBuilder();
                    sb.append(index).append(" - ");
                    sb.append("Inscrição: ").append(inscricaoEstadual).append(" | ");
                    sb.append("CNPJ: ").append(cnpj);
                    if (!situacao.isEmpty()) {
                        sb.append(" | Situação: ").append(situacao);
                    }
                    
                    System.out.println(sb.toString());
                    index++;
                }
            }
        }

        if (links.isEmpty()) {
            System.out.println("Nenhum link de inscrição encontrado.");
            return;
        }

        int escolha = obterEscolhaDoUsuario(links.size());
        if (escolha == -1) {
            System.out.println("Entrada inválida. Por favor, digite um número.");
            return;
        }
        navegarParaLinkEscolhido(links, escolha);
    }

    /**
     * Solicita que o usuário escolha uma inscrição a partir do console.
     */
    private int obterEscolhaDoUsuario(int totalLinks) {
        System.out.print("Escolha uma opção (digite o número): ");
        int escolha = -1;
        if (scanner.hasNextInt()) {
            escolha = scanner.nextInt();
            scanner.nextLine(); 
            if (escolha <= 0 || escolha > totalLinks) {
                System.out.println("Opção inválida.");
                escolha = -1;
            }
        } else {
            scanner.nextLine();
        }
        return escolha;
    }

    /**
     * Faz a navegação para a URL escolhida pelo usuário.
     */
    private void navegarParaLinkEscolhido(List<String> links, int escolha) {
        String urlEscolhida = links.get(escolha - 1);
        if (!urlEscolhida.startsWith("http")) {
            urlEscolhida = BASE_URL + urlEscolhida;
        }
        System.out.println("Navegando para: " + urlEscolhida);

        try {
            Document proximaPagina = getPagina(urlEscolhida);
            System.out.println("Página acessada com sucesso!");
            System.out.println("Título da nova página: " + proximaPagina.title());
            
            // Exibe o menu de opções da página principal
            exibirMenu(proximaPagina);
            
        } catch (Exception e) {
            System.out.println("Erro ao acessar a página: " + e.getMessage());
        }
    }
}
