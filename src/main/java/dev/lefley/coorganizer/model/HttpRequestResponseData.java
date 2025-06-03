package dev.lefley.coorganizer.model;

public class HttpRequestResponseData {
    public final HttpRequestData request;
    public final HttpResponseData response;
    public final AnnotationData annotations;
    
    public HttpRequestResponseData(HttpRequestData request, HttpResponseData response, AnnotationData annotations) {
        this.request = request;
        this.response = response;
        this.annotations = annotations;
    }
    
    // Backwards compatibility constructor
    public HttpRequestResponseData(HttpRequestData request, HttpResponseData response) {
        this.request = request;
        this.response = response;
        this.annotations = null;
    }
}