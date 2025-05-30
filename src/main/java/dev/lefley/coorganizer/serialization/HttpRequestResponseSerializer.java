package dev.lefley.coorganizer.serialization;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import dev.lefley.coorganizer.model.HttpRequestData;
import dev.lefley.coorganizer.model.HttpRequestResponseData;
import dev.lefley.coorganizer.model.HttpResponseData;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class HttpRequestResponseSerializer {
    private final MontoyaApi api;
    private final Gson gson;
    
    public HttpRequestResponseSerializer(MontoyaApi api) {
        this.api = api;
        this.gson = new Gson();
    }
    
    public String serialize(List<HttpRequestResponse> items) {
        api.logging().logToOutput("Starting serialization of " + items.size() + " HTTP request/response items using Gson");
        
        List<HttpRequestResponseData> dataItems = new ArrayList<>();
        
        for (int i = 0; i < items.size(); i++) {
            api.logging().logToOutput("Serializing item " + (i + 1) + "/" + items.size());
            HttpRequestResponse item = items.get(i);
            
            HttpRequestData requestData = serializeRequest(item, i + 1);
            HttpResponseData responseData = serializeResponse(item, i + 1);
            
            dataItems.add(new HttpRequestResponseData(requestData, responseData));
        }
        
        String json = gson.toJson(dataItems);
        api.logging().logToOutput("Gson serialization complete. Total JSON length: " + json.length());
        return json;
    }
    
    public List<HttpRequestResponse> deserialize(String jsonData) {
        api.logging().logToOutput("Starting deserialization of JSON data using Gson (length: " + jsonData.length() + ")");
        List<HttpRequestResponse> items = new ArrayList<>();
        
        try {
            Type listType = new TypeToken<List<HttpRequestResponseData>>(){}.getType();
            List<HttpRequestResponseData> dataItems = gson.fromJson(jsonData, listType);
            
            api.logging().logToOutput("Gson deserialized " + dataItems.size() + " objects");
            
            for (int i = 0; i < dataItems.size(); i++) {
                api.logging().logToOutput("Processing object " + (i + 1) + "/" + dataItems.size());
                HttpRequestResponseData dataItem = dataItems.get(i);
                
                HttpRequestResponse item = deserializeItem(dataItem, i + 1);
                if (item != null) {
                    items.add(item);
                }
            }
        } catch (Exception e) {
            api.logging().logToError("Error deserializing HTTP items with Gson: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            e.printStackTrace();
        }
        
        api.logging().logToOutput("Gson deserialization complete. Created " + items.size() + " HttpRequestResponse items");
        return items;
    }
    
    private HttpRequestData serializeRequest(HttpRequestResponse item, int itemNumber) {
        api.logging().logToOutput("Serializing request for item " + itemNumber);
        String method = item.request().method();
        String url = item.request().url();
        api.logging().logToOutput("Request method: " + method + ", URL: " + url);
        
        String requestHeaders = item.request().toString().split("\\r\\n\\r\\n")[0];
        byte[] requestBodyBytes = item.request().body().getBytes();
        api.logging().logToOutput("Request headers length: " + requestHeaders.length() + ", body length: " + requestBodyBytes.length + " bytes");
        
        return new HttpRequestData(
            Base64.getEncoder().encodeToString(method.getBytes()),
            Base64.getEncoder().encodeToString(url.getBytes()),
            Base64.getEncoder().encodeToString(requestHeaders.getBytes()),
            Base64.getEncoder().encodeToString(requestBodyBytes)
        );
    }
    
    private HttpResponseData serializeResponse(HttpRequestResponse item, int itemNumber) {
        if (item.response() != null) {
            api.logging().logToOutput("Serializing response for item " + itemNumber);
            int statusCode = item.response().statusCode();
            api.logging().logToOutput("Response status code: " + statusCode);
            
            String responseHeaders = item.response().headers().toString();
            byte[] responseBodyBytes = item.response().body().getBytes();
            api.logging().logToOutput("Response headers length: " + responseHeaders.length() + ", body length: " + responseBodyBytes.length + " bytes");
            
            return new HttpResponseData(
                Base64.getEncoder().encodeToString(String.valueOf(statusCode).getBytes()),
                Base64.getEncoder().encodeToString(responseHeaders.getBytes()),
                Base64.getEncoder().encodeToString(responseBodyBytes)
            );
        } else {
            api.logging().logToOutput("No response available for item " + itemNumber);
            return new HttpResponseData(
                Base64.getEncoder().encodeToString("0".getBytes()),
                Base64.getEncoder().encodeToString("".getBytes()),
                Base64.getEncoder().encodeToString("".getBytes())
            );
        }
    }
    
    private HttpRequestResponse deserializeItem(HttpRequestResponseData dataItem, int itemNumber) {
        HttpRequestData requestData = dataItem.request;
        if (requestData == null) {
            api.logging().logToError("No request data found for object " + itemNumber);
            return null;
        }
        
        try {
            String requestUrl = new String(Base64.getDecoder().decode(requestData.url));
            String requestMethod = new String(Base64.getDecoder().decode(requestData.method));
            String requestHeaders = new String(Base64.getDecoder().decode(requestData.headers));
            byte[] requestBody = Base64.getDecoder().decode(requestData.body);
            
            api.logging().logToOutput("Decoded request - URL: " + requestUrl + ", Method: " + requestMethod);
            
            HttpRequest request = HttpRequest.httpRequestFromUrl(requestUrl)
                .withMethod(requestMethod)
                .withBody(new String(requestBody));
            
            HttpResponse response = deserializeResponse(dataItem.response, itemNumber);
            
            HttpRequestResponse item = HttpRequestResponse.httpRequestResponse(request, response);
            api.logging().logToOutput("Successfully created HttpRequestResponse with " + (response != null ? "response" : "request only") + " for object " + itemNumber);
            return item;
            
        } catch (Exception e) {
            api.logging().logToError("Failed to decode request data for object " + itemNumber + ": " + e.getMessage());
            return null;
        }
    }
    
    private HttpResponse deserializeResponse(HttpResponseData responseData, int itemNumber) {
        if (responseData == null) {
            api.logging().logToOutput("No response data available for object " + itemNumber);
            return null;
        }
        
        try {
            String statusCodeStr = new String(Base64.getDecoder().decode(responseData.statusCode));
            String responseHeaders = new String(Base64.getDecoder().decode(responseData.headers));
            byte[] responseBody = Base64.getDecoder().decode(responseData.body);
            
            int statusCode = Integer.parseInt(statusCodeStr);
            api.logging().logToOutput("Decoded response - Status: " + statusCode + ", Headers length: " + responseHeaders.length() + ", Body length: " + responseBody.length);
            
            if (statusCode > 0) {
                String responseString = "HTTP/1.1 " + statusCode + " OK\r\n" + responseHeaders + "\r\n\r\n" + new String(responseBody);
                HttpResponse response = HttpResponse.httpResponse(responseString);
                api.logging().logToOutput("Created HttpResponse for object " + itemNumber);
                return response;
            }
        } catch (Exception e) {
            api.logging().logToError("Failed to decode response data for object " + itemNumber + ": " + e.getMessage());
        }
        
        return null;
    }
}