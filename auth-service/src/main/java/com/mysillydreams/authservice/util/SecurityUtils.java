package com.mysillydreams.authservice.util;

/**
 * Security Utilities
 * 
 * SINGLE RESPONSIBILITY: Provide security-related utility functions
 * 
 * This utility class provides:
 * - Data masking for logging
 * - Security-related helper functions
 */
public class SecurityUtils {

    private SecurityUtils() {
        // Utility class - prevent instantiation
    }

    /**
     * Mask email for logging privacy
     * Converts "john.doe@example.com" to "jo***@example.com"
     */
    public static String maskEmail(String email) {
        if (email == null || email.isEmpty()) {
            return "null";
        }
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) {
            return email.charAt(0) + "***@***";
        }
        return email.substring(0, 2) + "***" + email.substring(atIndex);
    }

    /**
     * Mask user ID for logging
     * Shows only first 8 characters of UUID
     */
    public static String maskUserId(String userId) {
        if (userId == null || userId.isEmpty()) {
            return "null";
        }
        if (userId.length() <= 8) {
            return userId;
        }
        return userId.substring(0, 8) + "***";
    }

    /**
     * Mask sensitive data for logging
     * Generic method for masking any sensitive string
     */
    public static String maskSensitiveData(String data) {
        if (data == null || data.isEmpty()) {
            return "null";
        }
        if (data.length() <= 3) {
            return "***";
        }
        return data.substring(0, 2) + "***";
    }
}
