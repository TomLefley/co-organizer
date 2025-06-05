package dev.lefley.coorganizer.matcher;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.HttpService;
import burp.api.montoya.proxy.http.InterceptedResponse;
import dev.lefley.coorganizer.config.ServerConfiguration;
import dev.lefley.coorganizer.util.Logger;

public class SharedItemDownloadMatcher {
    // Server configuration - see ServerConfiguration class to change server address
    private static final String TARGET_HOST = ServerConfiguration.HOST;
    private static final int TARGET_PORT = ServerConfiguration.PORT;
    private static final String TARGET_PATH_SUFFIX = ServerConfiguration.IMPORT_ENDPOINT_SUFFIX;
    
    private final MontoyaApi api;
    private final Logger logger;
    
    public SharedItemDownloadMatcher(MontoyaApi api) {
        this.api = api;
        this.logger = new Logger(api, SharedItemDownloadMatcher.class);
    }
    
    public boolean matches(InterceptedResponse response) {
        HttpService httpService = response.initiatingRequest().httpService();
        String path = response.initiatingRequest().path();
        
        return TARGET_HOST.equals(httpService.host()) && 
               httpService.port() == TARGET_PORT && 
               path != null && 
               path.endsWith(TARGET_PATH_SUFFIX);
    }
}