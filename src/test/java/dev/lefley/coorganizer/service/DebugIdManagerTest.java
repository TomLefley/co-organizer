package dev.lefley.coorganizer.service;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.persistence.Persistence;
import burp.api.montoya.persistence.Preferences;
import burp.api.montoya.logging.Logging;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

@DisplayName("DebugIdManager Tests")
class DebugIdManagerTest {

    @Mock
    private MontoyaApi api;
    
    @Mock
    private Logging logging;
    
    @Mock
    private Persistence persistence;
    
    @Mock
    private Preferences preferences;

    private static final String DEBUG_ID_KEY = "co-organizer.debug-id";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        when(api.logging()).thenReturn(logging);
        when(api.persistence()).thenReturn(persistence);
        when(persistence.preferences()).thenReturn(preferences);
    }

    @Test
    @DisplayName("Should generate new debug ID when preference doesn't exist")
    void shouldGenerateNewDebugIdWhenPreferenceDoesNotExist() {
        // Given: No existing preference
        when(preferences.getString(DEBUG_ID_KEY)).thenReturn(null);
        
        // When: Creating DebugIdManager
        DebugIdManager debugIdManager = new DebugIdManager(api);
        
        // Then: Should generate and store a new UUID
        verify(preferences).setString(eq(DEBUG_ID_KEY), anyString());
        assertThat(debugIdManager.getDebugId()).isNotNull();
        assertThat(debugIdManager.getDebugId()).hasSize(36); // UUID format
        assertThat(debugIdManager.isDebugIdEnabled()).isTrue();
    }

    @Test
    @DisplayName("Should use existing debug ID when preference exists")
    void shouldUseExistingDebugIdWhenPreferenceExists() {
        // Given: Existing debug ID preference
        String existingDebugId = "12345678-1234-1234-1234-123456789012";
        when(preferences.getString(DEBUG_ID_KEY)).thenReturn(existingDebugId);
        
        // When: Creating DebugIdManager
        DebugIdManager debugIdManager = new DebugIdManager(api);
        
        // Then: Should use existing debug ID without generating new one
        verify(preferences, never()).setString(eq(DEBUG_ID_KEY), anyString());
        assertThat(debugIdManager.getDebugId()).isEqualTo(existingDebugId);
        assertThat(debugIdManager.isDebugIdEnabled()).isTrue();
    }

    @Test
    @DisplayName("Should respect empty debug ID preference for privacy")
    void shouldRespectEmptyDebugIdPreferenceForPrivacy() {
        // Given: Empty debug ID preference (user cleared it for privacy)
        when(preferences.getString(DEBUG_ID_KEY)).thenReturn("");
        
        // When: Creating DebugIdManager
        DebugIdManager debugIdManager = new DebugIdManager(api);
        
        // Then: Should respect empty preference and not generate new ID
        verify(preferences, never()).setString(eq(DEBUG_ID_KEY), anyString());
        assertThat(debugIdManager.getDebugId()).isEmpty();
        assertThat(debugIdManager.isDebugIdEnabled()).isFalse();
    }

    @Test
    @DisplayName("Should clear debug ID and store empty string")
    void shouldClearDebugIdAndStoreEmptyString() {
        // Given: DebugIdManager with existing debug ID
        String existingDebugId = "12345678-1234-1234-1234-123456789012";
        when(preferences.getString(DEBUG_ID_KEY)).thenReturn(existingDebugId);
        DebugIdManager debugIdManager = new DebugIdManager(api);
        
        // When: Clearing debug ID
        debugIdManager.clearDebugId();
        
        // Then: Should set debug ID to empty and store it
        verify(preferences).setString(DEBUG_ID_KEY, "");
        assertThat(debugIdManager.getDebugId()).isEmpty();
        assertThat(debugIdManager.isDebugIdEnabled()).isFalse();
    }

    @Test
    @DisplayName("Should regenerate debug ID and store new value")
    void shouldRegenerateDebugIdAndStoreNewValue() {
        // Given: DebugIdManager with existing debug ID
        String existingDebugId = "12345678-1234-1234-1234-123456789012";
        when(preferences.getString(DEBUG_ID_KEY)).thenReturn(existingDebugId);
        DebugIdManager debugIdManager = new DebugIdManager(api);
        
        // When: Regenerating debug ID
        String newDebugId = debugIdManager.regenerateDebugId();
        
        // Then: Should generate new UUID and store it
        verify(preferences).setString(eq(DEBUG_ID_KEY), eq(newDebugId));
        assertThat(newDebugId).isNotNull();
        assertThat(newDebugId).hasSize(36); // UUID format
        assertThat(newDebugId).isNotEqualTo(existingDebugId);
        assertThat(debugIdManager.getDebugId()).isEqualTo(newDebugId);
        assertThat(debugIdManager.isDebugIdEnabled()).isTrue();
    }

    @Test
    @DisplayName("Should correctly identify when debug ID is enabled")
    void shouldCorrectlyIdentifyWhenDebugIdIsEnabled() {
        // Test with valid UUID
        when(preferences.getString(DEBUG_ID_KEY)).thenReturn("12345678-1234-1234-1234-123456789012");
        DebugIdManager enabledManager = new DebugIdManager(api);
        assertThat(enabledManager.isDebugIdEnabled()).isTrue();
        
        // Test with empty string
        when(preferences.getString(DEBUG_ID_KEY)).thenReturn("");
        DebugIdManager disabledManager = new DebugIdManager(api);
        assertThat(disabledManager.isDebugIdEnabled()).isFalse();
    }

    @Test
    @DisplayName("Should handle debug ID preference key correctly")
    void shouldHandleDebugIdPreferenceKeyCorrectly() {
        // Given: Creating DebugIdManager
        when(preferences.getString(DEBUG_ID_KEY)).thenReturn(null);
        
        // When: Initializing
        new DebugIdManager(api);
        
        // Then: Should use correct preference key
        verify(preferences).getString("co-organizer.debug-id");
        verify(preferences).setString(eq("co-organizer.debug-id"), anyString());
    }

    @Test
    @DisplayName("Should generate valid UUID format")
    void shouldGenerateValidUuidFormat() {
        // Given: No existing preference
        when(preferences.getString(DEBUG_ID_KEY)).thenReturn(null);
        
        // When: Creating DebugIdManager
        DebugIdManager debugIdManager = new DebugIdManager(api);
        
        // Then: Should generate valid UUID format (8-4-4-4-12 characters with hyphens)
        String debugId = debugIdManager.getDebugId();
        assertThat(debugId).matches("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");
    }

    @Test
    @DisplayName("Should generate different UUIDs on multiple regenerations")
    void shouldGenerateDifferentUuidsOnMultipleRegenerations() {
        // Given: DebugIdManager with initial debug ID
        when(preferences.getString(DEBUG_ID_KEY)).thenReturn("initial-uuid");
        DebugIdManager debugIdManager = new DebugIdManager(api);
        
        // When: Regenerating multiple times
        String uuid1 = debugIdManager.regenerateDebugId();
        String uuid2 = debugIdManager.regenerateDebugId();
        String uuid3 = debugIdManager.regenerateDebugId();
        
        // Then: All UUIDs should be different
        assertThat(uuid1).isNotEqualTo(uuid2);
        assertThat(uuid2).isNotEqualTo(uuid3);
        assertThat(uuid1).isNotEqualTo(uuid3);
        
        // All should be valid UUIDs
        assertThat(uuid1).matches("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");
        assertThat(uuid2).matches("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");
        assertThat(uuid3).matches("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");
    }
}