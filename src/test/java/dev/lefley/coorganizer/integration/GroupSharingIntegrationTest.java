package dev.lefley.coorganizer.integration;

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

@DisplayName("Group Sharing Integration Tests")
class GroupSharingIntegrationTest {

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
    @DisplayName("Should complete full encryption and decryption flow")
    void shouldCompleteFullEncryptionAndDecryptionFlow() {
        // Given: A group and some test data
        Group group = groupManager.createGroup("Integration Test Group");
        String originalData = "{\"items\":[{\"id\":1,\"name\":\"test\"}],\"count\":1}";
        
        // When: Encrypting data for sharing
        String encryptedData = CryptoUtils.encrypt(originalData, group.getSymmetricKey());
        
        JsonObject shareStructure = new JsonObject();
        shareStructure.addProperty("fingerprint", group.getFingerprint());
        shareStructure.addProperty("data", encryptedData);
        
        String base64Encoded = Base64.getEncoder().encodeToString(gson.toJson(shareStructure).getBytes());
        
        // Simulate receiving the data
        byte[] decodedBytes = Base64.getDecoder().decode(base64Encoded);
        JsonObject receivedStructure = gson.fromJson(new String(decodedBytes), JsonObject.class);
        
        String receivedFingerprint = receivedStructure.get("fingerprint").getAsString();
        String receivedEncryptedData = receivedStructure.get("data").getAsString();
        
        // Find group by fingerprint (simulating download handler logic)
        Group foundGroup = groupManager.getGroups().stream()
            .filter(g -> g.getFingerprint().equals(receivedFingerprint))
            .findFirst()
            .orElse(null);
        
        assertThat(foundGroup).isNotNull();
        
        // Decrypt the data
        String decryptedData = CryptoUtils.decrypt(receivedEncryptedData, foundGroup.getSymmetricKey());
        
        // Then: Decrypted data should match original
        assertThat(decryptedData).isEqualTo(originalData);
    }

    @Test
    @DisplayName("Should handle unencrypted data structure")
    void shouldHandleUnencryptedDataStructure() {
        // Given: Unencrypted data structure
        String originalData = "{\"items\":[{\"id\":1}]}";
        
        JsonObject unencryptedStructure = new JsonObject();
        unencryptedStructure.addProperty("data", originalData);
        
        String base64Encoded = Base64.getEncoder().encodeToString(gson.toJson(unencryptedStructure).getBytes());
        
        // When: Processing the data
        byte[] decodedBytes = Base64.getDecoder().decode(base64Encoded);
        JsonObject receivedStructure = gson.fromJson(new String(decodedBytes), JsonObject.class);
        
        // Then: Should handle unencrypted data correctly
        assertThat(receivedStructure.has("fingerprint")).isFalse();
        assertThat(receivedStructure.has("data")).isTrue();
        
        String receivedData = receivedStructure.get("data").getAsString();
        assertThat(receivedData).isEqualTo(originalData);
    }

    @Test
    @DisplayName("Should reject access after leaving group")
    void shouldRejectAccessAfterLeavingGroup() {
        // Given: A group and encrypted data
        Group group = groupManager.createGroup("Temporary Group");
        String originalData = "{\"secret\":\"data\"}";
        String encryptedData = CryptoUtils.encrypt(originalData, group.getSymmetricKey());
        
        JsonObject shareStructure = new JsonObject();
        shareStructure.addProperty("fingerprint", group.getFingerprint());
        shareStructure.addProperty("data", encryptedData);
        
        // When: User leaves the group
        groupManager.leaveGroup(group);
        
        // Then: User should no longer have access to decrypt
        Group foundGroup = groupManager.getGroups().stream()
            .filter(g -> g.getFingerprint().equals(group.getFingerprint()))
            .findFirst()
            .orElse(null);
        
        assertThat(foundGroup).isNull();
        
        // Attempting to find the group for decryption should fail
        assertThat(groupManager.getGroups()).doesNotContain(group);
    }

