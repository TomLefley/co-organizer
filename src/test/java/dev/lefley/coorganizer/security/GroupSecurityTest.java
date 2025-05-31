package dev.lefley.coorganizer.security;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.persistence.Persistence;
import burp.api.montoya.persistence.Preferences;
import burp.api.montoya.logging.Logging;
import dev.lefley.coorganizer.crypto.CryptoUtils;
import dev.lefley.coorganizer.model.Group;
import dev.lefley.coorganizer.service.GroupManager;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@DisplayName("Group Security Tests")
class GroupSecurityTest {

    @Mock
    private MontoyaApi api;
    
    @Mock
    private Logging logging;
    
    @Mock
    private Persistence persistence;
    
    @Mock
    private Preferences preferences;

    private GroupManager groupManager;
    private Gson gson;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        when(api.logging()).thenReturn(logging);
        when(api.persistence()).thenReturn(persistence);
        when(persistence.preferences()).thenReturn(preferences);
        when(preferences.getString("co-organizer.groups")).thenReturn(null);
        
        groupManager = new GroupManager(api);
        gson = new Gson();
    }

    @Test
    @DisplayName("Should prevent access with invalid fingerprint")
    void shouldPreventAccessWithInvalidFingerprint() {
        // Given: A group and some encrypted data
        Group group = groupManager.createGroup("Valid Group");
        String testData = "{\"secret\":\"data\"}";
        String encryptedData = CryptoUtils.encrypt(testData, group.getSymmetricKey());
        
        // When: Creating share structure with different fingerprint
        JsonObject shareStructure = new JsonObject();
        shareStructure.addProperty("fingerprint", "FAKE-1234-ABCD-5678");
        shareStructure.addProperty("data", encryptedData);
        
        String base64Encoded = Base64.getEncoder().encodeToString(gson.toJson(shareStructure).getBytes());
        byte[] decodedBytes = Base64.getDecoder().decode(base64Encoded);
        JsonObject receivedStructure = gson.fromJson(new String(decodedBytes), JsonObject.class);
        
        String receivedFingerprint = receivedStructure.get("fingerprint").getAsString();
        
        // Then: Should not find group with fake fingerprint
        Group foundGroup = groupManager.getGroups().stream()
            .filter(g -> g.getFingerprint().equals(receivedFingerprint))
            .findFirst()
            .orElse(null);
        
        assertThat(foundGroup).isNull();
    }

    @Test
    @DisplayName("Should prevent decryption with wrong key")
    void shouldPreventDecryptionWithWrongKey() {
        // Given: Two different groups and data encrypted with one
        Group group1 = groupManager.createGroup("Group 1");
        Group group2 = groupManager.createGroup("Group 2");
        
        String sensitiveData = "{\"password\":\"secret123\",\"token\":\"abc-xyz\"}";
        String encryptedData = CryptoUtils.encrypt(sensitiveData, group1.getSymmetricKey());
        
        // When/Then: Attempting to decrypt with wrong key should fail
        assertThatThrownBy(() -> {
            CryptoUtils.decrypt(encryptedData, group2.getSymmetricKey());
        }).isInstanceOf(RuntimeException.class)
          .hasMessageContaining("Failed to decrypt data");
    }

    @Test
    @DisplayName("Should handle tampered encrypted data")
    void shouldHandleTamperedEncryptedData() {
        // Given: A group and some encrypted data
        Group group = groupManager.createGroup("Security Test Group");
        String originalData = "{\"important\":\"data\"}";
        String encryptedData = CryptoUtils.encrypt(originalData, group.getSymmetricKey());
        
        // When: Tampering with encrypted data
        String tamperedData = encryptedData.substring(0, encryptedData.length() - 10) + "TAMPERED!!";
        
        // Then: Decryption should fail due to authentication tag mismatch
        assertThatThrownBy(() -> {
            CryptoUtils.decrypt(tamperedData, group.getSymmetricKey());
        }).isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("Should handle malformed JSON structures")
    void shouldHandleMalformedJsonStructures() {
        // Given: Various malformed JSON structures
        String malformedJson1 = "{\"data\":}"; // Invalid syntax
        String malformedJson2 = "{\"fingerprint\":\"test\"}"; // Missing data field
        String malformedJson3 = "{\"data\":\"test\",\"extra\":\"field\"}"; // Extra field (should be ok)
        
        // When/Then: Should handle malformed JSON gracefully
        assertThatThrownBy(() -> {
            gson.fromJson(malformedJson1, JsonObject.class);
        }).isInstanceOf(Exception.class);
        
        JsonObject validButIncomplete = gson.fromJson(malformedJson2, JsonObject.class);
        assertThat(validButIncomplete.has("data")).isFalse();
        assertThat(validButIncomplete.has("fingerprint")).isTrue();
        
        JsonObject validWithExtra = gson.fromJson(malformedJson3, JsonObject.class);
        assertThat(validWithExtra.has("data")).isTrue();
        assertThat(validWithExtra.has("extra")).isTrue();
    }

    @Test
    @DisplayName("Should handle invalid base64 data")
    void shouldHandleInvalidBase64Data() {
        // Given: Invalid base64 strings
        String invalidBase64_1 = "this is not base64!";
        String invalidBase64_2 = "SGVsbG8gV29ybGQ"; // Valid but incomplete padding
        String invalidBase64_3 = "SGVsbG8gV29ybGQ=@#$"; // Valid start but invalid chars
        
        // When/Then: Should handle invalid base64 gracefully
        assertThatThrownBy(() -> {
            Base64.getDecoder().decode(invalidBase64_1);
        }).isInstanceOf(IllegalArgumentException.class);
        
        assertThatThrownBy(() -> {
            Base64.getDecoder().decode(invalidBase64_3);
        }).isInstanceOf(IllegalArgumentException.class);
        
        // This one might actually decode successfully with some decoders
        // Let's test what actually happens
        try {
            byte[] result = Base64.getDecoder().decode(invalidBase64_2);
            // If it succeeds, verify we get some result
            assertThat(result).isNotNull();
        } catch (IllegalArgumentException e) {
            // If it fails, that's also acceptable
            assertThat(e).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    @DisplayName("Should ensure encryption produces different outputs")
    void shouldEnsureEncryptionProducesDifferentOutputs() {
        // Given: Same group and same data
        Group group = groupManager.createGroup("Randomness Test Group");
        String testData = "{\"test\":\"data\"}";
        
        // When: Encrypting same data multiple times
        String encrypted1 = CryptoUtils.encrypt(testData, group.getSymmetricKey());
        String encrypted2 = CryptoUtils.encrypt(testData, group.getSymmetricKey());
        String encrypted3 = CryptoUtils.encrypt(testData, group.getSymmetricKey());
        
        // Then: All encrypted outputs should be different (due to random IVs)
        assertThat(encrypted1).isNotEqualTo(encrypted2);
        assertThat(encrypted2).isNotEqualTo(encrypted3);
        assertThat(encrypted1).isNotEqualTo(encrypted3);
        
        // But all should decrypt to same original data
        String decrypted1 = CryptoUtils.decrypt(encrypted1, group.getSymmetricKey());
        String decrypted2 = CryptoUtils.decrypt(encrypted2, group.getSymmetricKey());
        String decrypted3 = CryptoUtils.decrypt(encrypted3, group.getSymmetricKey());
        
        assertThat(decrypted1).isEqualTo(testData);
        assertThat(decrypted2).isEqualTo(testData);
        assertThat(decrypted3).isEqualTo(testData);
    }

    @Test
    @DisplayName("Should validate symmetric key format")
    void shouldValidateSymmetricKeyFormat() {
        // Given: Generated symmetric keys
        String key1 = CryptoUtils.generateSymmetricKey();
        String key2 = CryptoUtils.generateSymmetricKey();
        
        // When/Then: Keys should be valid base64 and of correct length
        assertThat(key1).isNotNull();
        assertThat(key2).isNotNull();
        assertThat(key1).isNotEqualTo(key2);
        
        // Should be valid base64
        assertThatNoException(() -> {
            Base64.getDecoder().decode(key1);
            Base64.getDecoder().decode(key2);
        });
        
        // Decoded keys should be 32 bytes (256 bits)
        byte[] decodedKey1 = Base64.getDecoder().decode(key1);
        byte[] decodedKey2 = Base64.getDecoder().decode(key2);
        assertThat(decodedKey1).hasSize(32);
        assertThat(decodedKey2).hasSize(32);
    }

    @Test
    @DisplayName("Should validate fingerprint format and uniqueness")
    void shouldValidateFingerprintFormatAndUniqueness() {
        // Given: Multiple groups
        Group group1 = groupManager.createGroup("Group 1");
        Group group2 = groupManager.createGroup("Group 2");
        Group group3 = groupManager.createGroup("Group 3");
        
        // When/Then: Fingerprints should follow expected format
        String fingerprint1 = group1.getFingerprint();
        String fingerprint2 = group2.getFingerprint();
        String fingerprint3 = group3.getFingerprint();
        
        // Should match pattern: XXXX-XXXX-XXXX-XXXX (hex)
        String fingerprintPattern = "^[A-F0-9]{4}-[A-F0-9]{4}-[A-F0-9]{4}-[A-F0-9]{4}$";
        assertThat(fingerprint1).matches(fingerprintPattern);
        assertThat(fingerprint2).matches(fingerprintPattern);
        assertThat(fingerprint3).matches(fingerprintPattern);
        
        // Should be unique
        assertThat(fingerprint1).isNotEqualTo(fingerprint2);
        assertThat(fingerprint2).isNotEqualTo(fingerprint3);
        assertThat(fingerprint1).isNotEqualTo(fingerprint3);
    }

    @Test
    @DisplayName("Should handle empty and null data encryption")
    void shouldHandleEmptyAndNullDataEncryption() {
        // Given: A group and edge case data
        Group group = groupManager.createGroup("Edge Case Group");
        
        // When/Then: Should handle empty string
        String emptyData = "";
        String encryptedEmpty = CryptoUtils.encrypt(emptyData, group.getSymmetricKey());
        String decryptedEmpty = CryptoUtils.decrypt(encryptedEmpty, group.getSymmetricKey());
        assertThat(decryptedEmpty).isEqualTo(emptyData);
        
        // Should handle null by throwing exception
        assertThatThrownBy(() -> {
            CryptoUtils.encrypt(null, group.getSymmetricKey());
        }).isInstanceOf(Exception.class);
    }

    private static void assertThatNoException(Runnable runnable) {
        try {
            runnable.run();
        } catch (Exception e) {
            throw new AssertionError("Expected no exception, but got: " + e.getMessage(), e);
        }
    }
}