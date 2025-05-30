package dev.lefley.coorganizer.matcher;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.HttpService;
import burp.api.montoya.proxy.http.InterceptedResponse;

public class SharedItemDownloadMatcher {
    private final MontoyaApi api;
    
    public SharedItemDownloadMatcher(MontoyaApi api) {
        this.api = api;
    }
    
    public boolean matches(InterceptedResponse response) {
        HttpService httpService = response.initiatingRequest().httpService();
        String path = response.initiatingRequest().path();
        
        // Debug logging
        api.logging().logToOutput("Debug - Host: '" + httpService.host() + "'");
        api.logging().logToOutput("Debug - Port: " + httpService.port());
        api.logging().logToOutput("Debug - Path: '" + path + "'");
        api.logging().logToOutput("Debug - Path ends with '/download': " + (path != null && path.endsWith("/download")));
        
        boolean matches = "localhost".equals(httpService.host()) && 
                         httpService.port() == 3000 && 
                         path != null && 
                         path.endsWith("/download");
        
        api.logging().logToOutput("Debug - Overall match result: " + matches);
        
        return matches;
    }
}