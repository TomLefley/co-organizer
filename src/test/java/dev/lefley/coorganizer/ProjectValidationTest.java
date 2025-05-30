package dev.lefley.coorganizer;

import dev.lefley.coorganizer.model.HttpRequestData;
import dev.lefley.coorganizer.model.HttpRequestResponseData;
import dev.lefley.coorganizer.model.HttpResponseData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Project Validation Test")
class ProjectValidationTest {

    @Test
    @DisplayName("should properly encode and decode data in model classes")
    void shouldProperlyEncodeAndDecodeDataInModelClasses() {
        // Given
        String originalMethod = "POST";
        String originalUrl = "https://example.com/api/test";
        String originalHeaders = "Content-Type: application/json\r\nAuthorization: Bearer token123";
        String originalBody = "{\"name\": \"Test User\", \"email\": \"test@example.com\"}";
        
        // Encode as base64 (simulating serialization process)
        String encodedMethod = Base64.getEncoder().encodeToString(originalMethod.getBytes());
        String encodedUrl = Base64.getEncoder().encodeToString(originalUrl.getBytes());
        String encodedHeaders = Base64.getEncoder().encodeToString(originalHeaders.getBytes());
        String encodedBody = Base64.getEncoder().encodeToString(originalBody.getBytes());

        // When - create request data with encoded values
        HttpRequestData requestData = new HttpRequestData(encodedMethod, encodedUrl, encodedHeaders, encodedBody);
        
        // And - create response data
        String statusCode = Base64.getEncoder().encodeToString("201".getBytes());
        String responseHeaders = Base64.getEncoder().encodeToString("Location: /api/test/123".getBytes());
        String responseBody = Base64.getEncoder().encodeToString("{\"id\": 123, \"status\": \"created\"}".getBytes());
        
        HttpResponseData responseData = new HttpResponseData(statusCode, responseHeaders, responseBody);
        
        // And - create combined data
        HttpRequestResponseData combinedData = new HttpRequestResponseData(requestData, responseData);

        // Then - verify data integrity
        assertThat(combinedData.request).isNotNull();
        assertThat(combinedData.response).isNotNull();
        
        // And - verify encoded data can be decoded back
        assertThat(new String(Base64.getDecoder().decode(combinedData.request.method))).isEqualTo(originalMethod);
        assertThat(new String(Base64.getDecoder().decode(combinedData.request.url))).isEqualTo(originalUrl);
        assertThat(new String(Base64.getDecoder().decode(combinedData.request.headers))).isEqualTo(originalHeaders);
        assertThat(new String(Base64.getDecoder().decode(combinedData.request.body))).isEqualTo(originalBody);
        
        assertThat(new String(Base64.getDecoder().decode(combinedData.response.statusCode))).isEqualTo("201");
        assertThat(new String(Base64.getDecoder().decode(combinedData.response.headers))).contains("Location");
        assertThat(new String(Base64.getDecoder().decode(combinedData.response.body))).contains("\"id\": 123");
    }

