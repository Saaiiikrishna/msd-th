package com.mysillydreams.userservice.encryption;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Mock encryption service for development and testing environments.
 * WARNING: This is NOT secure and should NEVER be used in production!
 * 
 * This service provides basic encryption simulation for development purposes
 * where Vault is not available or desired.
 */
@Service("mockCryptoService")
@ConditionalOnProperty(name = "spring.cloud.vault.enabled", havingValue = "false", matchIfMissing = true)
@Slf4j
public class MockEncryptionService implements EncryptionService {

    private static final String MOCK_PREFIX = "MOCK_ENCRYPTED:";
    private static final String MOCK_HMAC_KEY = "mock-hmac-key-for-development-only";
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    public MockEncryptionService() {
        log.warn("=".repeat(80));
        log.warn("MockEncryptionService is active - THIS IS NOT SECURE!");
        log.warn("This should ONLY be used in development environments.");
        log.warn("Ensure Vault is properly configured for production use.");
        log.warn("=".repeat(80));
    }

    @Override
    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) {
            return plaintext;
        }

        try {
            // Simple Base64 encoding with a prefix (NOT SECURE!)
            String encoded = Base64.getEncoder().encodeToString(plaintext.getBytes(StandardCharsets.UTF_8));
            String result = MOCK_PREFIX + encoded;
            
            log.debug("Mock encrypted data (length: {} -> {})", plaintext.length(), result.length());
            return result;
            
        } catch (Exception e) {
            log.error("Mock encryption failed: {}", e.getMessage());
            throw new RuntimeException("Mock encryption failed", e);
        }
    }

    @Override
    public String decrypt(String ciphertext) {
        if (ciphertext == null || ciphertext.isEmpty()) {
            return ciphertext;
        }

        try {
            if (!ciphertext.startsWith(MOCK_PREFIX)) {
                log.warn("Attempting to decrypt data that doesn't have mock prefix: {}", 
                        ciphertext.substring(0, Math.min(20, ciphertext.length())));
                return ciphertext; // Return as-is if not mock encrypted
            }

            String encoded = ciphertext.substring(MOCK_PREFIX.length());
            String result = new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
            
            log.debug("Mock decrypted data (length: {} -> {})", ciphertext.length(), result.length());
            return result;
            
        } catch (Exception e) {
            log.error("Mock decryption failed for data: {}", 
                    ciphertext.substring(0, Math.min(50, ciphertext.length())));
            throw new RuntimeException("Mock decryption failed", e);
        }
    }

    @Override
    public String generateHmac(String data) {
        if (data == null || data.isEmpty()) {
            return null;
        }

        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(
                MOCK_HMAC_KEY.getBytes(StandardCharsets.UTF_8), 
                HMAC_ALGORITHM
            );
            mac.init(keySpec);
            
            byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            String result = bytesToHex(hmacBytes);
            
            log.debug("Generated mock HMAC for data (length: {})", data.length());
            return result;
            
        } catch (Exception e) {
            log.error("Mock HMAC generation failed: {}", e.getMessage());
            throw new RuntimeException("Mock HMAC generation failed", e);
        }
    }

    @Override
    public boolean verifyHmac(String data, String expectedHmac) {
        if (data == null || expectedHmac == null) {
            return false;
        }

        try {
            String actualHmac = generateHmac(data);
            boolean matches = expectedHmac.equals(actualHmac);
            
            log.debug("HMAC verification result: {}", matches);
            return matches;
            
        } catch (Exception e) {
            log.error("Mock HMAC verification failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String generateEmailHmac(String email) {
        if (email == null || email.trim().isEmpty()) {
            return null;
        }
        
        String normalizedEmail = normalizeEmail(email);
        return generateHmac(normalizedEmail);
    }

    @Override
    public String generatePhoneHmac(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return null;
        }
        
        String normalizedPhone = normalizePhone(phone);
        return generateHmac(normalizedPhone);
    }

    @Override
    public String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }
        return email.trim().toLowerCase();
    }

    @Override
    public String normalizePhone(String phone) {
        if (phone == null) {
            return null;
        }
        
        // Remove all non-digit characters
        String digitsOnly = phone.replaceAll("[^0-9]", "");
        
        // Add country code if missing (assuming India +91 for now)
        if (digitsOnly.length() == 10) {
            digitsOnly = "91" + digitsOnly;
        }
        
        // Ensure it starts with +
        if (!digitsOnly.startsWith("+")) {
            digitsOnly = "+" + digitsOnly;
        }
        
        return digitsOnly;
    }

    /**
     * Generates a mock session token for testing purposes
     */
    public String generateMockSessionToken(String userId, String userRef) {
        try {
            String payload = String.format("%s:%s:%d", userId, userRef, System.currentTimeMillis());
            return "mock_session_" + Base64.getEncoder().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("Failed to generate mock session token: {}", e.getMessage());
            return "mock_session_" + System.currentTimeMillis();
        }
    }

    /**
     * Generates a deterministic hash for testing uniqueness constraints
     */
    public String generateDeterministicHash(String input) {
        if (input == null || input.isEmpty()) {
            return null;
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 algorithm not available: {}", e.getMessage());
            // Fallback to simple hash
            return String.valueOf(input.hashCode());
        }
    }

    /**
     * Simulates key rotation (no-op in mock service)
     */
    public void rotateEncryptionKey() {
        log.info("Mock key rotation performed (no actual rotation in mock service)");
    }

    /**
     * Checks if the given ciphertext was encrypted by this mock service
     */
    public boolean isMockEncrypted(String ciphertext) {
        return ciphertext != null && ciphertext.startsWith(MOCK_PREFIX);
    }

    /**
     * Converts byte array to hexadecimal string
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    /**
     * Generates random data for testing purposes
     */
    public String generateRandomData(int length) {
        SecureRandom random = new SecureRandom();
        byte[] randomBytes = new byte[length];
        random.nextBytes(randomBytes);
        return Base64.getEncoder().encodeToString(randomBytes);
    }

    /**
     * Creates a mock encrypted value that looks realistic for testing
     */
    public String createMockEncryptedValue(String plaintext) {
        if (plaintext == null) {
            return null;
        }
        
        // Add some randomness to make it look more realistic
        String randomSuffix = generateRandomData(8);
        return encrypt(plaintext + ":" + randomSuffix);
    }

    /**
     * Extracts the original value from a mock encrypted value
     */
    public String extractFromMockEncryptedValue(String mockEncrypted) {
        if (mockEncrypted == null || !isMockEncrypted(mockEncrypted)) {
            return mockEncrypted;
        }
        
        try {
            String decrypted = decrypt(mockEncrypted);
            // Remove the random suffix we added
            int lastColonIndex = decrypted.lastIndexOf(':');
            if (lastColonIndex > 0) {
                return decrypted.substring(0, lastColonIndex);
            }
            return decrypted;
        } catch (Exception e) {
            log.warn("Failed to extract from mock encrypted value: {}", e.getMessage());
            return mockEncrypted;
        }
    }
}
