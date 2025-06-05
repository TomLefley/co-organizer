package dev.lefley.coorganizer.crypto;

import dev.lefley.coorganizer.util.Logger;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public class CryptoUtils {
    
    private static final Logger logger = new Logger(null, CryptoUtils.class);
    
    private static final String AES_ALGORITHM = "AES";
    private static final String AES_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int AES_KEY_LENGTH = 256;
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 16;
    private static final String HASH_ALGORITHM = "SHA-256";
    
    public static String generateSymmetricKey() {
        try {
            logger.debug("Generating new symmetric key");
            KeyGenerator keyGenerator = KeyGenerator.getInstance(AES_ALGORITHM);
            keyGenerator.init(AES_KEY_LENGTH);
            SecretKey secretKey = keyGenerator.generateKey();
            String encodedKey = Base64.getEncoder().encodeToString(secretKey.getEncoded());
            logger.debug("Successfully generated symmetric key");
            return encodedKey;
        } catch (NoSuchAlgorithmException e) {
            logger.error("Failed to generate symmetric key", e);
            throw new RuntimeException("Failed to generate symmetric key", e);
        }
    }
    
    public static String generateFingerprint(String groupName, String symmetricKey) {
        try {
            logger.debug("Generating fingerprint for group: " + groupName);
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            String combined = groupName + ":" + symmetricKey;
            byte[] hash = digest.digest(combined.getBytes());
            
            // Convert to hex and take first 16 characters for readability
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            // Format as groups of 4 characters for readability
            String fullHex = hexString.toString().substring(0, 16).toUpperCase();
            String fingerprint = String.format("%s-%s-%s-%s", 
                fullHex.substring(0, 4),
                fullHex.substring(4, 8), 
                fullHex.substring(8, 12),
                fullHex.substring(12, 16)
            );
            logger.debug("Successfully generated fingerprint: " + fingerprint);
            return fingerprint;
        } catch (NoSuchAlgorithmException e) {
            logger.error("Failed to generate fingerprint for group: " + groupName, e);
            throw new RuntimeException("Failed to generate fingerprint", e);
        }
    }
    
    public static String generateRandomId() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[16];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
    
    public static String encrypt(String plaintext, String base64Key) {
        try {
            // Decode the base64 key
            byte[] keyBytes = Base64.getDecoder().decode(base64Key);
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, AES_ALGORITHM);
            
            // Generate random IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);
            
            // Setup cipher
            Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec);
            
            // Encrypt the plaintext
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes());
            
            // Combine IV and ciphertext
            byte[] encryptedData = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, encryptedData, 0, iv.length);
            System.arraycopy(ciphertext, 0, encryptedData, iv.length, ciphertext.length);
            
            // Return base64 encoded result
            return Base64.getEncoder().encodeToString(encryptedData);
        } catch (Exception e) {
            logger.error("Failed to encrypt data", e);
            throw new RuntimeException("Failed to encrypt data", e);
        }
    }
    
    public static String decrypt(String encryptedData, String base64Key) {
        try {
            // Decode the base64 key and encrypted data
            byte[] keyBytes = Base64.getDecoder().decode(base64Key);
            byte[] encryptedBytes = Base64.getDecoder().decode(encryptedData);
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, AES_ALGORITHM);
            
            // Extract IV and ciphertext
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] ciphertext = new byte[encryptedBytes.length - GCM_IV_LENGTH];
            System.arraycopy(encryptedBytes, 0, iv, 0, iv.length);
            System.arraycopy(encryptedBytes, iv.length, ciphertext, 0, ciphertext.length);
            
            // Setup cipher
            Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec);
            
            // Decrypt the ciphertext
            byte[] plaintext = cipher.doFinal(ciphertext);
            
            return new String(plaintext);
        } catch (Exception e) {
            logger.error("Failed to decrypt data", e);
            throw new RuntimeException("Failed to decrypt data", e);
        }
    }
}