    @Test
    @DisplayName("Should handle malformed encrypted data gracefully")
    void shouldHandleMalformedEncryptedDataGracefully() {
        // Given: A group and malformed encrypted data
        Group group = groupManager.createGroup("Test Group");
        String malformedEncryptedData = "this-is-not-encrypted-data";
        
        // When/Then: Decryption should fail gracefully
        assertThatThrownBy(() -> {
            CryptoUtils.decrypt(malformedEncryptedData, group.getSymmetricKey());
        }).isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("Should validate fingerprint matching")
    void shouldValidateFingerprintMatching() {
        // Given: Two different groups
        Group group1 = groupManager.createGroup("Group 1");
        Group group2 = groupManager.createGroup("Group 2");
        
        // When: Checking fingerprints
        // Then: Fingerprints should be different
        assertThat(group1.getFingerprint()).isNotEqualTo(group2.getFingerprint());
        
        // Fingerprints should be consistent
        assertThat(group1.getFingerprint()).isEqualTo(group1.getFingerprint());
        assertThat(group2.getFingerprint()).isEqualTo(group2.getFingerprint());
    }

    @Test
    @DisplayName("Should handle cross-group encryption isolation")
    void shouldHandleCrossGroupEncryptionIsolation() {
        // Given: Two groups and data encrypted with one group's key
        Group group1 = groupManager.createGroup("Group 1");
        Group group2 = groupManager.createGroup("Group 2");
        
        String testData = "{\"sensitive\":\"information\"}";
        String encryptedWithGroup1 = CryptoUtils.encrypt(testData, group1.getSymmetricKey());
        
        // When/Then: Attempting to decrypt with wrong group's key should fail
        assertThatThrownBy(() -> {
            CryptoUtils.decrypt(encryptedWithGroup1, group2.getSymmetricKey());
        }).isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("Should handle large encrypted payloads")
    void shouldHandleLargeEncryptedPayloads() {
        // Given: A group and large test data
        Group group = groupManager.createGroup("Large Data Group");
        
        StringBuilder largeData = new StringBuilder();
        largeData.append("{\"items\":[");
        for (int i = 0; i < 1000; i++) {
            if (i > 0) largeData.append(",");
            largeData.append("{\"id\":").append(i).append(",\"data\":\"")
                .append("x".repeat(100)).append("\"}");
        }
        largeData.append("]}");
        
        String originalData = largeData.toString();
        
        // When: Encrypting and decrypting large data
        String encrypted = CryptoUtils.encrypt(originalData, group.getSymmetricKey());
        String decrypted = CryptoUtils.decrypt(encrypted, group.getSymmetricKey());
        
        // Then: Should handle large payloads correctly
        assertThat(decrypted).isEqualTo(originalData);
        assertThat(encrypted).isNotEqualTo(originalData);
    }

    @Test
    @DisplayName("Should maintain data integrity through full sharing cycle")
    void shouldMaintainDataIntegrityThroughFullSharingCycle() {
        // Given: A group and complex test data
        Group group = groupManager.createGroup("Integrity Test Group");
        String originalData = "{\"nested\":{\"array\":[1,2,3],\"string\":\"test\",\"boolean\":true,\"null\":null}}";
        
        // When: Going through full sharing cycle
        // 1. Encrypt for sharing
        String encrypted = CryptoUtils.encrypt(originalData, group.getSymmetricKey());
        
        // 2. Create share structure
        JsonObject shareStructure = new JsonObject();
        shareStructure.addProperty("fingerprint", group.getFingerprint());
        shareStructure.addProperty("data", encrypted);
        
        // 3. Encode for transmission
        String encoded = Base64.getEncoder().encodeToString(gson.toJson(shareStructure).getBytes());
        
        // 4. Decode on receiving end
        byte[] decoded = Base64.getDecoder().decode(encoded);
        JsonObject received = gson.fromJson(new String(decoded), JsonObject.class);
        
        // 5. Find group and decrypt
        String fingerprint = received.get("fingerprint").getAsString();
        String encryptedData = received.get("data").getAsString();
        
        Group foundGroup = groupManager.getGroups().stream()
            .filter(g -> g.getFingerprint().equals(fingerprint))
            .findFirst()
            .orElse(null);
        
        assertThat(foundGroup).isNotNull();
        
        String decrypted = CryptoUtils.decrypt(encryptedData, foundGroup.getSymmetricKey());
        
        // Then: Data should remain identical
        assertThat(decrypted).isEqualTo(originalData);
    }
}