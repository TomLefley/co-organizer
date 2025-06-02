package dev.lefley.coorganizer.service;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.HttpService;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import dev.lefley.coorganizer.config.ServerConfiguration;
import dev.lefley.coorganizer.crypto.CryptoUtils;
import dev.lefley.coorganizer.model.Group;
import dev.lefley.coorganizer.serialization.HttpRequestResponseSerializer;
import dev.lefley.coorganizer.util.Logger;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.util.Base64;
import java.util.List;

public class ShareService {
    // Server configuration - see ServerConfiguration class to change server address
    private static final String STORE_HOST = ServerConfiguration.HOST;
    private static final int STORE_PORT = ServerConfiguration.PORT;
    private static final String STORE_PATH = ServerConfiguration.SHARE_ENDPOINT;
    
    private final MontoyaApi api;
    private final HttpRequestResponseSerializer serializer;
    private final NotificationService notificationService;
    private final DebugIdManager debugIdManager;
    private final Gson gson;
    private final Logger logger;
    
    public ShareService(MontoyaApi api) {
        this.api = api;
        this.serializer = new HttpRequestResponseSerializer(api);
        this.notificationService = new NotificationService(api);
        this.debugIdManager = new DebugIdManager(api);
        this.gson = new Gson();
        this.logger = new Logger(api, ShareService.class);
    }
    
    public void shareItems(List<HttpRequestResponse> selectedItems) {
        shareItems(selectedItems, null);
    }
    
    public void shareItems(List<HttpRequestResponse> selectedItems, Group group) {
        logger.info("Starting share operation with " + selectedItems.size() + " items" + 
            (group != null ? " for group: " + group.getName() : " (unencrypted)"));
        
        try {
            String jsonData = serializer.serialize(selectedItems);
            logger.debug("JSON serialization complete. Data length: " + jsonData.length());
            
            // Create the outer JSON structure
            JsonObject outerJson = new JsonObject();
            
            if (group != null) {
                // Encrypt the data using the group's key
                String encryptedData = CryptoUtils.encrypt(jsonData, group.getSymmetricKey());
                outerJson.addProperty("fingerprint", group.getFingerprint());
                outerJson.addProperty("data", encryptedData);
                logger.debug("Data encrypted for group: " + group.getName());
            } else {
                // No encryption, just wrap in outer JSON
                outerJson.addProperty("data", jsonData);
                logger.debug("Data not encrypted (no group specified)");
            }
            
            String finalJsonData = gson.toJson(outerJson);
            logger.debug("Final JSON structure created. Length: " + finalJsonData.length());
            
            String url = uploadToStore(finalJsonData);
            
            if (url != null) {
                copyToClipboard(url);
                logger.info("Share successful - URL copied to clipboard: " + url);
                String message = group != null ? 
                    "Sharing link copied to clipboard! (encrypted for " + group.getName() + ")" :
                    "Sharing link copied to clipboard!";
                notificationService.showSuccessToast(message);
            } else {
                logger.error("Failed to extract URL from response");
                notificationService.showErrorToast("Share failed: could not extract link from response");
            }
            
        } catch (Exception e) {
            logger.error("Error sharing items", e);
            notificationService.showErrorToast("Share failed: " + e.getClass().getSimpleName());
        }
    }
    
    private String uploadToStore(String jsonData) {
        logger.debug("Creating HTTP request to: " + STORE_HOST + ":" + STORE_PORT + STORE_PATH);
        
        HttpService httpService = HttpService.httpService(STORE_HOST, STORE_PORT, false);
        
        String encodedJsonData = Base64.getEncoder().encodeToString(jsonData.getBytes());
        logger.debug("Base64 encoded JSON data (original: " + jsonData.length() + " bytes, encoded: " + encodedJsonData.length() + " chars)");
        
        String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();
        String multipartBody = createMultipartFormData(boundary, encodedJsonData);
        
        logger.debug("Created multipart form data with boundary: " + boundary);
        logger.trace("Multipart body length: " + multipartBody.length());
        
        HttpRequest.Builder requestBuilder = HttpRequest.httpRequest()
                .withMethod("POST")
                .withPath(STORE_PATH)
                .withHeader("Content-Type", "multipart/form-data; boundary=" + boundary)
                .withHeader("Host", STORE_HOST + ":" + STORE_PORT);
        
        // Add debug ID header if enabled
        if (debugIdManager.isDebugIdEnabled()) {
            requestBuilder.withHeader("X-Debug-Id", debugIdManager.getDebugId());
            logger.debug("Added debug ID header to request");
        } else {
            logger.debug("Debug ID disabled - not adding header to request");
        }
        
        HttpRequest request = requestBuilder
                .withBody(multipartBody)
                .withService(httpService);
        
        logger.debug("Sending HTTP request using Montoya network stack...");
        HttpRequestResponse response = api.http().sendRequest(request);
        
        if (response.response() != null) {
            int statusCode = response.response().statusCode();
            String responseBodyStr = response.response().bodyToString();
            
            logger.debug("HTTP response received. Status code: " + statusCode);
            logger.trace("Response body: " + responseBodyStr);
            
            if (statusCode == 200) {
                return extractUrlFromResponse(responseBodyStr);
            } else {
                logger.error("HTTP request failed. Status: " + statusCode + ", Body: " + responseBodyStr);
                notificationService.showErrorToast("Share failed: HTTP " + statusCode);
            }
        } else {
            logger.error("No response received from HTTP request");
            notificationService.showErrorToast("Share failed: No response from server");
        }
        
        return null;
    }
    
    private String createMultipartFormData(String boundary, String encodedData) {
        logger.trace("Creating multipart form data for file 'rr' with base64 encoded content");
        
        StringBuilder multipart = new StringBuilder();
        
        multipart.append("--").append(boundary).append("\r\n");
        multipart.append("Content-Disposition: form-data; name=\"rr\"; filename=\"rr\"\r\n");
        multipart.append("Content-Type: application/octet-stream\r\n");
        multipart.append("Content-Transfer-Encoding: base64\r\n");
        multipart.append("\r\n");
        multipart.append(encodedData);
        multipart.append("\r\n");
        multipart.append("--").append(boundary).append("--\r\n");
        
        logger.trace("Multipart form data created successfully with base64 encoding");
        return multipart.toString();
    }
    
    private String extractUrlFromResponse(String responseBody) {
        logger.debug("Extracting URL from response body (length: " + responseBody.length() + ")");
        
        if (responseBody.contains("\"url\"")) {
            logger.trace("Found 'url' property in response");
            int urlStart = responseBody.indexOf("\"url\":");
            if (urlStart != -1) {
                logger.trace("Found 'url' key at position: " + urlStart);
                urlStart = responseBody.indexOf("\"", urlStart + 6) + 1;
                int urlEnd = responseBody.indexOf("\"", urlStart);
                if (urlEnd != -1) {
                    String extractedUrl = responseBody.substring(urlStart, urlEnd);
                    logger.debug("Successfully extracted URL: " + extractedUrl);
                    return extractedUrl;
                } else {
                    logger.error("Could not find end quote for URL value");
                }
            } else {
                logger.error("Could not find 'url' key in response");
            }
        } else {
            logger.error("Response does not contain 'url' property");
        }
        return null;
    }
    
    private void copyToClipboard(String text) {
        logger.debug("Copying text to clipboard: " + text);
        try {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            StringSelection selection = new StringSelection(text);
            clipboard.setContents(selection, null);
            logger.debug("Successfully copied to clipboard");
        } catch (Exception e) {
            logger.error("Failed to copy to clipboard: " + e.getMessage());
        }
    }
}