package dev.lefley.coorganizer.matcher;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.HttpService;
import burp.api.montoya.proxy.http.InterceptedResponse;
import dev.lefley.coorganizer.util.Logger;

public class SharedItemDownloadMatcher {
    private static final String TARGET_HOST = "localhost";
    private static final int TARGET_PORT = 3000;
    private static final String TARGET_PATH_SUFFIX = "/import";
    
    private final MontoyaApi api;
    private final Logger logger;
    
    public SharedItemDownloadMatcher(MontoyaApi api) {
        this.api = api;
        this.logger = new Logger(api, SharedItemDownloadMatcher.class);
    }
    
    public boolean matches(InterceptedResponse response) {
        HttpService httpService = response.initiatingRequest().httpService();
        String path = response.initiatingRequest().path();
        
        // Debug logging
        logger.trace("Host: '" + httpService.host() + "'");
        logger.trace("Port: " + httpService.port());
        logger.trace("Path: '" + path + "'");
        logger.trace("Path ends with '" + TARGET_PATH_SUFFIX + "': " + (path != null && path.endsWith(TARGET_PATH_SUFFIX)));
        
        boolean matches = TARGET_HOST.equals(httpService.host()) && 
                         httpService.port() == TARGET_PORT && 
                         path != null && 
                         path.endsWith(TARGET_PATH_SUFFIX);
        
        logger.trace("Overall match result: " + matches);
        
        return matches;
    }
}