package dev.lefley.coorganizer.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("HttpRequestData")
class HttpRequestDataTest {

    @Test
    @DisplayName("should create instance with all fields")
    void shouldCreateInstanceWithAllFields() {
        // Given
        String method = "POST";
        String url = "https://example.com/api";
        String headers = "Content-Type: application/json";
        String body = "{\"test\": \"data\"}";

        // When
        HttpRequestData requestData = new HttpRequestData(method, url, headers, body);

        // Then
        assertThat(requestData.method).isEqualTo(method);
        assertThat(requestData.url).isEqualTo(url);
        assertThat(requestData.headers).isEqualTo(headers);
        assertThat(requestData.body).isEqualTo(body);
    }

    @Test
    @DisplayName("should handle null values")
    void shouldHandleNullValues() {
        // When
        HttpRequestData requestData = new HttpRequestData(null, null, null, null);

        // Then
        assertThat(requestData.method).isNull();
        assertThat(requestData.url).isNull();
        assertThat(requestData.headers).isNull();
        assertThat(requestData.body).isNull();
    }

    @Test
    @DisplayName("should handle empty strings")
    void shouldHandleEmptyStrings() {
        // When
        HttpRequestData requestData = new HttpRequestData("", "", "", "");

        // Then
        assertThat(requestData.method).isEmpty();
        assertThat(requestData.url).isEmpty();
        assertThat(requestData.headers).isEmpty();
        assertThat(requestData.body).isEmpty();
    }
}