package dev.lefley.coorganizer.model;

public class HttpRequestData {
    public final String method;
    public final String url;
    public final String headers;
    public final String body;
    
    public HttpRequestData(String method, String url, String headers, String body) {
        this.method = method;
        this.url = url;
        this.headers = headers;
        this.body = body;
    }
}