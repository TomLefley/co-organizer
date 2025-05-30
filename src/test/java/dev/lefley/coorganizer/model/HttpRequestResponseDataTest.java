package dev.lefley.coorganizer.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("HttpRequestResponseData")
class HttpRequestResponseDataTest {

    @Test
    @DisplayName("should create instance with request and response")
    void shouldCreateInstanceWithRequestAndResponse() {
        // Given
        HttpRequestData requestData = new HttpRequestData("GET", "https://example.com", "headers", "body");
        HttpResponseData responseData = new HttpResponseData("200", "response-headers", "response-body");

        // When
        HttpRequestResponseData data = new HttpRequestResponseData(requestData, responseData);

        // Then
        assertThat(data.request).isEqualTo(requestData);
        assertThat(data.response).isEqualTo(responseData);
    }

    @Test
    @DisplayName("should handle null request")
    void shouldHandleNullRequest() {
        // Given
        HttpResponseData responseData = new HttpResponseData("200", "headers", "body");

        // When
        HttpRequestResponseData data = new HttpRequestResponseData(null, responseData);

        // Then
        assertThat(data.request).isNull();
        assertThat(data.response).isEqualTo(responseData);
    }

    @Test
    @DisplayName("should handle null response")
    void shouldHandleNullResponse() {
        // Given
        HttpRequestData requestData = new HttpRequestData("GET", "https://example.com", "headers", "body");

        // When
        HttpRequestResponseData data = new HttpRequestResponseData(requestData, null);

        // Then
        assertThat(data.request).isEqualTo(requestData);
        assertThat(data.response).isNull();
    }

    @Test
    @DisplayName("should maintain reference integrity")
    void shouldMaintainReferenceIntegrity() {
        // Given
        HttpRequestData requestData = new HttpRequestData("GET", "url", "headers", "body");
        HttpResponseData responseData = new HttpResponseData("200", "headers", "body");

        // When
        HttpRequestResponseData data = new HttpRequestResponseData(requestData, responseData);

        // Then
        assertThat(data.request).isSameAs(requestData);
        assertThat(data.response).isSameAs(responseData);
    }
}