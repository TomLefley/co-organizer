package dev.lefley.coorganizer.service;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.HttpService;
import burp.api.montoya.http.RequestOptions;
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
    private final Gson gson;
    private final Logger logger;
    
    public ShareService(MontoyaApi api) {
        this.api = api;
        this.serializer = new HttpRequestResponseSerializer(api);
        this.notificationService = new NotificationService(api);
        this.gson = new Gson();
        this.logger = new Logger(api, ShareService.class);
    }
    
    public void shareItems(List<HttpRequestResponse> selectedItems) {
        shareItems(selectedItems, null);
    }
    
    public void shareItems(List<HttpRequestResponse> selectedItems, Group group) {
        try {
            String jsonData = serializer.serialize(selectedItems);
            
            // Create the outer JSON structure
            JsonObject outerJson = new JsonObject();
            
            if (group != null) {
                // Encrypt the data using the group's key
                String encryptedData = CryptoUtils.encrypt(jsonData, group.getSymmetricKey());
                outerJson.addProperty("fingerprint", group.getFingerprint());
                outerJson.addProperty("data", encryptedData);
            } else {
                // No encryption, just wrap in outer JSON
                outerJson.addProperty("data", jsonData);
            }
            
            String finalJsonData = gson.toJson(outerJson);
            String url = uploadToStore(finalJsonData);
            
            if (url != null) {
                copyToClipboard(url);
                logger.info("Share successful - " + selectedItems.size() + " items");
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
        HttpService httpService = HttpService.httpService(STORE_HOST, STORE_PORT, false);
        
        String encodedJsonData = Base64.getEncoder().encodeToString(jsonData.getBytes());
        String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();
        String multipartBody = createMultipartFormData(boundary, encodedJsonData);
        
        HttpRequest request = HttpRequest.httpRequest()
                .withMethod("POST")
                .withPath(STORE_PATH)
                .withHeader("Content-Type", "multipart/form-data; boundary=" + boundary)
                .withHeader("Host", STORE_HOST + ":" + STORE_PORT)
                .withBody(multipartBody)
                .withService(httpService);
        
        RequestOptions requestOptions = RequestOptions.requestOptions().withUpstreamTLSVerification();
        HttpRequestResponse response = api.http().sendRequest(request, requestOptions);
        
        if (response.response() != null) {
            int statusCode = response.response().statusCode();
            String responseBodyStr = response.response().bodyToString();
            
            if (statusCode == 200) {
                return extractUrlFromResponse(responseBodyStr);
            } else {
                logger.debug("HTTP request failed. Status: " + statusCode);
                notificationService.showErrorToast("Share failed: HTTP " + statusCode);
            }
        } else {
            logger.error("No response received from HTTP request");
            notificationService.showErrorToast("Share failed: No response from server");
        }
        
        return null;
    }
    
    private String createMultipartFormData(String boundary, String encodedData) {
        StringBuilder multipart = new StringBuilder();
        
        multipart.append("--").append(boundary).append("\r\n");
        multipart.append("Content-Disposition: form-data; name=\"rr\"; filename=\"rr\"\r\n");
        multipart.append("Content-Type: application/octet-stream\r\n");
        multipart.append("Content-Transfer-Encoding: base64\r\n");
        multipart.append("\r\n");
        multipart.append(encodedData);
        multipart.append("\r\n");
        multipart.append("--").append(boundary).append("--\r\n");
        
        return multipart.toString();
    }
    
    private String extractUrlFromResponse(String responseBody) {
        if (responseBody.contains("\"url\"")) {
            int urlStart = responseBody.indexOf("\"url\":");
            if (urlStart != -1) {
                urlStart = responseBody.indexOf("\"", urlStart + 6) + 1;
                int urlEnd = responseBody.indexOf("\"", urlStart);
                if (urlEnd != -1) {
                    String extractedUrl = responseBody.substring(urlStart, urlEnd);
                    return extractedUrl;
                } else {
                    logger.debug("Could not find end quote for URL value");
                }
            } else {
                logger.debug("Could not find 'url' key in response");
            }
        } else {
            logger.debug("Response does not contain 'url' property");
        }
        return null;
    }
    
    private void copyToClipboard(String text) {
        try {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            StringSelection selection = new StringSelection(text);
            clipboard.setContents(selection, null);
        } catch (Exception e) {
            logger.error("Failed to copy to clipboard", e);
        }
    }
}