    @Test
    @DisplayName("should handle complex HTTP data correctly")
    void shouldHandleComplexHttpDataCorrectly() {
        // Given - complex HTTP request/response scenario
        String complexMethod = "PATCH";
        String complexUrl = "https://api.example.com/users/123?include=profile&format=json";
        String complexHeaders = "Content-Type: application/json\r\n" +
                               "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9\r\n" +
                               "User-Agent: BurpSuite/2023.1\r\n" +
                               "Accept: application/json, */*\r\n" +
                               "Accept-Encoding: gzip, deflate";
        String complexBody = "{\n" +
                           "  \"name\": \"Updated User\",\n" +
                           "  \"email\": \"updated@example.com\",\n" +
                           "  \"profile\": {\n" +
                           "    \"bio\": \"This is a test bio with special chars: <>&\\\"'\",\n" +
                           "    \"tags\": [\"developer\", \"tester\", \"security\"]\n" +
                           "  }\n" +
                           "}";

        // When - create and encode data
        HttpRequestData requestData = new HttpRequestData(
            Base64.getEncoder().encodeToString(complexMethod.getBytes()),
            Base64.getEncoder().encodeToString(complexUrl.getBytes()),
            Base64.getEncoder().encodeToString(complexHeaders.getBytes()),
            Base64.getEncoder().encodeToString(complexBody.getBytes())
        );

        HttpResponseData responseData = new HttpResponseData(
            Base64.getEncoder().encodeToString("200".getBytes()),
            Base64.getEncoder().encodeToString("Content-Type: application/json\r\nCache-Control: no-cache".getBytes()),
            Base64.getEncoder().encodeToString("{\"success\": true, \"updated_at\": \"2023-01-01T00:00:00Z\"}".getBytes())
        );

        HttpRequestResponseData data = new HttpRequestResponseData(requestData, responseData);

        // Then - verify complex data is preserved
        assertThat(data.request).isNotNull();
        assertThat(data.response).isNotNull();
        
        // Decode and verify
        String decodedMethod = new String(Base64.getDecoder().decode(data.request.method));
        String decodedUrl = new String(Base64.getDecoder().decode(data.request.url));
        String decodedBody = new String(Base64.getDecoder().decode(data.request.body));
        
        assertThat(decodedMethod).isEqualTo(complexMethod);
        assertThat(decodedUrl).contains("include=profile");
        assertThat(decodedBody).contains("special chars");
        assertThat(decodedBody).contains("developer");
    }

    @Test
    @DisplayName("should handle edge cases in data models")
    void shouldHandleEdgeCasesInDataModels() {
        // Given - edge case data
        String emptyString = "";
        String largeString = "x".repeat(10000);
        String unicodeString = "测试数据 🚀 тест データ";
        String binaryString = "binary\0data\255\127";

        // When - create data with edge cases
        HttpRequestData requestData = new HttpRequestData(
            Base64.getEncoder().encodeToString(emptyString.getBytes()),
            Base64.getEncoder().encodeToString(largeString.getBytes()),
            Base64.getEncoder().encodeToString(unicodeString.getBytes()),
            Base64.getEncoder().encodeToString(binaryString.getBytes())
        );

        HttpResponseData responseData = new HttpResponseData(
            Base64.getEncoder().encodeToString("0".getBytes()), // Edge case: status code 0
            Base64.getEncoder().encodeToString(emptyString.getBytes()),
            Base64.getEncoder().encodeToString(unicodeString.getBytes())
        );

        HttpRequestResponseData data = new HttpRequestResponseData(requestData, responseData);

        // Then - verify edge cases are handled
        assertThat(data.request.method).isNotNull();
        assertThat(data.request.url).isNotNull();
        assertThat(data.response.statusCode).isNotNull();
        
        // Verify decoding works for edge cases
        String decodedMethod = new String(Base64.getDecoder().decode(data.request.method));
        String decodedUrl = new String(Base64.getDecoder().decode(data.request.url));
        String decodedHeaders = new String(Base64.getDecoder().decode(data.request.headers));
        String decodedBody = new String(Base64.getDecoder().decode(data.request.body));

        assertThat(decodedMethod).isEmpty();
        assertThat(decodedUrl).hasSize(10000);
        assertThat(decodedHeaders).contains("🚀");
        assertThat(decodedBody).contains("\0");
    }

    @Test
    @DisplayName("should maintain data consistency across operations")
    void shouldMaintainDataConsistencyAcrossOperations() {
        // Given - original data
        HttpRequestData originalRequest = new HttpRequestData("GET", "https://example.com", "headers", "body");
        HttpResponseData originalResponse = new HttpResponseData("200", "headers", "body");
        HttpRequestResponseData originalData = new HttpRequestResponseData(originalRequest, originalResponse);

        // When - create multiple references
        HttpRequestResponseData copiedData = new HttpRequestResponseData(originalData.request, originalData.response);

        // Then - verify reference consistency
        assertThat(copiedData.request).isSameAs(originalData.request);
        assertThat(copiedData.response).isSameAs(originalData.response);
        
        // And - verify data integrity
        assertThat(copiedData.request.method).isEqualTo(originalRequest.method);
        assertThat(copiedData.request.url).isEqualTo(originalRequest.url);
        assertThat(copiedData.response.statusCode).isEqualTo(originalResponse.statusCode);
    }
}