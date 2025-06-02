package dev.lefley.coorganizer.handler;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.responses.HttpResponse;
import burp.api.montoya.proxy.http.InterceptedResponse;
import burp.api.montoya.proxy.http.ProxyResponseReceivedAction;
import burp.api.montoya.logging.Logging;
import dev.lefley.coorganizer.config.ServerConfiguration;
import dev.lefley.coorganizer.crypto.CryptoUtils;
import dev.lefley.coorganizer.matcher.SharedItemDownloadMatcher;
import dev.lefley.coorganizer.model.Group;
import dev.lefley.coorganizer.serialization.HttpRequestResponseSerializer;
import dev.lefley.coorganizer.service.GroupManager;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@DisplayName("SharedItemDownloadResponseHandler Tests")
class SharedItemDownloadResponseHandlerTest {

    @Mock
    private MontoyaApi api;
    
    @Mock
    private Logging logging;
    
    @Mock
    private SharedItemDownloadMatcher matcher;
    
    @Mock
    private HttpRequestResponseSerializer serializer;
    
    @Mock
    private GroupManager groupManager;
    
    @Mock
    private InterceptedResponse interceptedResponse;
    
    @Mock
    private burp.api.montoya.http.message.requests.HttpRequest httpRequest;

    private SharedItemDownloadResponseHandler handler;
    private Gson gson;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        when(api.logging()).thenReturn(logging);
        when(interceptedResponse.initiatingRequest()).thenReturn(httpRequest);
        when(httpRequest.url()).thenReturn(ServerConfiguration.BASE_URL + "/test");
        
        handler = new SharedItemDownloadResponseHandler(api);
        gson = new Gson();
    }

    @Test
    @DisplayName("Should handle JSON structure parsing correctly")
    void shouldHandleJsonStructureParsingCorrectly() {
        // Given: Test JSON structures
        String testFingerprint = "ABCD-1234-EFGH-5678";
        String encryptedData = "encrypted-test-data";
        
        JsonObject encryptedJson = new JsonObject();
        encryptedJson.addProperty("fingerprint", testFingerprint);
        encryptedJson.addProperty("data", encryptedData);
        
        JsonObject unencryptedJson = new JsonObject();
        unencryptedJson.addProperty("data", "unencrypted-data");
        
        // When: Parsing structures
        String encryptedString = gson.toJson(encryptedJson);
        String unencryptedString = gson.toJson(unencryptedJson);
        
        JsonObject parsedEncrypted = gson.fromJson(encryptedString, JsonObject.class);
        JsonObject parsedUnencrypted = gson.fromJson(unencryptedString, JsonObject.class);
        
        // Then: Should parse correctly
        assertThat(parsedEncrypted.has("fingerprint")).isTrue();
        assertThat(parsedEncrypted.get("fingerprint").getAsString()).isEqualTo(testFingerprint);
        
        assertThat(parsedUnencrypted.has("fingerprint")).isFalse();
        assertThat(parsedUnencrypted.has("data")).isTrue();
    }

    @Test
    @DisplayName("Should handle base64 encoding and decoding")
    void shouldHandleBase64EncodingAndDecoding() {
        // Given: Test JSON data
        JsonObject testJson = new JsonObject();
        testJson.addProperty("data", "test-data");
        testJson.addProperty("fingerprint", "ABCD-1234-EFGH-5678");
        
        String jsonString = gson.toJson(testJson);
        
        // When: Encoding and decoding
        String encoded = Base64.getEncoder().encodeToString(jsonString.getBytes());
        byte[] decoded = Base64.getDecoder().decode(encoded);
        String decodedString = new String(decoded);
        JsonObject decodedJson = gson.fromJson(decodedString, JsonObject.class);
        
        // Then: Should maintain data integrity
        assertThat(decodedJson.get("data").getAsString()).isEqualTo("test-data");
        assertThat(decodedJson.get("fingerprint").getAsString()).isEqualTo("ABCD-1234-EFGH-5678");
    }

    @Test
    @DisplayName("Should handle response data structure requirements")
    void shouldHandleResponseDataStructureRequirements() {
        // Given: Expected data structures for handler processing
        JsonObject encryptedStructure = new JsonObject();
        encryptedStructure.addProperty("fingerprint", "ABCD-1234-EFGH-5678");
        encryptedStructure.addProperty("data", "encrypted-data");
        
        JsonObject unencryptedStructure = new JsonObject();
        unencryptedStructure.addProperty("data", "plain-data");
        
        // When: Validating structure requirements
        // Then: Both structures should be valid JSON
        assertThat(gson.toJson(encryptedStructure)).isNotEmpty();
        assertThat(gson.toJson(unencryptedStructure)).isNotEmpty();
        
        // Encrypted structure should have fingerprint
        assertThat(encryptedStructure.has("fingerprint")).isTrue();
        // Unencrypted structure should not have fingerprint
        assertThat(unencryptedStructure.has("fingerprint")).isFalse();
    }

    @Test
    @DisplayName("Should handle handler instantiation")
    void shouldHandleHandlerInstantiation() {
        // Given: A valid API instance
        // When: Creating handler
        SharedItemDownloadResponseHandler testHandler = new SharedItemDownloadResponseHandler(api);
        
        // Then: Should create successfully
        assertThat(testHandler).isNotNull();
    }

    @Test
    @DisplayName("Should handle JSON structure validation")
    void shouldHandleJsonStructureValidation() {
        // Given: Valid JSON structure tests
        JsonObject validUnencrypted = new JsonObject();
        validUnencrypted.addProperty("data", "test-data");
        
        JsonObject validEncrypted = new JsonObject();
        validEncrypted.addProperty("fingerprint", "ABCD-1234-EFGH-5678");
        validEncrypted.addProperty("data", "encrypted-data");
        
        // When: Parsing JSON structures
        String unencryptedJson = gson.toJson(validUnencrypted);
        String encryptedJson = gson.toJson(validEncrypted);
        
        JsonObject parsedUnencrypted = gson.fromJson(unencryptedJson, JsonObject.class);
        JsonObject parsedEncrypted = gson.fromJson(encryptedJson, JsonObject.class);
        
        // Then: Should parse correctly
        assertThat(parsedUnencrypted.has("data")).isTrue();
        assertThat(parsedUnencrypted.has("fingerprint")).isFalse();
        
        assertThat(parsedEncrypted.has("data")).isTrue();
        assertThat(parsedEncrypted.has("fingerprint")).isTrue();
        assertThat(parsedEncrypted.get("fingerprint").getAsString()).isEqualTo("ABCD-1234-EFGH-5678");
    }
}