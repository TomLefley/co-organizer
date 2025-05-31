package dev.lefley.coorganizer.matcher;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.HttpService;
import burp.api.montoya.proxy.http.InterceptedResponse;

public class SharedItemDownloadMatcher {
    private static final String TARGET_HOST = "localhost";
    private static final int TARGET_PORT = 3000;
    private static final String TARGET_PATH_SUFFIX = "/download";
    
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
        api.logging().logToOutput("Debug - Path ends with '" + TARGET_PATH_SUFFIX + "': " + (path != null && path.endsWith(TARGET_PATH_SUFFIX)));
        
        boolean matches = TARGET_HOST.equals(httpService.host()) && 
                         httpService.port() == TARGET_PORT && 
                         path != null && 
                         path.endsWith(TARGET_PATH_SUFFIX);
        
        api.logging().logToOutput("Debug - Overall match result: " + matches);
        
        return matches;
    }
}