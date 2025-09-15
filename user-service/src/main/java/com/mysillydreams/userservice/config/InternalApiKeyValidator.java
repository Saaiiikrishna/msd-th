package com.mysillydreams.userservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Validates internal API key configuration at startup for User Service
 * Ensures that internal API keys are properly configured for service-to-service communication
 */
@Component
@Slf4j
public class InternalApiKeyValidator {

    @Value("${app.security.internal-api-keys}")
    private Set<String> validApiKeys;

    /**
     * Validate internal API key configuration after application startup
     */
    @EventListener(ApplicationReadyEvent.class)
    public void validateInternalApiKeyConfiguration() {
        log.info("🔍 === VALIDATING INTERNAL API KEY CONFIGURATION ===");
        
        // Basic configuration validation
        if (validApiKeys == null || validApiKeys.isEmpty()) {
            log.error("❌ CRITICAL: No internal API keys configured!");
            log.error("❌ Please set app.security.internal-api-keys property");
            log.error("❌ Service-to-service communication will fail!");
            return;
        }

        log.info("✅ Found {} configured internal API key(s)", validApiKeys.size());

        // Validate each API key
        int secureKeys = 0;
        int defaultKeys = 0;
        
        for (String apiKey : validApiKeys) {
            String maskedKey = maskApiKey(apiKey);
            log.info("🔑 Validating API key: {}", maskedKey);
            
            if (isDefaultKey(apiKey)) {
                defaultKeys++;
                log.warn("⚠️ Default API key detected: {} - change this in production!", maskedKey);
            }
            
            if (isSecureApiKey(apiKey)) {
                secureKeys++;
                log.info("✅ API key {} meets security requirements", maskedKey);
            } else {
                log.warn("⚠️ API key {} may not meet security requirements", maskedKey);
            }
        }

        // Summary
        log.info("📊 API Key Security Summary:");
        log.info("📊 Total keys: {}", validApiKeys.size());
        log.info("📊 Secure keys: {}", secureKeys);
        log.info("📊 Default keys: {}", defaultKeys);
        
        if (defaultKeys > 0) {
            log.warn("⚠️ {} default API key(s) detected - update for production use", defaultKeys);
        }
        
        if (secureKeys == validApiKeys.size()) {
            log.info("✅ All API keys meet security requirements");
        }
    }

    /**
     * Check if API key is a default/example key
     */
    private boolean isDefaultKey(String apiKey) {
        return apiKey != null && (
            apiKey.equals("auth-service-internal-key-12345") ||
            apiKey.equals("admin-service-key-67890") ||
            apiKey.contains("example") ||
            apiKey.contains("default") ||
            apiKey.contains("test-key")
        );
    }

    /**
     * Validate API key security requirements
     */
    private boolean isSecureApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() < 20) {
            return false;
        }

        // Check for common weak patterns
        String lowerKey = apiKey.toLowerCase();
        if (lowerKey.contains("password") || 
            lowerKey.contains("secret") ||
            lowerKey.contains("key123") ||
            lowerKey.contains("admin") ||
            lowerKey.contains("test")) {
            return false;
        }

        return true;
    }

    /**
     * Mask API key for secure logging
     */
    private String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() < 8) {
            return "***";
        }
        return apiKey.substring(0, 4) + "***" + apiKey.substring(apiKey.length() - 4);
    }

    /**
     * Get count of configured API keys
     */
    public int getConfiguredKeyCount() {
        return validApiKeys != null ? validApiKeys.size() : 0;
    }

    /**
     * Check if a specific service key is configured
     */
    public boolean hasServiceKey(String serviceName) {
        if (validApiKeys == null) {
            return false;
        }
        
        return validApiKeys.stream()
                .anyMatch(key -> key.toLowerCase().contains(serviceName.toLowerCase()));
    }
}
