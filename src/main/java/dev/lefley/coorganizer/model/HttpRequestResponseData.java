package dev.lefley.coorganizer.model;

public class HttpRequestResponseData {
    public final HttpRequestData request;
    public final HttpResponseData response;
    
    public HttpRequestResponseData(HttpRequestData request, HttpResponseData response) {
        this.request = request;
        this.response = response;
    }
}