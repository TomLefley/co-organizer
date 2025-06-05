package dev.lefley.coorganizer.serialization;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.Annotations;
import burp.api.montoya.core.HighlightColor;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
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
        logger.debug("Serializing " + items.size() + " HTTP request/response items");
        
        JsonArray itemsArray = new JsonArray();
        
        for (int i = 0; i < items.size(); i++) {
            HttpRequestResponse item = items.get(i);
            if (item == null) {
                logger.error("Item " + i + " is null, skipping");
                continue;
            }
            
            JsonObject itemJson = new JsonObject();
            
            // Serialize request
            if (item.request() != null) {
                try {
                    String requestString = item.request().toString();
                    if (requestString != null) {
                        String encodedRequest = Base64.getEncoder().encodeToString(requestString.getBytes());
                        itemJson.addProperty("request", encodedRequest);
                    }
                } catch (Exception e) {
                    logger.error("Failed to serialize request for item " + i + ": " + e.getMessage());
                }
            }
            
            // Serialize response
            if (item.response() != null) {
                try {
                    String responseString = item.response().toString();
                    if (responseString != null) {
                        String encodedResponse = Base64.getEncoder().encodeToString(responseString.getBytes());
                        itemJson.addProperty("response", encodedResponse);
                    }
                } catch (Exception e) {
                    logger.error("Failed to serialize response for item " + i + ": " + e.getMessage());
                }
            }
            
            // Serialize annotations
            if (item.annotations() != null) {
                try {
                    JsonObject annotationsJson = new JsonObject();
                    
                    if (item.annotations().hasNotes()) {
                        String notes = item.annotations().notes();
                        if (notes != null) {
                            String encodedNotes = Base64.getEncoder().encodeToString(notes.getBytes());
                            annotationsJson.addProperty("notes", encodedNotes);
                        }
                    }
                    
                    if (item.annotations().hasHighlightColor()) {
                        HighlightColor color = item.annotations().highlightColor();
                        if (color != null) {
                            annotationsJson.addProperty("highlightColor", color.toString());
                        }
                    }
                    
                    if (annotationsJson.size() > 0) {
                        itemJson.add("annotations", annotationsJson);
                    }
                } catch (Exception e) {
                    logger.error("Failed to serialize annotations for item " + i + ": " + e.getMessage());
                }
            }
            
            if (itemJson.size() > 0) {
                itemsArray.add(itemJson);
            }
        }
        
        JsonObject rootJson = new JsonObject();
        rootJson.add("items", itemsArray);
        
        String result = gson.toJson(rootJson);
        logger.debug("Successfully serialized " + itemsArray.size() + " items");
        return result;
    }
    
    public List<HttpRequestResponse> deserialize(String jsonData) {
        logger.debug("Deserializing HTTP request/response items from JSON data");
        
        List<HttpRequestResponse> items = new ArrayList<>();
        
        try {
            JsonObject rootJson = gson.fromJson(jsonData, JsonObject.class);
            if (!rootJson.has("items")) {
                logger.error("JSON data missing 'items' array");
                return items;
            }
            
            JsonArray itemsArray = rootJson.getAsJsonArray("items");
            
            for (int i = 0; i < itemsArray.size(); i++) {
                try {
                    JsonObject itemJson = itemsArray.get(i).getAsJsonObject();
                    HttpRequestResponse item = deserializeItem(itemJson, i);
                    if (item != null) {
                        items.add(item);
                    }
                } catch (Exception e) {
                    logger.error("Failed to deserialize item " + i + ": " + e.getMessage());
                }
            }
            
        } catch (JsonSyntaxException e) {
            logger.error("Failed to parse JSON data: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error during deserialization: " + e.getMessage());
        }
        
        logger.debug("Successfully deserialized " + items.size() + " items");
        return items;
    }
    
    private HttpRequestResponse deserializeItem(JsonObject itemJson, int itemNumber) {
        HttpRequest request = deserializeRequest(itemJson, itemNumber);
        HttpResponse response = deserializeResponse(itemJson, itemNumber);
        Annotations annotations = deserializeAnnotations(itemJson, itemNumber);
        
        if (request != null) {
            return HttpRequestResponse.httpRequestResponse(request, response, annotations);
        } else {
            logger.error("Item " + itemNumber + " has no valid request, skipping");
            return null;
        }
    }
    
    private HttpRequest deserializeRequest(JsonObject itemJson, int itemNumber) {
        if (itemJson.has("request")) {
            try {
                String encodedRequest = itemJson.get("request").getAsString();
                String requestString = new String(Base64.getDecoder().decode(encodedRequest));
                return HttpRequest.httpRequest(requestString);
            } catch (Exception e) {
                logger.error("Failed to deserialize request for item " + itemNumber + ": " + e.getMessage());
            }
        }
        return null;
    }
    
    private HttpResponse deserializeResponse(JsonObject itemJson, int itemNumber) {
        if (itemJson.has("response")) {
            try {
                String encodedResponse = itemJson.get("response").getAsString();
                String responseString = new String(Base64.getDecoder().decode(encodedResponse));
                return HttpResponse.httpResponse(responseString);
            } catch (Exception e) {
                logger.error("Failed to deserialize response for item " + itemNumber + ": " + e.getMessage());
            }
        }
        return null;
    }
    
    private Annotations deserializeAnnotations(JsonObject itemJson, int itemNumber) {
        if (itemJson.has("annotations")) {
            try {
                JsonObject annotationsJson = itemJson.getAsJsonObject("annotations");
                Annotations annotations = Annotations.annotations();
                
                if (annotationsJson.has("notes")) {
                    String encodedNotes = annotationsJson.get("notes").getAsString();
                    String notes = new String(Base64.getDecoder().decode(encodedNotes));
                    annotations = annotations.withNotes(notes);
                }
                
                if (annotationsJson.has("highlightColor")) {
                    String colorString = annotationsJson.get("highlightColor").getAsString();
                    try {
                        HighlightColor color = HighlightColor.valueOf(colorString);
                        annotations = annotations.withHighlightColor(color);
                    } catch (IllegalArgumentException e) {
                        logger.error("Invalid highlight color: " + colorString);
                    }
                }
                
                return annotations;
            } catch (Exception e) {
                logger.error("Failed to deserialize annotations for item " + itemNumber + ": " + e.getMessage());
            }
        }
        return null;
    }
}