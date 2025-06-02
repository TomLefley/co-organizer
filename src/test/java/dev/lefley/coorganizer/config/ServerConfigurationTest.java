package dev.lefley.coorganizer.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ServerConfiguration Tests")
class ServerConfigurationTest {

    @Test
    @DisplayName("Should provide correct default configuration values")
    void shouldProvideCorrectDefaultConfigurationValues() {
        assertThat(ServerConfiguration.HOST).isEqualTo("localhost");
        assertThat(ServerConfiguration.PORT).isEqualTo(3000);
        assertThat(ServerConfiguration.SHARE_ENDPOINT).isEqualTo("/share");
        assertThat(ServerConfiguration.IMPORT_ENDPOINT_SUFFIX).isEqualTo("/import");
    }

    @Test
    @DisplayName("Should construct correct base URL")
    void shouldConstructCorrectBaseUrl() {
        String expectedUrl = "http://" + ServerConfiguration.HOST + ":" + ServerConfiguration.PORT;
        assertThat(ServerConfiguration.BASE_URL).isEqualTo(expectedUrl);
        assertThat(ServerConfiguration.BASE_URL).isEqualTo("http://localhost:3000");
    }

    @Test
    @DisplayName("Should provide consistent endpoint paths")
    void shouldProvideConsistentEndpointPaths() {
        assertThat(ServerConfiguration.SHARE_ENDPOINT).startsWith("/");
        assertThat(ServerConfiguration.IMPORT_ENDPOINT_SUFFIX).startsWith("/");
    }

    @Test
    @DisplayName("Should prevent instantiation")
    void shouldPreventInstantiation() {
        assertThatThrownBy(() -> {
            // Use reflection to try to instantiate the utility class
            ServerConfiguration.class.getDeclaredConstructor().newInstance();
        }).hasCauseInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("Should have non-null configuration values")
    void shouldHaveNonNullConfigurationValues() {
        assertThat(ServerConfiguration.HOST).isNotNull().isNotEmpty();
        assertThat(ServerConfiguration.BASE_URL).isNotNull().isNotEmpty();
        assertThat(ServerConfiguration.SHARE_ENDPOINT).isNotNull().isNotEmpty();
        assertThat(ServerConfiguration.IMPORT_ENDPOINT_SUFFIX).isNotNull().isNotEmpty();
    }

    @Test
    @DisplayName("Should provide valid port number")
    void shouldProvideValidPortNumber() {
        assertThat(ServerConfiguration.PORT).isGreaterThan(0);
        assertThat(ServerConfiguration.PORT).isLessThanOrEqualTo(65535);
    }
}