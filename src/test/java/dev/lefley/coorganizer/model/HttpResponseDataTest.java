package dev.lefley.coorganizer.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("HttpResponseData")
class HttpResponseDataTest {

    @Test
    @DisplayName("should create instance with all fields")
    void shouldCreateInstanceWithAllFields() {
        // Given
        String statusCode = "200";
        String headers = "Content-Type: application/json";
        String body = "{\"success\": true}";

        // When
        HttpResponseData responseData = new HttpResponseData(statusCode, headers, body);

        // Then
        assertThat(responseData.statusCode).isEqualTo(statusCode);
        assertThat(responseData.headers).isEqualTo(headers);
        assertThat(responseData.body).isEqualTo(body);
    }

    @Test
    @DisplayName("should handle different status codes")
    void shouldHandleDifferentStatusCodes() {
        // Given/When/Then
        HttpResponseData response200 = new HttpResponseData("200", "headers", "body");
        assertThat(response200.statusCode).isEqualTo("200");

        HttpResponseData response404 = new HttpResponseData("404", "headers", "body");
        assertThat(response404.statusCode).isEqualTo("404");

        HttpResponseData response500 = new HttpResponseData("500", "headers", "body");
        assertThat(response500.statusCode).isEqualTo("500");
    }

    @Test
    @DisplayName("should handle null values")
    void shouldHandleNullValues() {
        // When
        HttpResponseData responseData = new HttpResponseData(null, null, null);

        // Then
        assertThat(responseData.statusCode).isNull();
        assertThat(responseData.headers).isNull();
        assertThat(responseData.body).isNull();
    }
}