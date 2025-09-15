package com.mysillydreams.userservice.health;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Health indicator for internal API key configuration
 */
@Component
@Slf4j
public class InternalApiKeyHealthIndicator {

    @Value("${app.security.internal-api-keys}")
    private Set<String> validApiKeys;

    public String health() {
        try {
            return checkInternalApiKeyConfiguration();
        } catch (Exception e) {
            log.warn("Internal API key health check failed", e);
            return "DOWN: " + e.getMessage();
        }
    }

    private String checkInternalApiKeyConfiguration() {
        Map<String, Object> details = new HashMap<>();
        
        // Check if API keys are configured
        if (validApiKeys == null || validApiKeys.isEmpty()) {
            return "DOWN: No internal API keys configured";
        }

        // Check for weak or default keys
        Set<String> weakKeys = Set.of(
            "default", "test", "admin", "password", "123456", 
            "internal-service-key-123", "dev-key", "local-key"
        );
        
        boolean hasWeakKeys = validApiKeys.stream()
                .anyMatch(key -> weakKeys.contains(key) || key.length() < 16);

        details.put("configured_keys_count", validApiKeys.size());
        details.put("has_weak_keys", hasWeakKeys);
        details.put("min_key_length", validApiKeys.stream()
                .mapToInt(String::length)
                .min()
                .orElse(0));
        details.put("max_key_length", validApiKeys.stream()
                .mapToInt(String::length)
                .max()
                .orElse(0));

        if (hasWeakKeys) {
            return "DOWN: Weak API keys detected";
        }

        return "UP: Internal API keys properly configured";
    }

    /**
     * Validates if a given API key is valid
     */
    public boolean isValidApiKey(String apiKey) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return false;
        }
        
        return validApiKeys != null && validApiKeys.contains(apiKey.trim());
    }

    /**
     * Gets the count of configured API keys (for monitoring)
     */
    public int getConfiguredKeyCount() {
        return validApiKeys != null ? validApiKeys.size() : 0;
    }

    /**
     * Checks if any weak keys are configured (for security auditing)
     */
    public boolean hasWeakKeys() {
        if (validApiKeys == null || validApiKeys.isEmpty()) {
            return true; // No keys is considered weak
        }

        Set<String> weakKeys = Set.of(
            "default", "test", "admin", "password", "123456", 
            "internal-service-key-123", "dev-key", "local-key"
        );
        
        return validApiKeys.stream()
                .anyMatch(key -> weakKeys.contains(key) || key.length() < 16);
    }

    /**
     * Gets security recommendations based on current configuration
     */
    public Map<String, String> getSecurityRecommendations() {
        Map<String, String> recommendations = new HashMap<>();
        
        if (validApiKeys == null || validApiKeys.isEmpty()) {
            recommendations.put("critical", "Configure at least one internal API key");
            return recommendations;
        }

        if (hasWeakKeys()) {
            recommendations.put("high", "Replace weak API keys with stronger ones (min 32 characters)");
        }

        if (validApiKeys.size() == 1) {
            recommendations.put("medium", "Consider configuring multiple API keys for different services");
        }

        boolean hasShortKeys = validApiKeys.stream()
                .anyMatch(key -> key.length() < 32);
        
        if (hasShortKeys) {
            recommendations.put("medium", "Use longer API keys (recommended: 32+ characters)");
        }

        if (recommendations.isEmpty()) {
            recommendations.put("info", "API key configuration looks good");
        }

        return recommendations;
    }

    /**
     * Logs security audit information
     */
    public void logSecurityAudit() {
        if (validApiKeys == null || validApiKeys.isEmpty()) {
            log.warn("🔐 SECURITY AUDIT: No internal API keys configured - service is vulnerable");
            return;
        }

        log.info("🔐 SECURITY AUDIT: {} internal API key(s) configured", validApiKeys.size());
        
        if (hasWeakKeys()) {
            log.warn("🔐 SECURITY AUDIT: Weak API keys detected - consider strengthening");
        } else {
            log.info("🔐 SECURITY AUDIT: API key strength appears adequate");
        }

        getSecurityRecommendations().forEach((level, recommendation) -> 
            log.info("🔐 SECURITY RECOMMENDATION [{}]: {}", level.toUpperCase(), recommendation)
        );
    }
}
