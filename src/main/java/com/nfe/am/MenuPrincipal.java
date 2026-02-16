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
    private Scanner scanner;

    public MenuPrincipal(WebScrapping web, Scanner scanner) {
        this.web = web;
        this.scanner = scanner;
    }

  

    /**
     * Exibe o menu de opções da página principal.
     *
     */
    public void exibirMenu(Document documento) {
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
               
            } catch (Exception e) {
                System.out.println("Erro ao acessar opção: " + e.getMessage());
            }
        }
    }

    /**
     * Solicita que o usuário escolha uma opção a partir do console.
     */
    private int obterEscolhaDoUsuario(int totalLinks) {
        System.out.print("Escolha uma opção (digite o número): ");
        int escolha = -1;
        if (scanner.hasNextInt()) {
            escolha = scanner.nextInt();
            // Consumir o \n que sobra
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

    public WebScrapping getWeb() {
        return web;
    }

    public void setWeb(WebScrapping web) {
        this.web = web;
    }
}
