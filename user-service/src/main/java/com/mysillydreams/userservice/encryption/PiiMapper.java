package com.mysillydreams.userservice.encryption;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Utility class for handling PII encryption, decryption, and HMAC generation.
 * Provides centralized methods for consistent PII processing across the application.
 */
@Component
@Slf4j
public class PiiMapper {

    private final EncryptionService encryptionService;
    
    // Patterns for data validation
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );
    
    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "^[+]?[0-9\\s\\-\\(\\)]{7,15}$"
    );

    public PiiMapper(EncryptionService encryptionService) {
        this.encryptionService = encryptionService;
    }

    /**
     * Encrypts PII data and generates HMAC for searchable fields
     */
    public static class PiiData {
        private final String encryptedValue;
        private final String hmacValue;

        public PiiData(String encryptedValue, String hmacValue) {
            this.encryptedValue = encryptedValue;
            this.hmacValue = hmacValue;
        }

        public String getEncryptedValue() { return encryptedValue; }
        public String getHmacValue() { return hmacValue; }
    }

    /**
     * Processes email for storage - encrypts and generates HMAC
     */
    public PiiData processEmailForStorage(String email) {
        if (email == null || email.trim().isEmpty()) {
            return new PiiData(null, null);
        }

        try {
            // Validate email format
            if (!isValidEmail(email)) {
                log.warn("Invalid email format provided for encryption");
                throw new IllegalArgumentException("Invalid email format");
            }

            String normalizedEmail = encryptionService.normalizeEmail(email);
            String encryptedEmail = encryptionService.encrypt(normalizedEmail);
            String emailHmac = encryptionService.generateEmailHmac(email);

            log.debug("Processed email for storage (encrypted length: {}, HMAC length: {})", 
                    encryptedEmail != null ? encryptedEmail.length() : 0,
                    emailHmac != null ? emailHmac.length() : 0);

            return new PiiData(encryptedEmail, emailHmac);

        } catch (Exception e) {
            log.error("Failed to process email for storage: {}", e.getMessage());
            throw new RuntimeException("Email processing failed", e);
        }
    }

    /**
     * Processes phone number for storage - encrypts and generates HMAC
     */
    public PiiData processPhoneForStorage(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return new PiiData(null, null);
        }

        try {
            // Validate phone format
            if (!isValidPhone(phone)) {
                log.warn("Invalid phone format provided for encryption");
                throw new IllegalArgumentException("Invalid phone format");
            }

            String normalizedPhone = encryptionService.normalizePhone(phone);
            String encryptedPhone = encryptionService.encrypt(normalizedPhone);
            String phoneHmac = encryptionService.generatePhoneHmac(phone);

            log.debug("Processed phone for storage (encrypted length: {}, HMAC length: {})", 
                    encryptedPhone != null ? encryptedPhone.length() : 0,
                    phoneHmac != null ? phoneHmac.length() : 0);

            return new PiiData(encryptedPhone, phoneHmac);

        } catch (Exception e) {
            log.error("Failed to process phone for storage: {}", e.getMessage());
            throw new RuntimeException("Phone processing failed", e);
        }
    }

    /**
     * Processes general PII data for storage (names, DOB, etc.)
     */
    public String processPiiForStorage(String piiData) {
        if (piiData == null || piiData.trim().isEmpty()) {
            return null;
        }

        try {
            String trimmedData = piiData.trim();
            String encryptedData = encryptionService.encrypt(trimmedData);

            log.debug("Processed PII for storage (original length: {}, encrypted length: {})", 
                    trimmedData.length(), 
                    encryptedData != null ? encryptedData.length() : 0);

            return encryptedData;

        } catch (Exception e) {
            log.error("Failed to process PII for storage: {}", e.getMessage());
            throw new RuntimeException("PII processing failed", e);
        }
    }

    /**
     * Decrypts PII data for use
     */
    public String decryptPii(String encryptedData) {
        if (encryptedData == null || encryptedData.trim().isEmpty()) {
            return null;
        }

        try {
            String decryptedData = encryptionService.decrypt(encryptedData);
            
            log.debug("Decrypted PII data (encrypted length: {}, decrypted length: {})", 
                    encryptedData.length(), 
                    decryptedData != null ? decryptedData.length() : 0);

            return decryptedData;

        } catch (Exception e) {
            log.error("Failed to decrypt PII data: {}", e.getMessage());
            throw new RuntimeException("PII decryption failed", e);
        }
    }

    /**
     * Generates HMAC for email lookup
     */
    public String generateEmailLookupHmac(String email) {
        if (email == null || email.trim().isEmpty()) {
            return null;
        }

        try {
            if (!isValidEmail(email)) {
                throw new IllegalArgumentException("Invalid email format for lookup");
            }

            return encryptionService.generateEmailHmac(email);

        } catch (Exception e) {
            log.error("Failed to generate email lookup HMAC: {}", e.getMessage());
            throw new RuntimeException("Email HMAC generation failed", e);
        }
    }

    /**
     * Generates HMAC for phone lookup
     */
    public String generatePhoneLookupHmac(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return null;
        }

        try {
            if (!isValidPhone(phone)) {
                throw new IllegalArgumentException("Invalid phone format for lookup");
            }

            return encryptionService.generatePhoneHmac(phone);

        } catch (Exception e) {
            log.error("Failed to generate phone lookup HMAC: {}", e.getMessage());
            throw new RuntimeException("Phone HMAC generation failed", e);
        }
    }

    /**
     * Masks email for display (shows first 2 chars + domain)
     */
    public String maskEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return null;
        }

        try {
            String[] parts = email.split("@");
            if (parts.length != 2) {
                return "***@***.***";
            }

            String localPart = parts[0];
            String domainPart = parts[1];

            String maskedLocal = localPart.length() > 2 
                ? localPart.substring(0, 2) + "***"
                : "***";

            String[] domainParts = domainPart.split("\\.");
            String maskedDomain = domainParts.length > 1
                ? domainParts[0].charAt(0) + "***." + domainParts[domainParts.length - 1]
                : "***";

            return maskedLocal + "@" + maskedDomain;

        } catch (Exception e) {
            log.warn("Failed to mask email: {}", e.getMessage());
            return "***@***.***";
        }
    }

    /**
     * Masks phone number for display (shows country code + last 4 digits)
     */
    public String maskPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return null;
        }

        try {
            String normalizedPhone = encryptionService.normalizePhone(phone);
            
            if (normalizedPhone.length() > 6) {
                String countryCode = normalizedPhone.substring(0, 3); // +XX
                String lastFour = normalizedPhone.substring(normalizedPhone.length() - 4);
                String stars = "*".repeat(normalizedPhone.length() - 7); // -3 for country code, -4 for last digits
                return countryCode + stars + lastFour;
            } else {
                return "+**" + "*".repeat(Math.max(0, normalizedPhone.length() - 2));
            }

        } catch (Exception e) {
            log.warn("Failed to mask phone: {}", e.getMessage());
            return "+**********";
        }
    }

    /**
     * Masks general PII data for display
     */
    public String maskPii(String piiData) {
        if (piiData == null || piiData.trim().isEmpty()) {
            return null;
        }

        try {
            if (piiData.length() <= 2) {
                return "*".repeat(piiData.length());
            } else if (piiData.length() <= 4) {
                return piiData.charAt(0) + "*".repeat(piiData.length() - 1);
            } else {
                return piiData.substring(0, 2) + "*".repeat(piiData.length() - 2);
            }

        } catch (Exception e) {
            log.warn("Failed to mask PII data: {}", e.getMessage());
            return "***";
        }
    }

    /**
     * Validates email format
     */
    public boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    /**
     * Validates phone number format
     */
    public boolean isValidPhone(String phone) {
        return phone != null && PHONE_PATTERN.matcher(phone.trim()).matches();
    }

    /**
     * Checks if data appears to be encrypted
     */
    public boolean isEncrypted(String data) {
        if (data == null || data.trim().isEmpty()) {
            return false;
        }

        // Check for common encryption prefixes/patterns
        return data.startsWith("vault:") || 
               data.startsWith("MOCK_ENCRYPTED:") ||
               (data.length() > 50 && !data.contains(" ")); // Heuristic for encrypted data
    }

    /**
     * Safely processes PII with error handling
     */
    public String safeProcessPii(String piiData, boolean encrypt) {
        try {
            if (encrypt) {
                return processPiiForStorage(piiData);
            } else {
                return decryptPii(piiData);
            }
        } catch (Exception e) {
            log.error("Safe PII processing failed (encrypt={}): {}", encrypt, e.getMessage());
            return encrypt ? null : piiData; // Return null for encryption failure, original for decryption failure
        }
    }

    /**
     * Batch processes multiple PII fields
     */
    public static class BatchPiiResult {
        private final String encryptedFirstName;
        private final String encryptedLastName;
        private final PiiData emailData;
        private final PiiData phoneData;
        private final String encryptedDob;

        public BatchPiiResult(String encryptedFirstName, String encryptedLastName, 
                            PiiData emailData, PiiData phoneData, String encryptedDob) {
            this.encryptedFirstName = encryptedFirstName;
            this.encryptedLastName = encryptedLastName;
            this.emailData = emailData;
            this.phoneData = phoneData;
            this.encryptedDob = encryptedDob;
        }

        // Getters
        public String getEncryptedFirstName() { return encryptedFirstName; }
        public String getEncryptedLastName() { return encryptedLastName; }
        public PiiData getEmailData() { return emailData; }
        public PiiData getPhoneData() { return phoneData; }
        public String getEncryptedDob() { return encryptedDob; }
    }

    /**
     * Processes all user PII fields in a single operation
     */
    public BatchPiiResult batchProcessUserPii(String firstName, String lastName, 
                                            String email, String phone, String dob) {
        try {
            String encryptedFirstName = processPiiForStorage(firstName);
            String encryptedLastName = processPiiForStorage(lastName);
            PiiData emailData = processEmailForStorage(email);
            PiiData phoneData = processPhoneForStorage(phone);
            String encryptedDob = processPiiForStorage(dob);

            log.debug("Batch processed user PII fields");
            return new BatchPiiResult(encryptedFirstName, encryptedLastName, emailData, phoneData, encryptedDob);

        } catch (Exception e) {
            log.error("Batch PII processing failed: {}", e.getMessage());
            throw new RuntimeException("Batch PII processing failed", e);
        }
    }
}
