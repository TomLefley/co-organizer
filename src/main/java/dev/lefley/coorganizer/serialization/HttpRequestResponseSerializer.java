package dev.lefley.coorganizer.serialization;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.Annotations;
import burp.api.montoya.core.HighlightColor;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import dev.lefley.coorganizer.model.AnnotationData;
import dev.lefley.coorganizer.model.HttpRequestData;
import dev.lefley.coorganizer.model.HttpRequestResponseData;
import dev.lefley.coorganizer.model.HttpResponseData;
import dev.lefley.coorganizer.util.Logger;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class HttpRequestResponseSerializer {
    private final MontoyaApi api;
    private final Gson gson;
    private final Logger logger;
    
    public HttpRequestResponseSerializer(MontoyaApi api) {
        this.api = api;
        this.gson = new Gson();
        this.logger = new Logger(api, HttpRequestResponseSerializer.class);
    }
    
    public String serialize(List<HttpRequestResponse> items) {
        List<HttpRequestResponseData> dataItems = new ArrayList<>();
        
        for (int i = 0; i < items.size(); i++) {
            HttpRequestResponse item = items.get(i);
            
            HttpRequestData requestData = serializeRequest(item, i + 1);
            HttpResponseData responseData = serializeResponse(item, i + 1);
            AnnotationData annotationData = serializeAnnotations(item, i + 1);
            
            dataItems.add(new HttpRequestResponseData(requestData, responseData, annotationData));
        }
        
        String json = gson.toJson(dataItems);
        return json;
    }
    
    public List<HttpRequestResponse> deserialize(String jsonData) {
        List<HttpRequestResponse> items = new ArrayList<>();
        
        try {
            Type listType = new TypeToken<List<HttpRequestResponseData>>(){}.getType();
            List<HttpRequestResponseData> dataItems = gson.fromJson(jsonData, listType);
            
            
            for (int i = 0; i < dataItems.size(); i++) {
                HttpRequestResponseData dataItem = dataItems.get(i);
                
                HttpRequestResponse item = deserializeItem(dataItem, i + 1);
                if (item != null) {
                    items.add(item);
                }
            }
        } catch (Exception e) {
            logger.error("Error deserializing HTTP items with Gson", e);
        }
        
        return items;
    }
    
    private HttpRequestData serializeRequest(HttpRequestResponse item, int itemNumber) {
        String method = item.request().method();
        String url = item.request().url();
        
        String requestHeaders = item.request().toString().split("\\r\\n\\r\\n")[0];
        byte[] requestBodyBytes = item.request().body().getBytes();
        
        return new HttpRequestData(
            Base64.getEncoder().encodeToString(method.getBytes()),
            Base64.getEncoder().encodeToString(url.getBytes()),
            Base64.getEncoder().encodeToString(requestHeaders.getBytes()),
            Base64.getEncoder().encodeToString(requestBodyBytes)
        );
    }
    
    private HttpResponseData serializeResponse(HttpRequestResponse item, int itemNumber) {
        if (item.response() != null) {
            int statusCode = item.response().statusCode();
            
            String responseHeaders = item.response().headers().toString();
            byte[] responseBodyBytes = item.response().body().getBytes();
            
            return new HttpResponseData(
                Base64.getEncoder().encodeToString(String.valueOf(statusCode).getBytes()),
                Base64.getEncoder().encodeToString(responseHeaders.getBytes()),
                Base64.getEncoder().encodeToString(responseBodyBytes)
            );
        } else {
            return new HttpResponseData(
                Base64.getEncoder().encodeToString("0".getBytes()),
                Base64.getEncoder().encodeToString("".getBytes()),
                Base64.getEncoder().encodeToString("".getBytes())
            );
        }
    }
    
    private AnnotationData serializeAnnotations(HttpRequestResponse item, int itemNumber) {
        
        try {
            // Get annotations from the item
            String notes = "";
            String highlightColor = "";
            boolean hasNotes = false;
            boolean hasHighlightColor = false;
            
            if (item.annotations() != null) {
                // Check if the item has notes
                if (item.annotations().hasNotes()) {
                    notes = item.annotations().notes();
                    hasNotes = true;
                }
                
                // Check if the item has highlight color
                if (item.annotations().hasHighlightColor()) {
                    highlightColor = item.annotations().highlightColor().toString();
                    hasHighlightColor = true;
                }
            }
            
            return new AnnotationData(
                Base64.getEncoder().encodeToString(notes.getBytes()),
                Base64.getEncoder().encodeToString(highlightColor.getBytes()),
                hasNotes,
                hasHighlightColor
            );
            
        } catch (Exception e) {
            logger.error("Error serializing annotations for item " + itemNumber, e);
            // Return empty annotation data on error
            return new AnnotationData(
                Base64.getEncoder().encodeToString("".getBytes()),
                Base64.getEncoder().encodeToString("".getBytes()),
                false,
                false
            );
        }
    }
    
    private HttpRequestResponse deserializeItem(HttpRequestResponseData dataItem, int itemNumber) {
        HttpRequestData requestData = dataItem.request;
        if (requestData == null) {
            logger.error("No request data found for object " + itemNumber);
            return null;
        }
        
        try {
            String requestUrl = new String(Base64.getDecoder().decode(requestData.url));
            String requestMethod = new String(Base64.getDecoder().decode(requestData.method));
            String requestHeaders = new String(Base64.getDecoder().decode(requestData.headers));
            byte[] requestBody = Base64.getDecoder().decode(requestData.body);
            
            
            HttpRequest request = HttpRequest.httpRequestFromUrl(requestUrl)
                .withMethod(requestMethod)
                .withBody(new String(requestBody));
            
            HttpResponse response = deserializeResponse(dataItem.response, itemNumber);
            
            // Create annotations from deserialized data
            Annotations annotations = null;
            if (dataItem.annotations != null) {
                annotations = createAnnotations(dataItem.annotations, itemNumber);
            }
            
            // Create HttpRequestResponse with annotations using the three-parameter factory method
            HttpRequestResponse item;
            if (annotations != null) {
                item = HttpRequestResponse.httpRequestResponse(request, response, annotations);
            } else {
                item = HttpRequestResponse.httpRequestResponse(request, response);
            }
            
            return item;
            
        } catch (Exception e) {
            logger.error("Failed to decode request data for object " + itemNumber, e);
            return null;
        }
    }
    
    private HttpResponse deserializeResponse(HttpResponseData responseData, int itemNumber) {
        if (responseData == null) {
            return null;
        }
        
        try {
            String statusCodeStr = new String(Base64.getDecoder().decode(responseData.statusCode));
            String responseHeaders = new String(Base64.getDecoder().decode(responseData.headers));
            byte[] responseBody = Base64.getDecoder().decode(responseData.body);
            
            int statusCode = Integer.parseInt(statusCodeStr);
            
            if (statusCode > 0) {
                String responseString = "HTTP/1.1 " + statusCode + " OK\r\n" + responseHeaders + "\r\n\r\n" + new String(responseBody);
                HttpResponse response = HttpResponse.httpResponse(responseString);
                return response;
            }
        } catch (Exception e) {
            logger.error("Failed to decode response data for object " + itemNumber, e);
        }
        
        return null;
    }
    
    private Annotations createAnnotations(AnnotationData annotationData, int itemNumber) {
        try {
            if (annotationData.hasNotes || annotationData.hasHighlightColor) {
                
                String notes = "";
                HighlightColor highlightColor = null;
                
                if (annotationData.hasNotes) {
                    notes = new String(Base64.getDecoder().decode(annotationData.notes));
                }
                
                if (annotationData.hasHighlightColor) {
                    String colorName = new String(Base64.getDecoder().decode(annotationData.highlightColor));
                    try {
                        highlightColor = HighlightColor.valueOf(colorName);
                    } catch (IllegalArgumentException e) {
                        logger.error("Invalid highlight color: " + colorName + " for item " + itemNumber);
                    }
                }
                
                // Create annotations using the Montoya API factory method
                if (annotationData.hasNotes && annotationData.hasHighlightColor && highlightColor != null) {
                    return Annotations.annotations(notes, highlightColor);
                } else if (annotationData.hasNotes) {
                    return Annotations.annotations(notes);
                } else if (annotationData.hasHighlightColor && highlightColor != null) {
                    return Annotations.annotations(highlightColor);
                }
            }
            
            return null; // No annotations to create
            
        } catch (Exception e) {
            logger.error("Error creating annotations for item " + itemNumber, e);
            return null;
        }
    }
}