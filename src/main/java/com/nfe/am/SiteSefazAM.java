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

    /**
     * Construtor que recebe o certificado escolhido e inicializa o scanner para leitura de dados do usuário.
     */
    public SiteSefazAM(CertificadoEscolhido certificadoEscolhido) throws Exception {
        super(certificadoEscolhido);
        this.scanner = new Scanner(System.in);
    }

    public Scanner getScanner() {
        return scanner;
    }

    /**
     * Função que acessa o site da Sefaz AM e lida com a seleção de filiais e o login.
     */
    @Override
    public void acessarSite() throws Exception {
        System.out.println("---- Iniciando rotina do site Sefaz AM ----");
        String urlDeLogin = BASE_URL + "loginSSL.asp";
        Document paginaAtual = getPagina(urlDeLogin);

        //Verifica se há filiais para seleção
        String linhaDeSelecaoDeFiliais = "table.dg_table tbody tr";
        Boolean temFiliais = !paginaAtual.select(linhaDeSelecaoDeFiliais).isEmpty();
        if (temFiliais) {
            System.out.println("Seleção de filiais detectada.");
            paginaAtual = processarFiliais(paginaAtual);
        } else {
            System.out.println("Login direto efetuado (sem seleção de filiais).");
        }

        //Exibe o menu de opções se a página atual não for nula
        if (paginaAtual != null) {
            //gerarTXT(paginaAtual, "paginaAtual.txt");
            exibirMenu(paginaAtual);
        }
    }

    /**
     * Função que exibe o menu de opções da página principal.
     */
    private void exibirMenu(Document documento) throws Exception {
        System.out.println("---- Página Principal Carregada ----");

        //Extrai os itens do menu da página
        List<MenuItem> itens = extrairItensMenu(documento);
        
        //Verifica se há itens de menu
        if (itens.isEmpty()) {
            System.out.println("Nenhum item de menu encontrado.");
        }
    
        //Imprime as opções do menu
        imprimirOpcoes(itens);
    
        //Solicita ao usuário que escolha uma opção
        System.out.print("Escolha uma opção informando o idlink: ");
        String escolhaIdLink = scanner.nextLine().trim();

        //Processa a escolha do usuário
        processarEscolha(itens, escolhaIdLink);

        //Executa a ação escolhida pelo usuário
        switch (escolhaIdLink) {
            //Caso a opção escolhida seja a de baixar as NF-e, NFC-e e CT-e
            case "42" -> {
                BaixarNFEAM baixarNFEAM = new BaixarNFEAM(certificadoEscolhido, getCookieManager());
                baixarNFEAM.acessarSite();
            }
        }
    }
    
    // --- Funções Auxiliares para extrair itens do exibirMenu ---
    /**
     * Função que extrai os itens do menu da página e retorna uma lista de MenuItem.
     */
    private List<MenuItem> extrairItensMenu(Document documento) {
        Elements elements = documento.select("div.menuDte_itemMenu");
        List<MenuItem> itens = new ArrayList<>();
    
        for (Element el : elements) {
            String titulo = el.select("div.menuDte_itemMenu_titulo").text();
            String idLink = el.attr("idlink");
            String href = el.attr("href");
            String urlFinal = "";
    
            if (!idLink.isEmpty()) {
                urlFinal = "https://online.sefaz.am.gov.br/redirect2.asp?id=" + idLink;
            } else if (!href.isEmpty()) {
                urlFinal = href.startsWith("http") ? href : BASE_URL + href;
            }
    
            if (!urlFinal.isEmpty()) {
                itens.add(new MenuItem(titulo, urlFinal, idLink));
            }
        }
        return itens;
    }
    
    /**
     * Imprime as opções do menu.
     */
    private void imprimirOpcoes(List<MenuItem> itens) {
        System.out.println("Opções disponíveis (idlink - Título):");
        for (MenuItem item : itens) {
            String label = item.idLink.isEmpty() ? "(sem idlink)" : item.idLink;
            System.out.println(label + " - " + item.titulo);
        }
    }
    
    /**
     * Processa a escolha do usuário.
     */
    private Document processarEscolha(List<MenuItem> itens, String idEscolhido) {
        for (int i = 0; i < itens.size(); i++) {
            MenuItem item = itens.get(i);
            if (idEscolhido.equals(item.idLink)) {
                return realizarNavegacaoSegura(item.url, item.titulo);
            }
        }
        System.out.println("idlink não encontrado.");
        return null;
    }
    
    /**
     * Processa a listagem de filiais (inscrições) encontradas na página.
     */
    private Document processarFiliais(Document documento) {
        Elements linhasFiliais = documento.select("table.dg_table tbody tr");
        int qtdInscricoes = linhasFiliais.size();
        System.out.println("Encontradas " + qtdInscricoes + " inscrições.");

        List<String> links = new ArrayList<>();
        int index = 1;
        for (Element linha : linhasFiliais) {
            Elements colunas = linha.select("td");
            if (colunas.size() > 2) {
                
                String cnpj = colunas.get(1).text();
                // Coluna 2: Link (Inscrição Estadual)
                Element linkInscricaoEstadual = colunas.get(2).selectFirst("a");
                
                String situacao = "";
                // Coluna 3: Situação (se existir)
                if (colunas.size() > 3) {
                    situacao = colunas.get(3).text();
                }

                if (linkInscricaoEstadual != null) {
                    String inscricaoEstadual = linkInscricaoEstadual.text();
                    String href = linkInscricaoEstadual.attr("href");
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
            System.out.println("Nenhum link de inscrição estadual encontrado.");
            return null;
        }

        int escolha = obterEscolhaDoUsuario(links.size());
        if (escolha == -1) {
            System.out.println("Entrada inválida. Por favor, digite um número.");
            return null;
        }

        //Realiza a navegação para a URL escolhida pelo usuário
        return realizarNavegacaoSegura(links.get(escolha - 1), "Inscrição " + escolha);
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
    private Document realizarNavegacaoSegura(String url, String nomeDestino) {
        try {
            String urlFormatada = url.startsWith("http") ? url : BASE_URL + url;
            System.out.println("Navegando para: " + nomeDestino);
            Document doc = getPagina(urlFormatada);
            System.out.println("Sucesso: " + doc.title());
            return doc;
        } catch (Exception e) {
            System.err.println("Erro ao acessar [" + nomeDestino + "]: " + e.getMessage());
            return null;
        }
    }

    /**
     * Classe que representa um item do menu.
     */
    private class MenuItem {
        String titulo;
        String url;
        String idLink;
    
        public MenuItem(String titulo, String url, String idLink) {
            this.titulo = titulo;
            this.url = url;
            this.idLink = idLink;
        }
    }
}