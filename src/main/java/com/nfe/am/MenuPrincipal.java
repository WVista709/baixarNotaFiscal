package com.nfe.am;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.web.WebScrapping;

public class MenuPrincipal {

    private static final String BASE_URL = "https://online.sefaz.am.gov.br/dte/";
    private WebScrapping web;

    public MenuPrincipal(WebScrapping web) {
        this.web = web;
    }

    public void processar(Document documento) {
        verificarPagina(documento);
    }

    /**
     * Verifica se a página contém a consulta de arquivos XML e, se sim, processa a consulta. <br>
     * Caso contrário, exibe o menu de opções da página principal.
     */
    private void verificarPagina(Document documento) {
        if (documento.text().contains("CONSULTA DE ARQUIVOS XML")) {
            new ConsultaXml(web).processar(documento);
        } else {
            exibirMenu(documento);
        }
    }

    /**
     * Exibe o menu de opções da página principal.
     *
     */
    private void exibirMenu(Document documento) {
        System.out.println("---- Página Principal Carregada ----");
        
        Elements itensMenu = documento.select("div.menuDte_itemMenu");
        System.out.println("Encontradas " + itensMenu.size() + " opções de menu.");
        
        if (itensMenu.isEmpty()) {
            System.out.println("Nenhum item de menu encontrado.");
            return;
        }

        List<String> titulos = new ArrayList<>();
        List<String> links = new ArrayList<>();
        int index = 1;

        for (Element item : itensMenu) {
            String titulo = item.select("div.menuDte_itemMenu_titulo").text();
            String idLink = item.attr("idlink");
            String href = item.attr("href");
            
            if (!idLink.isEmpty()) {
                links.add("https://online.sefaz.am.gov.br/redirect2.asp?id=" + idLink);
                titulos.add(titulo);
                System.out.println(index + " - " + titulo);
                index++;
            } else if (!href.isEmpty()) {
                if (!href.startsWith("http")) {
                    href = BASE_URL + href;
                }
                links.add(href);
                titulos.add(titulo);
                System.out.println(index + " - " + titulo);
                index++;
            }
        }
        
        int escolha = obterEscolhaDoUsuario(links.size());
        if (escolha != -1) {
            String urlEscolhida = links.get(escolha - 1);
            System.out.println("Navegando para: " + titulos.get(escolha - 1));
            
            try {
               Document proximaPagina = web.getPagina(urlEscolhida);
               System.out.println("Página acessada: " + proximaPagina.title());
               
               verificarPagina(proximaPagina);
               
            } catch (Exception e) {
                System.out.println("Erro ao acessar opção: " + e.getMessage());
            }
        }
    }

    /**
     * Solicita que o usuário escolha uma opção a partir do console.
     */
    private int obterEscolhaDoUsuario(int totalLinks) {
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.print("Escolha uma opção (digite o número): ");
            int escolha = -1;
            if (scanner.hasNextInt()) {
                escolha = scanner.nextInt();
                if (escolha <= 0 || escolha > totalLinks) {
                    System.out.println("Opção inválida.");
                    escolha = -1;
                }
            }
            return escolha;
        }
    }

    public WebScrapping getWeb() {
        return web;
    }

    public void setWeb(WebScrapping web) {
        this.web = web;
    }
}
