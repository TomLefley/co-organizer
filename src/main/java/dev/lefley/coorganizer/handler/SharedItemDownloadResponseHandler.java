package dev.lefley.coorganizer.handler;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.responses.HttpResponse;
import burp.api.montoya.proxy.http.InterceptedResponse;
import burp.api.montoya.proxy.http.ProxyResponseHandler;
import burp.api.montoya.proxy.http.ProxyResponseReceivedAction;
import burp.api.montoya.proxy.http.ProxyResponseToBeSentAction;
import dev.lefley.coorganizer.crypto.CryptoUtils;
import dev.lefley.coorganizer.matcher.SharedItemDownloadMatcher;
import dev.lefley.coorganizer.model.Group;
import dev.lefley.coorganizer.serialization.HttpRequestResponseSerializer;
import dev.lefley.coorganizer.service.GroupManager;
import dev.lefley.coorganizer.util.Logger;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.util.Base64;
import java.util.List;

public class SharedItemDownloadResponseHandler implements ProxyResponseHandler {
    private static final String IMPORTED_MESSAGE = "[shared item imported]";
    private static final short SUCCESS_STATUS_CODE = 201;
    private static final short UNAUTHORIZED_STATUS_CODE = 401;
    
    private final MontoyaApi api;
    private final SharedItemDownloadMatcher matcher;
    private final HttpRequestResponseSerializer serializer;
    private final GroupManager groupManager;
    private final Gson gson;
    private final Logger logger;
    
    public SharedItemDownloadResponseHandler(MontoyaApi api) {
        this.api = api;
        this.matcher = new SharedItemDownloadMatcher(api);
        this.serializer = new HttpRequestResponseSerializer(api);
        this.groupManager = new GroupManager(api);
        this.gson = new Gson();
        this.logger = new Logger(api, SharedItemDownloadResponseHandler.class);
    }
    
    @Override
    public ProxyResponseReceivedAction handleResponseReceived(InterceptedResponse interceptedResponse) {
        String url = interceptedResponse.initiatingRequest().url();
        logger.debug("Proxy intercepted response from URL: " + url);
        
        if (matcher.matches(interceptedResponse)) {
            logger.info("Response matches shared item import pattern, processing...");
            
            // Check if user has access to decrypt the data
            ProcessResult result = processDownload(interceptedResponse);
            
            if (result == ProcessResult.UNAUTHORIZED) {
                logger.info("User does not have access to this group, returning 401 Unauthorized");
                HttpResponse unauthorizedResponse = interceptedResponse.withBody("Unauthorized").withStatusCode(UNAUTHORIZED_STATUS_CODE);
                return ProxyResponseReceivedAction.doNotIntercept(unauthorizedResponse);
            } else if (result == ProcessResult.SUCCESS) {
                logger.info("Successfully processed shared items, returning 201 Success");
                HttpResponse modifiedResponse = interceptedResponse.withBody(IMPORTED_MESSAGE).withStatusCode(SUCCESS_STATUS_CODE);
                return ProxyResponseReceivedAction.doNotIntercept(modifiedResponse);
            } else {
                logger.error("Failed to process shared items, continuing with original response");
                return ProxyResponseReceivedAction.continueWith(interceptedResponse);
            }
            
        } else {
            logger.trace("Response does not match shared item import pattern, continuing");
            return ProxyResponseReceivedAction.continueWith(interceptedResponse);
        }
    }
    
    @Override
    public ProxyResponseToBeSentAction handleResponseToBeSent(InterceptedResponse interceptedResponse) {
        return ProxyResponseToBeSentAction.continueWith(interceptedResponse);
    }
    
    private enum ProcessResult {
        SUCCESS, UNAUTHORIZED, ERROR
    }
    
    private ProcessResult processDownload(InterceptedResponse interceptedResponse) {
        try {
            String responseBody = interceptedResponse.bodyToString();
            logger.debug("Response body length: " + responseBody.length());
            
            String outerJsonData = decodeBase64Response(responseBody);
            if (outerJsonData == null) {
                return ProcessResult.ERROR;
            }
            
            // Parse the outer JSON structure
            JsonObject outerJson = gson.fromJson(outerJsonData, JsonObject.class);
            
            String actualData;
            if (outerJson.has("fingerprint")) {
                // Encrypted data - need to decrypt
                String fingerprint = outerJson.get("fingerprint").getAsString();
                String encryptedData = outerJson.get("data").getAsString();
                
                logger.debug("Found encrypted data for fingerprint: " + fingerprint);
                
                // Refresh groups to ensure we have the latest data (in case user left a group)
                groupManager.refreshGroupsFromPreferences();
                
                // Find the group with this fingerprint
                Group group = findGroupByFingerprint(fingerprint);
                if (group == null) {
                    logger.info("User does not have access to group with fingerprint: " + fingerprint);
                    return ProcessResult.UNAUTHORIZED;
                }
                
                // Decrypt the data
                logger.debug("Decrypting data using group: " + group.getName());
                actualData = CryptoUtils.decrypt(encryptedData, group.getSymmetricKey());
                
            } else {
                // Unencrypted data
                actualData = outerJson.get("data").getAsString();
                logger.debug("Processing unencrypted shared data");
            }
            
            // Deserialize and send to organizer
            List<HttpRequestResponse> httpItems = serializer.deserialize(actualData);
            sendToOrganizer(httpItems);
            
            return ProcessResult.SUCCESS;
            
        } catch (Exception e) {
            logger.error("Error processing shared item download", e);
            return ProcessResult.ERROR;
        }
    }
    
    private Group findGroupByFingerprint(String fingerprint) {
        return groupManager.getGroups().stream()
            .filter(group -> group.getFingerprint().equals(fingerprint))
            .findFirst()
            .orElse(null);
    }
    
    private String decodeBase64Response(String responseBody) {
        try {
            byte[] decodedBytes = Base64.getDecoder().decode(responseBody);
            String decodedJsonData = new String(decodedBytes);
            logger.debug("Successfully decoded base64 response (decoded length: " + decodedJsonData.length() + ")");
            return decodedJsonData;
        } catch (IllegalArgumentException e) {
            logger.error("Failed to decode base64 response: " + e.getMessage());
            return null;
        }
    }
    
    private void sendToOrganizer(List<HttpRequestResponse> items) {
        logger.info("Sending " + items.size() + " items to organizer");
        
        int successCount = 0;
        for (int i = 0; i < items.size(); i++) {
            try {
                HttpRequestResponse item = items.get(i);
                logger.trace("Sending item " + (i + 1) + "/" + items.size() + " to organizer");
                api.organizer().sendToOrganizer(item);
                successCount++;
                logger.trace("Successfully sent item " + (i + 1) + " to organizer");
            } catch (Exception e) {
                logger.error("Failed to send item " + (i + 1) + " to organizer: " + e.getMessage());
            }
        }
        
        logger.info("Successfully sent " + successCount + "/" + items.size() + " items to organizer");
    }
}