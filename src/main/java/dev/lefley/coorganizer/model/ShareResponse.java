package dev.lefley.coorganizer.model;

public class ShareResponse {
    private String url;
    private String error;
    
    public ShareResponse() {
    }
    
    public ShareResponse(String url, String error) {
        this.url = url;
        this.error = error;
    }
    
    public String getUrl() {
        return url;
    }
    
    public void setUrl(String url) {
        this.url = url;
    }
    
    public String getError() {
        return error;
    }
    
    public void setError(String error) {
        this.error = error;
    }
    
    public boolean hasUrl() {
        return url != null && !url.trim().isEmpty();
    }
    
    public boolean hasError() {
        return error != null && !error.trim().isEmpty();
    }
}