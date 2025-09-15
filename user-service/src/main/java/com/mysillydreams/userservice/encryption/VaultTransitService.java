package com.mysillydreams.userservice.encryption;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.vault.core.VaultOperations;
import org.springframework.vault.support.VaultResponse;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;

/**
 * Service for encrypting/decrypting PII data using HashiCorp Vault Transit engine.
 * Also provides HMAC functionality for searchable encrypted fields.
 */
@Service
@ConditionalOnProperty(name = "spring.cloud.vault.enabled", havingValue = "true")
@Slf4j
public class VaultTransitService implements EncryptionService {

    private final VaultOperations vaultOperations;
    private final String transitBackend;
    private final String encryptionKeyName;
    private final String hmacKeyName;

    public VaultTransitService(
            VaultOperations vaultOperations,
            @Value("${spring.cloud.vault.transit.backend:transit}") String transitBackend,
            @Value("${spring.cloud.vault.transit.key-name:user_pii}") String encryptionKeyName,
            @Value("${app.encryption.hmac-key-name:user_search_hmac}") String hmacKeyName) {
        
        this.vaultOperations = vaultOperations;
        this.transitBackend = transitBackend;
        this.encryptionKeyName = encryptionKeyName;
        this.hmacKeyName = hmacKeyName;
        
        log.info("VaultTransitService initialized with backend: {}, encryption key: {}, HMAC key: {}", 
                transitBackend, encryptionKeyName, hmacKeyName);
        
        // Initialize keys if they don't exist
        initializeKeys();
    }

