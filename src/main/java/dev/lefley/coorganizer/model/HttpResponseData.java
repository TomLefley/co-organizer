package dev.lefley.coorganizer.model;

public class HttpResponseData {
    public final String statusCode;
    public final String headers;
    public final String body;
    
    public HttpResponseData(String statusCode, String headers, String body) {
        this.statusCode = statusCode;
        this.headers = headers;
        this.body = body;
    }
}