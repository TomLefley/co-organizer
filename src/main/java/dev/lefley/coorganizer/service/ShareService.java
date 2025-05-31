package dev.lefley.coorganizer.service;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.HttpService;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import dev.lefley.coorganizer.crypto.CryptoUtils;
import dev.lefley.coorganizer.model.Group;
import dev.lefley.coorganizer.serialization.HttpRequestResponseSerializer;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.util.Base64;
import java.util.List;

public class ShareService {
    private static final String STORE_HOST = "localhost";
    private static final int STORE_PORT = 3000;
    private static final String STORE_PATH = "/store";
    
    private final MontoyaApi api;
    private final HttpRequestResponseSerializer serializer;
    private final NotificationService notificationService;
    private final Gson gson;
    
    public ShareService(MontoyaApi api) {
        this.api = api;
        this.serializer = new HttpRequestResponseSerializer(api);
        this.notificationService = new NotificationService(api);
        this.gson = new Gson();
    }
    
    public void shareItems(List<HttpRequestResponse> selectedItems) {
        shareItems(selectedItems, null);
    }
    
    public void shareItems(List<HttpRequestResponse> selectedItems, Group group) {
        api.logging().logToOutput("Starting shareSelectedItems with " + selectedItems.size() + " items" + 
            (group != null ? " for group: " + group.getName() : " (no group)"));
        
        try {
            String jsonData = serializer.serialize(selectedItems);
            api.logging().logToOutput("JSON serialization complete. Data length: " + jsonData.length());
            
            // Create the outer JSON structure
            JsonObject outerJson = new JsonObject();
            
            if (group != null) {
                // Encrypt the data using the group's key
                String encryptedData = CryptoUtils.encrypt(jsonData, group.getSymmetricKey());
                outerJson.addProperty("fingerprint", group.getFingerprint());
                outerJson.addProperty("data", encryptedData);
                api.logging().logToOutput("Data encrypted for group: " + group.getName());
            } else {
                // No encryption, just wrap in outer JSON
                outerJson.addProperty("data", jsonData);
                api.logging().logToOutput("Data not encrypted (no group specified)");
            }
            
            String finalJsonData = gson.toJson(outerJson);
            api.logging().logToOutput("Final JSON structure created. Length: " + finalJsonData.length());
            
            String url = uploadToStore(finalJsonData);
            
            if (url != null) {
                copyToClipboard(url);
                api.logging().logToOutput("URL copied to clipboard: " + url);
                String message = group != null ? 
                    "Sharing link copied to clipboard! (encrypted for " + group.getName() + ")" :
                    "Sharing link copied to clipboard!";
                notificationService.showSuccessToast(message);
            } else {
                api.logging().logToError("Failed to extract URL from response");
                notificationService.showErrorToast("Share failed: could not extract link from response");
            }
            
        } catch (Exception e) {
            api.logging().logToError("Error sharing items: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            notificationService.showErrorToast("Share failed: " + e.getClass().getSimpleName());
            e.printStackTrace();
        }
    }
    
    private String uploadToStore(String jsonData) {
        api.logging().logToOutput("Creating HTTP request to: " + STORE_HOST + ":" + STORE_PORT + STORE_PATH);
        
        HttpService httpService = HttpService.httpService(STORE_HOST, STORE_PORT, false);
        
        String encodedJsonData = Base64.getEncoder().encodeToString(jsonData.getBytes());
        api.logging().logToOutput("Base64 encoded JSON data (original: " + jsonData.length() + " bytes, encoded: " + encodedJsonData.length() + " chars)");
        
        String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();
        String multipartBody = createMultipartFormData(boundary, encodedJsonData);
        
        api.logging().logToOutput("Created multipart form data with boundary: " + boundary);
        api.logging().logToOutput("Multipart body length: " + multipartBody.length());
        
        HttpRequest request = HttpRequest.httpRequest()
                .withMethod("POST")
                .withPath(STORE_PATH)
                .withHeader("Content-Type", "multipart/form-data; boundary=" + boundary)
                .withHeader("Host", STORE_HOST + ":" + STORE_PORT)
                .withBody(multipartBody)
                .withService(httpService);
        
        api.logging().logToOutput("Sending HTTP request using Montoya network stack...");
        HttpRequestResponse response = api.http().sendRequest(request);
        
        if (response.response() != null) {
            int statusCode = response.response().statusCode();
            String responseBodyStr = response.response().bodyToString();
            
            api.logging().logToOutput("HTTP response received. Status code: " + statusCode);
            api.logging().logToOutput("Response body: " + responseBodyStr);
            
            if (statusCode == 200) {
                return extractUrlFromResponse(responseBodyStr);
            } else {
                api.logging().logToError("HTTP request failed. Status: " + statusCode + ", Body: " + responseBodyStr);
                notificationService.showErrorToast("Share failed: HTTP " + statusCode);
            }
        } else {
            api.logging().logToError("No response received from HTTP request");
            notificationService.showErrorToast("Share failed: No response from server");
        }
        
        return null;
    }
    
    private String createMultipartFormData(String boundary, String encodedData) {
        api.logging().logToOutput("Creating multipart form data for file 'shareditem' with base64 encoded content");
        
        StringBuilder multipart = new StringBuilder();
        
        multipart.append("--").append(boundary).append("\r\n");
        multipart.append("Content-Disposition: form-data; name=\"shareditem\"; filename=\"shareditem\"\r\n");
        multipart.append("Content-Type: application/octet-stream\r\n");
        multipart.append("Content-Transfer-Encoding: base64\r\n");
        multipart.append("\r\n");
        multipart.append(encodedData);
        multipart.append("\r\n");
        multipart.append("--").append(boundary).append("--\r\n");
        
        api.logging().logToOutput("Multipart form data created successfully with base64 encoding");
        return multipart.toString();
    }
    
    private String extractUrlFromResponse(String responseBody) {
        api.logging().logToOutput("Extracting URL from response body (length: " + responseBody.length() + ")");
        
        if (responseBody.contains("\"url\"")) {
            api.logging().logToOutput("Found 'url' property in response");
            int urlStart = responseBody.indexOf("\"url\":");
            if (urlStart != -1) {
                api.logging().logToOutput("Found 'url' key at position: " + urlStart);
                urlStart = responseBody.indexOf("\"", urlStart + 6) + 1;
                int urlEnd = responseBody.indexOf("\"", urlStart);
                if (urlEnd != -1) {
                    String extractedUrl = responseBody.substring(urlStart, urlEnd);
                    api.logging().logToOutput("Successfully extracted URL: " + extractedUrl);
                    return extractedUrl;
                } else {
                    api.logging().logToError("Could not find end quote for URL value");
                }
            } else {
                api.logging().logToError("Could not find 'url' key in response");
            }
        } else {
            api.logging().logToError("Response does not contain 'url' property");
        }
        return null;
    }
    
    private void copyToClipboard(String text) {
        api.logging().logToOutput("Copying text to clipboard: " + text);
        try {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            StringSelection selection = new StringSelection(text);
            clipboard.setContents(selection, null);
            api.logging().logToOutput("Successfully copied to clipboard");
        } catch (Exception e) {
            api.logging().logToError("Failed to copy to clipboard: " + e.getMessage());
        }
    }
}