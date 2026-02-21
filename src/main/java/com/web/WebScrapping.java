package com.web;

import java.io.FileWriter;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.X509KeyManager;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import com.certificado.CertificadoEscolhido;
import com.certificado.SelecionadorDeCertificado;

public abstract class WebScrapping {
    protected CertificadoEscolhido certificadoEscolhido;
    protected HttpClient cliente;
    protected KeyManagerFactory kmf;
    protected SSLContext sslContext;
    protected CookieManager cookieManager;

    /*
     * Construtor que recebe o certificado escolhido e inicializa o cliente HTTP.
     */
    public WebScrapping(CertificadoEscolhido certificadoEscolhido) throws Exception {
        this.certificadoEscolhido = certificadoEscolhido;
        this.kmf = kmf();
        this.sslContext = sslContext();
        this.cookieManager = cookieManager();
        inicializarCliente();
    }

    /**
     * Construtor que recebe o certificado escolhido e o cookie manager existente caso seja necessário.
     */
    public WebScrapping(CertificadoEscolhido certificadoEscolhido, CookieManager cookieManagerExistente) throws Exception {
        this.certificadoEscolhido = certificadoEscolhido;
        this.kmf = kmf();
        this.sslContext = sslContext();
        this.cookieManager = cookieManagerExistente;
        inicializarCliente();
    }

    /**
     * Função que inicializa o cliente HTTP com o certificado escolhido.
     */
    private void inicializarCliente() throws Exception{
        this.cliente = HttpClient.newBuilder()
            .sslContext(sslContext)
            .cookieHandler(cookieManager)
            .followRedirects(Redirect.ALWAYS)
            .build();
    }

    /**
     * Função abstrata para executar a rotina de scraping.
     */
    public abstract void acessarSite() throws Exception;

    /**
     * Função que gera um arquivo TXT com o conteúdo do documento HTML.
     */
    public void gerarTXT(Document documento, String nomeArquivo) throws Exception {
        String mensagem = "ERRO no gerarTXT: ";
        if (nomeArquivo.isEmpty()) {
            throw new Exception(mensagem + "Nome do arquivo não pode ser vazio");
        }

        if (!nomeArquivo.endsWith(".txt")) {
            nomeArquivo = nomeArquivo + ".txt";
        }

        try (FileWriter writer = new FileWriter(nomeArquivo)) {
            writer.write(documento.html());
        } catch (Exception e) {
            throw new Exception(mensagem + "Erro ao gerar arquivo " + nomeArquivo + ": " + e.getMessage());
        }
    }   

    /**
     * Função genérica para fazer GET e retornar o HTML já parseado pelo Jsoup.
     */
    public Document getPagina(String url) throws Exception {
        String mensagem = "ERRO no getPagina: ";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0.0.0 Safari/537.36")
                .GET()
                .build();

        HttpResponse<String> response = cliente.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 400) {
            throw new Exception(mensagem + "Erro HTTP " + response.statusCode() + " ao acessar " + url);
        }

        // Converte a String HTML em Objeto Jsoup para facilitar a leitura
        return Jsoup.parse(response.body());
    }

    /**
     * Método genérico para fazer POST e retornar o HTML já parseado pelo Jsoup
     */
    public Document postPagina(String url, String parametros) throws Exception {
        String mensagem = "ERRO no postPagina: ";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0.0.0 Safari/537.36")
                .POST(HttpRequest.BodyPublishers.ofString(parametros))
                .build();

        HttpResponse<String> response = cliente.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 400) {
            throw new Exception(mensagem + "Erro HTTP " + response.statusCode() + " ao acessar " + url);
        }

        return Jsoup.parse(response.body());
    }

    /**
     * Função que inicializa o KeyManagerFactory e garante a escolha do certificado escolhido.
     */
    private KeyManagerFactory kmf() throws Exception{
        this.kmf = KeyManagerFactory.getInstance("SunX509");
        this.kmf.init(certificadoEscolhido.getKeyStore(), null);
        return this.kmf;
    }

    /**
     * Função que inicializa o SSLContext e mantém a sessão do certificado escolhido.
     */
    private SSLContext sslContext() throws Exception{
        String mensagem = "ERRO no sslContext: ";
        X509KeyManager x509KeyManager = null;
        for (javax.net.ssl.KeyManager km : this.kmf.getKeyManagers()) {
            if (km instanceof X509KeyManager x509KeyManager1) {
                x509KeyManager = x509KeyManager1;
                break;
            }
        }

        if (x509KeyManager == null) {
            throw new Exception("ERRO no sslContext: Gerenciador de certificados X509 não encontrado.");
        } else {
            System.out.println(mensagem + "Gerenciador de certificados X509 encontrado.");
        }

        SelecionadorDeCertificado seletorFixo = new SelecionadorDeCertificado(
            x509KeyManager, 
            certificadoEscolhido.getAlias() 
        );

        this.sslContext = SSLContext.getInstance("TLSv1.2");
        this.sslContext.init(new KeyManager[] { seletorFixo }, null, null);
        return this.sslContext;
    }

    /**
     * Função que inicializa o CookieManager e garante que o http cliente mantenha a sessão do certificado escolhido.
     */
    private CookieManager cookieManager() throws Exception{
        this.cookieManager = new CookieManager();
        this.cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        return this.cookieManager;
    }

    public CertificadoEscolhido getCertificadoEscolhido() {
        return certificadoEscolhido;
    }

    public KeyManagerFactory getKmf() {
        return kmf;
    }

    public SSLContext getSslContext() {
        return sslContext;
    }

    public CookieManager getCookieManager() {
        return cookieManager;
    }
}
