package com.mysillydreams.authservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Validates internal API key configuration at startup
 * Ensures that the auth service can communicate with user service
 */
@Component
@Slf4j
public class InternalApiKeyValidator {

    @Value("${auth.user-service.base-url}")
    private String userServiceUrl;

    @Value("${auth.user-service.internal-api-key}")
    private String internalApiKey;

    private final WebClient.Builder webClientBuilder;

    public InternalApiKeyValidator(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }

    /**
     * Validate internal API key configuration after application startup
     */
    @EventListener(ApplicationReadyEvent.class)
    public void validateInternalApiKeyConfiguration() {
        log.info("🔍 === VALIDATING INTERNAL API KEY CONFIGURATION ===");
        
        // Basic configuration validation
        if (internalApiKey == null || internalApiKey.trim().isEmpty()) {
            log.error("❌ CRITICAL: Internal API key is not configured!");
            log.error("❌ Please set auth.user-service.internal-api-key property");
            return;
        }

        if (internalApiKey.equals("auth-service-internal-key-12345")) {
            log.warn("⚠️ WARNING: Using default internal API key - change this in production!");
        }

        log.info("✅ Internal API key is configured");
        log.info("🔗 Target User Service URL: {}", userServiceUrl);

        // Test connectivity to user service (non-blocking)
        testUserServiceConnectivity();
    }

    /**
     * Test connectivity to user service without blocking startup
     */
    private void testUserServiceConnectivity() {
        WebClient webClient = webClientBuilder
                .baseUrl(userServiceUrl)
                .build();

        // Test with a simple health check or options request
        webClient.options()
                .uri("/actuator/health")
                .header("X-Internal-API-Key", internalApiKey)
                .retrieve()
                .toBodilessEntity()
                .timeout(Duration.ofSeconds(5))
                .doOnSuccess(response -> {
                    log.info("✅ User Service connectivity test successful");
                    log.info("✅ Internal API key communication verified");
                })
                .doOnError(error -> {
                    log.warn("⚠️ User Service connectivity test failed: {}", error.getMessage());
                    log.warn("⚠️ This may indicate network issues or incorrect configuration");
                    log.warn("⚠️ User creation may fail until connectivity is restored");
                })
                .onErrorResume(error -> Mono.empty()) // Don't fail startup on connectivity issues
                .subscribe();
    }

    /**
     * Validate internal API key format and security
     */
    public boolean isSecureApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() < 20) {
            log.warn("⚠️ Internal API key is too short - consider using a longer, more secure key");
            return false;
        }

        if (apiKey.equals("auth-service-internal-key-12345")) {
            log.warn("⚠️ Using default API key - this should be changed in production");
            return false;
        }

        // Check for common weak patterns
        if (apiKey.toLowerCase().contains("password") || 
            apiKey.toLowerCase().contains("secret") ||
            apiKey.toLowerCase().contains("key123")) {
            log.warn("⚠️ API key contains common weak patterns");
            return false;
        }

        return true;
    }

    /**
     * Get masked version of API key for logging
     */
    public String getMaskedApiKey() {
        if (internalApiKey == null || internalApiKey.length() < 8) {
            return "***";
        }
        return internalApiKey.substring(0, 4) + "***" + internalApiKey.substring(internalApiKey.length() - 4);
    }
}
