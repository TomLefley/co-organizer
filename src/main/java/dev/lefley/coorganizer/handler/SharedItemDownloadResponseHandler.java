package dev.lefley.coorganizer.handler;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.responses.HttpResponse;
import burp.api.montoya.proxy.http.InterceptedResponse;
import burp.api.montoya.proxy.http.ProxyResponseHandler;
import burp.api.montoya.proxy.http.ProxyResponseReceivedAction;
import burp.api.montoya.proxy.http.ProxyResponseToBeSentAction;
import dev.lefley.coorganizer.matcher.SharedItemDownloadMatcher;
import dev.lefley.coorganizer.serialization.HttpRequestResponseSerializer;

import java.util.Base64;
import java.util.List;

public class SharedItemDownloadResponseHandler implements ProxyResponseHandler {
    private final MontoyaApi api;
    private final SharedItemDownloadMatcher matcher;
    private final HttpRequestResponseSerializer serializer;
    
    public SharedItemDownloadResponseHandler(MontoyaApi api) {
        this.api = api;
        this.matcher = new SharedItemDownloadMatcher(api);
        this.serializer = new HttpRequestResponseSerializer(api);
    }
    
    @Override
    public ProxyResponseReceivedAction handleResponseReceived(InterceptedResponse interceptedResponse) {
        String url = interceptedResponse.initiatingRequest().url();
        api.logging().logToOutput("Proxy intercepted response from URL: " + url);
        
        if (matcher.matches(interceptedResponse)) {
            api.logging().logToOutput("Response matches shared item download pattern, processing...");
            
            // Process in background thread to avoid blocking proxy
            new Thread(() -> processDownload(interceptedResponse)).start();
            
            // Do not intercept, change status code to 201
            api.logging().logToOutput("Setting status code to 201 and not intercepting");
            HttpResponse modifiedResponse = interceptedResponse.withBody("[shared item imported]").withStatusCode((short) 201);
            return ProxyResponseReceivedAction.doNotIntercept(modifiedResponse);
            
        } else {
            api.logging().logToOutput("Response does not match shared item download pattern, continuing");
            return ProxyResponseReceivedAction.continueWith(interceptedResponse);
        }
    }
    
    @Override
    public ProxyResponseToBeSentAction handleResponseToBeSent(InterceptedResponse interceptedResponse) {
        return ProxyResponseToBeSentAction.continueWith(interceptedResponse);
    }
    
    private void processDownload(InterceptedResponse interceptedResponse) {
        try {
            String responseBody = interceptedResponse.bodyToString();
            api.logging().logToOutput("Response body length: " + responseBody.length());
            api.logging().logToOutput("Response body preview (first 200 chars): " + responseBody.substring(0, Math.min(200, responseBody.length())));
            
            String decodedJsonData = decodeBase64Response(responseBody);
            if (decodedJsonData == null) {
                return;
            }
            
            List<HttpRequestResponse> httpItems = serializer.deserialize(decodedJsonData);
            sendToOrganizer(httpItems);
            
        } catch (Exception e) {
            api.logging().logToError("Error processing shared item download: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private String decodeBase64Response(String responseBody) {
        try {
            byte[] decodedBytes = Base64.getDecoder().decode(responseBody);
            String decodedJsonData = new String(decodedBytes);
            api.logging().logToOutput("Successfully decoded base64 response (decoded length: " + decodedJsonData.length() + ")");
            return decodedJsonData;
        } catch (IllegalArgumentException e) {
            api.logging().logToError("Failed to decode base64 response: " + e.getMessage());
            return null;
        }
    }
    
    private void sendToOrganizer(List<HttpRequestResponse> items) {
        api.logging().logToOutput("Sending " + items.size() + " items to organizer");
        
        int successCount = 0;
        for (int i = 0; i < items.size(); i++) {
            try {
                HttpRequestResponse item = items.get(i);
                api.logging().logToOutput("Sending item " + (i + 1) + "/" + items.size() + " to organizer");
                api.organizer().sendToOrganizer(item);
                successCount++;
                api.logging().logToOutput("Successfully sent item " + (i + 1) + " to organizer");
            } catch (Exception e) {
                api.logging().logToError("Failed to send item " + (i + 1) + " to organizer: " + e.getMessage());
            }
        }
        
        api.logging().logToOutput("Successfully sent " + successCount + "/" + items.size() + " items to organizer");
    }
}