    @Override
    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) {
            return plaintext;
        }

        try {
            log.debug("Encrypting data with key: {}", encryptionKeyName);
            
            VaultResponse response = vaultOperations.write(
                transitBackend + "/encrypt/" + encryptionKeyName,
                Map.of("plaintext", Base64.getEncoder().encodeToString(plaintext.getBytes(StandardCharsets.UTF_8)))
            );

            if (response == null || response.getData() == null) {
                throw new EncryptionException("Vault returned null response for encryption");
            }

            String ciphertext = (String) response.getData().get("ciphertext");
            if (ciphertext == null) {
                throw new EncryptionException("Vault returned null ciphertext");
            }

            log.debug("Successfully encrypted data");
            return ciphertext;

        } catch (Exception e) {
            log.error("Failed to encrypt data: {}", e.getMessage());
            throw new EncryptionException("Encryption failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String decrypt(String ciphertext) {
        if (ciphertext == null || ciphertext.isEmpty()) {
            return ciphertext;
        }

        try {
            log.debug("Decrypting data with key: {}", encryptionKeyName);
            
            VaultResponse response = vaultOperations.write(
                transitBackend + "/decrypt/" + encryptionKeyName,
                Map.of("ciphertext", ciphertext)
            );

            if (response == null || response.getData() == null) {
                throw new EncryptionException("Vault returned null response for decryption");
            }

            String plaintextBase64 = (String) response.getData().get("plaintext");
            if (plaintextBase64 == null) {
                throw new EncryptionException("Vault returned null plaintext");
            }

            String plaintext = new String(Base64.getDecoder().decode(plaintextBase64), StandardCharsets.UTF_8);
            log.debug("Successfully decrypted data");
            return plaintext;

        } catch (Exception e) {
            log.error("Failed to decrypt data: {}", e.getMessage());
            throw new EncryptionException("Decryption failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String generateHmac(String data) {
        if (data == null || data.isEmpty()) {
            return null;
        }

        try {
            log.debug("Generating HMAC for data with key: {}", hmacKeyName);
            
            VaultResponse response = vaultOperations.write(
                transitBackend + "/hmac/" + hmacKeyName,
                Map.of("input", Base64.getEncoder().encodeToString(data.getBytes(StandardCharsets.UTF_8)))
            );

            if (response == null || response.getData() == null) {
                throw new EncryptionException("Vault returned null response for HMAC");
            }

            String hmac = (String) response.getData().get("hmac");
            if (hmac == null) {
                throw new EncryptionException("Vault returned null HMAC");
            }

            // Extract the actual HMAC value (Vault returns format like "vault:v1:hmac-sha256:...")
            String[] parts = hmac.split(":");
            if (parts.length >= 3) {
                hmac = parts[parts.length - 1]; // Get the last part which is the actual HMAC
            }

            // Convert Base64 HMAC to hexadecimal to match database constraint
            try {
                byte[] hmacBytes = Base64.getDecoder().decode(hmac);
                hmac = bytesToHex(hmacBytes);
            } catch (IllegalArgumentException e) {
                log.warn("HMAC is not in Base64 format, using as-is: {}", hmac);
            }

            log.debug("Successfully generated HMAC");
            return hmac;

        } catch (Exception e) {
            log.error("Failed to generate HMAC: {}", e.getMessage());
            throw new EncryptionException("HMAC generation failed: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean verifyHmac(String data, String expectedHmac) {
        if (data == null || expectedHmac == null) {
            return false;
        }

        try {
            String actualHmac = generateHmac(data);
            return expectedHmac.equals(actualHmac);
        } catch (Exception e) {
            log.error("Failed to verify HMAC: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Generates HMAC for email after normalization
     */
    public String generateEmailHmac(String email) {
        if (email == null || email.trim().isEmpty()) {
            return null;
        }
        
        String normalizedEmail = normalizeEmail(email);
        return generateHmac(normalizedEmail);
    }

    /**
     * Generates HMAC for phone after normalization
     */
    public String generatePhoneHmac(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return null;
        }
        
        String normalizedPhone = normalizePhone(phone);
        return generateHmac(normalizedPhone);
    }

    /**
     * Normalizes email for consistent HMAC generation
     */
    public String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }
        return email.trim().toLowerCase();
    }

    /**
     * Normalizes phone number to E.164 format for consistent HMAC generation
     */
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
     * Rotates the encryption key (creates a new version)
     */
    public void rotateEncryptionKey() {
        try {
            log.info("Rotating encryption key: {}", encryptionKeyName);
            
            vaultOperations.write(
                transitBackend + "/keys/" + encryptionKeyName + "/rotate",
                Map.of()
            );
            
            log.info("Successfully rotated encryption key: {}", encryptionKeyName);
        } catch (Exception e) {
            log.error("Failed to rotate encryption key: {}", e.getMessage());
            throw new EncryptionException("Key rotation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Gets key information including version and creation time
     */
    @Cacheable(value = "vault-key-info", key = "#keyName")
    public Map<String, Object> getKeyInfo(String keyName) {
        try {
            VaultResponse response = vaultOperations.read(transitBackend + "/keys/" + keyName);
            
            if (response == null || response.getData() == null) {
                throw new EncryptionException("Failed to read key info for: " + keyName);
            }
            
            return response.getData();
        } catch (Exception e) {
            log.error("Failed to get key info for {}: {}", keyName, e.getMessage());
            throw new EncryptionException("Failed to get key info: " + e.getMessage(), e);
        }
    }

    /**
     * Initializes encryption and HMAC keys if they don't exist
     */
    private void initializeKeys() {
        try {
            // Check and create encryption key
            try {
                getKeyInfo(encryptionKeyName);
                log.debug("Encryption key {} already exists", encryptionKeyName);
            } catch (Exception e) {
                log.info("Creating encryption key: {}", encryptionKeyName);
                vaultOperations.write(
                    transitBackend + "/keys/" + encryptionKeyName,
                    Map.of("type", "aes256-gcm96")
                );
                log.info("Successfully created encryption key: {}", encryptionKeyName);
            }

            // Check and create HMAC key
            try {
                getKeyInfo(hmacKeyName);
                log.debug("HMAC key {} already exists", hmacKeyName);
            } catch (Exception e) {
                log.info("Creating HMAC key: {}", hmacKeyName);
                vaultOperations.write(
                    transitBackend + "/keys/" + hmacKeyName,
                    Map.of("type", "aes256-gcm96")
                );
                log.info("Successfully created HMAC key: {}", hmacKeyName);
            }

        } catch (Exception e) {
            log.warn("Failed to initialize Vault keys (this is normal in dev environments): {}", e.getMessage());
        }
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
     * Exception thrown when encryption/decryption operations fail
     */
    public static class EncryptionException extends RuntimeException {
        public EncryptionException(String message) {
            super(message);
        }

        public EncryptionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
