package com.nfe.am;

import java.util.Scanner;

import org.jsoup.nodes.Document;

import com.web.WebScrapping;

public class ConsultaXml {

    private WebScrapping web;

    public ConsultaXml(WebScrapping web) {
        this.web = web;
    }

    public void processar(Document documento) {
        System.out.println("---- Tela de Consulta de XML Detectada ----");
        try (Scanner scanner = new Scanner(System.in)) {
        
            System.out.println("Preencha os dados para consulta:");
            
            System.out.print("Data Inicial (dd/MM/yyyy): ");
            String dataInicial = scanner.nextLine();
            
            System.out.print("Data Final (dd/MM/yyyy): ");
            String dataFinal = scanner.nextLine();
            
            System.out.println("Modelo:");
            System.out.println("1 - NF-e (55)");
            System.out.println("2 - NFC-e (65)");
            System.out.println("3 - CT-e (57)");
            System.out.print("Escolha o modelo: ");
            int modeloOpcao = scanner.nextInt();
            String modelo = "55";
            if (modeloOpcao == 2) modelo = "65";
            else if (modeloOpcao == 3) modelo = "57";
            
            System.out.println("Origem:");
            System.out.println("1 - Emitidas");
            System.out.println("2 - Recebidas");
            System.out.print("Escolha a origem: ");
            int origemOpcao = scanner.nextInt();
            String origem = "EMITIDAS";
            if (origemOpcao == 2) origem = "RECEBIDAS";

            // Aqui você implementaria o POST para "exibirListaXML.do?acao=submitConsultaXML"
            System.out.println("Dados coletados: " + dataInicial + " a " + dataFinal + " | Modelo: " + modelo + " | Origem: " + origem);
            System.out.println("Funcionalidade de envio do formulário será implementada a seguir.");
        }
    }

    public WebScrapping getWeb() {
        return web;
    }

    public void setWeb(WebScrapping web) {
        this.web = web;
    }
}
