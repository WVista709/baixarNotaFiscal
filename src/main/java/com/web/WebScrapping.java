package com.web;

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

    public WebScrapping(CertificadoEscolhido certificadoEscolhido) throws Exception {
        this.certificadoEscolhido = certificadoEscolhido;
        this.kmf = kmf();
        this.sslContext = sslContext();
        this.cookieManager = cookieManager();
        inicializarCliente();
    }

    /**
     * Inicializa o cliente HTTP
     */
    private void inicializarCliente() throws Exception{
        this.cliente = HttpClient.newBuilder()
            .sslContext(sslContext)
            .cookieHandler(cookieManager)
            .followRedirects(Redirect.ALWAYS)
            .build();
    }

    /**
     * Método genérico para fazer GET e retornar o HTML já parseado pelo Jsoup
     */
    public Document getPagina(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0.0.0 Safari/537.36")
                .GET()
                .build();

        HttpResponse<String> response = cliente.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 400) {
            throw new Exception("Erro HTTP " + response.statusCode() + " ao acessar " + url);
        }

        // Converte a String HTML em Objeto Jsoup para facilitar a leitura
        return Jsoup.parse(response.body());
    }

    /**
     * Método abstrato para executar a rotina de scraping
     */
    public abstract void acessarSite() throws Exception;

    private KeyManagerFactory kmf() throws Exception{
        this.kmf = KeyManagerFactory.getInstance("SunX509");
        this.kmf.init(certificadoEscolhido.getKeyStore(), null);
        return this.kmf;
    }

    private SSLContext sslContext() throws Exception{
        X509KeyManager x509KeyManager = null;
        for (javax.net.ssl.KeyManager km : this.kmf.getKeyManagers()) {
            if (km instanceof X509KeyManager x509KeyManager1) {
                x509KeyManager = x509KeyManager1;
                break;
            }
        }

        if (x509KeyManager == null) {
            throw new Exception("Gerenciador de certificados X509 não encontrado.");
        }

        SelecionadorDeCertificado seletorFixo = new SelecionadorDeCertificado(
            x509KeyManager, 
            certificadoEscolhido.getAlias() 
        );

        this.sslContext = SSLContext.getInstance("TLSv1.2");
        this.sslContext.init(new KeyManager[] { seletorFixo }, null, null);
        return this.sslContext;
    }

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
