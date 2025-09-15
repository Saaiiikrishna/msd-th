package com.mysillydreams.userservice.encryption;

/**
 * Interface for encryption and HMAC services.
 * Provides methods for encrypting/decrypting PII data and generating HMACs for searchable fields.
 */
public interface EncryptionService {

    /**
     * Encrypts plaintext data.
     *
     * @param plaintext The data to encrypt
     * @return Encrypted ciphertext, or null/empty if input is null/empty
     * @throws RuntimeException if encryption fails
     */
    String encrypt(String plaintext);

    /**
     * Decrypts ciphertext data.
     *
     * @param ciphertext The encrypted data to decrypt
     * @return Decrypted plaintext, or null/empty if input is null/empty
     * @throws RuntimeException if decryption fails
     */
    String decrypt(String ciphertext);

    /**
     * Generates HMAC for the given data.
     * Used for creating searchable hashes of encrypted fields.
     *
     * @param data The data to generate HMAC for
     * @return HMAC hash as hex string, or null if input is null/empty
     * @throws RuntimeException if HMAC generation fails
     */
    String generateHmac(String data);

    /**
     * Verifies if the given data matches the expected HMAC.
     *
     * @param data The original data
     * @param expectedHmac The expected HMAC to verify against
     * @return true if HMAC matches, false otherwise
     */
    boolean verifyHmac(String data, String expectedHmac);

    /**
     * Generates HMAC for email after normalization.
     * Emails are normalized to lowercase and trimmed.
     *
     * @param email The email to generate HMAC for
     * @return HMAC hash for the normalized email
     */
    String generateEmailHmac(String email);

    /**
     * Generates HMAC for phone number after normalization.
     * Phone numbers are normalized to E.164 format.
     *
     * @param phone The phone number to generate HMAC for
     * @return HMAC hash for the normalized phone number
     */
    String generatePhoneHmac(String phone);

    /**
     * Normalizes email for consistent processing.
     *
     * @param email The email to normalize
     * @return Normalized email (lowercase, trimmed)
     */
    String normalizeEmail(String email);

    /**
     * Normalizes phone number for consistent processing.
     *
     * @param phone The phone number to normalize
     * @return Normalized phone number in E.164 format
     */
    String normalizePhone(String phone);
}
