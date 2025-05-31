package dev.lefley.coorganizer.crypto;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CryptoUtils Tests")
class CryptoUtilsTest {

    @Test
    @DisplayName("Should encrypt and decrypt data successfully")
    void shouldEncryptAndDecryptData() {
        // Given: A test key and plaintext
        String key = CryptoUtils.generateSymmetricKey();
        String plaintext = "This is test data that should be encrypted and decrypted correctly.";
        
        // When: Encrypting and then decrypting
        String encrypted = CryptoUtils.encrypt(plaintext, key);
        String decrypted = CryptoUtils.decrypt(encrypted, key);
        
        // Then: Decrypted data should match original
        assertThat(decrypted).isEqualTo(plaintext);
        assertThat(encrypted).isNotEqualTo(plaintext);
        assertThat(encrypted).isNotBlank();
    }
    
    @Test
    @DisplayName("Should generate unique symmetric keys")
    void shouldGenerateUniqueSymmetricKeys() {
        // When: Generating multiple keys
        String key1 = CryptoUtils.generateSymmetricKey();
        String key2 = CryptoUtils.generateSymmetricKey();
        
        // Then: Keys should be different
        assertThat(key1).isNotEqualTo(key2);
        assertThat(key1).isNotBlank();
        assertThat(key2).isNotBlank();
    }
    
    @Test
    @DisplayName("Should generate consistent fingerprints")
    void shouldGenerateConsistentFingerprints() {
        // Given: Same group name and key
        String groupName = "Test Group";
        String key = "test-key";
        
        // When: Generating fingerprints multiple times
        String fingerprint1 = CryptoUtils.generateFingerprint(groupName, key);
        String fingerprint2 = CryptoUtils.generateFingerprint(groupName, key);
        
        // Then: Fingerprints should be identical
        assertThat(fingerprint1).isEqualTo(fingerprint2);
        assertThat(fingerprint1).matches("[A-F0-9]{4}-[A-F0-9]{4}-[A-F0-9]{4}-[A-F0-9]{4}");
    }
    
    @Test
    @DisplayName("Should handle large JSON data encryption")
    void shouldHandleLargeJsonDataEncryption() {
        // Given: A large JSON-like string and key
        String key = CryptoUtils.generateSymmetricKey();
        StringBuilder largeData = new StringBuilder();
        largeData.append("{\"items\":[");
        for (int i = 0; i < 100; i++) {
            if (i > 0) largeData.append(",");
            largeData.append("{\"id\":").append(i).append(",\"data\":\"").append("x".repeat(100)).append("\"}");
        }
        largeData.append("]}");
        
        // When: Encrypting and decrypting large data
        String encrypted = CryptoUtils.encrypt(largeData.toString(), key);
        String decrypted = CryptoUtils.decrypt(encrypted, key);
        
        // Then: Should handle large data correctly
        assertThat(decrypted).isEqualTo(largeData.toString());
        assertThat(encrypted.length()).isGreaterThan(100);
    }
}