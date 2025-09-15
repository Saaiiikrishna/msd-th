package com.mysillydreams.userservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Vault Initialization Service for User Service
 * Ensures Vault is properly initialized with required encryption keys
 */
@Service
@Slf4j
public class VaultInitializationService {

    @Value("${vault.uri:http://vault:8200}")
    private String vaultUri;

    @Value("${vault.token:myroot}")
    private String vaultToken;

    @Value("${spring.cloud.vault.initialization.enabled:true}")
    private boolean initializationEnabled;

    @Value("${spring.cloud.vault.initialization.retry-attempts:3}")
    private int maxRetryAttempts;

    @Value("${spring.cloud.vault.initialization.retry-delay-seconds:5}")
    private int retryDelaySeconds;

    private final RestTemplate restTemplate = new RestTemplate();

    // Required encryption keys for user service
    private static final String[] REQUIRED_ENCRYPTION_KEYS = {
        "user_pii",           // For user PII data encryption
        "user_search_hmac"    // For searchable encrypted fields
    };

    private static final String TRANSIT_ENGINE_PATH = "transit";

    /**
     * Debug method to verify service is being loaded
     */
    @PostConstruct
    public void init() {
        log.info("🔧 VaultInitializationService (user-service) has been instantiated and is ready");
        log.info("🔧 Vault initialization enabled: {}", initializationEnabled);
        log.info("🔧 Vault URI: {}", vaultUri);
    }

    /**
     * Initialize Vault when application is ready
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        if (!initializationEnabled) {
            log.info("🔧 Vault initialization is disabled via configuration");
            return;
        }

        log.info("🚀 Starting Vault initialization for user-service...");
        initializeVaultWithRetry();
    }

    /**
     * Initialize Vault with retry mechanism
     */
    private void initializeVaultWithRetry() {
        int attempt = 1;

        while (attempt <= maxRetryAttempts) {
            try {
                log.info("🔄 Vault initialization attempt {} of {}", attempt, maxRetryAttempts);

                if (initializeVault()) {
                    log.info("✅ Vault initialization completed successfully on attempt {}", attempt);
                    return;
                }

            } catch (Exception e) {
                log.warn("⚠️ Vault initialization attempt {} failed: {}", attempt, e.getMessage());

                if (attempt == maxRetryAttempts) {
                    log.error("❌ Vault initialization failed after {} attempts. User service may not function properly!", maxRetryAttempts);
                    return;
                }
            }

            attempt++;

            if (attempt <= maxRetryAttempts) {
                log.info("⏳ Waiting {} seconds before retry...", retryDelaySeconds);
                try {
                    TimeUnit.SECONDS.sleep(retryDelaySeconds);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("❌ Vault initialization interrupted");
                    return;
                }
            }
        }
    }

    /**
     * Main initialization method
     */
    private boolean initializeVault() {
        try {
            // Step 1: Check Vault connectivity
            if (!checkVaultConnectivity()) {
                log.error("❌ Cannot connect to Vault at {}", vaultUri);
                return false;
            }

            // Step 2: Enable transit engine if not already enabled
            if (!ensureTransitEngineEnabled()) {
                log.error("❌ Failed to enable transit engine");
                return false;
            }

            // Step 3: Create all required encryption keys
            if (!createRequiredEncryptionKeys()) {
                log.error("❌ Failed to create required encryption keys");
                return false;
            }

            // Step 4: Test encryption functionality
            if (!testEncryptionFunctionality()) {
                log.error("❌ Encryption functionality test failed");
                return false;
            }

            log.info("🎉 Vault initialization completed successfully!");
            return true;

        } catch (Exception e) {
            log.error("❌ Vault initialization failed with exception", e);
            return false;
        }
    }

    /**
     * Check if Vault is accessible
     */
    private boolean checkVaultConnectivity() {
        try {
            log.debug("🔍 Checking Vault connectivity at {}", vaultUri);
            
            HttpHeaders headers = createVaultHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                vaultUri + "/v1/sys/health",
                HttpMethod.GET,
                entity,
                String.class
            );
            
            boolean isHealthy = response.getStatusCode().is2xxSuccessful();
            log.debug("✅ Vault connectivity check: {}", isHealthy ? "SUCCESS" : "FAILED");
            return isHealthy;
            
        } catch (Exception e) {
            log.debug("❌ Vault connectivity check failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Ensure transit engine is enabled
     */
    private boolean ensureTransitEngineEnabled() {
        try {
            log.debug("🔍 Checking if transit engine is enabled...");

            // Check if transit engine exists
            if (isTransitEngineEnabled()) {
                log.debug("✅ Transit engine is already enabled");
                return true;
            }

            log.info("🔧 Enabling transit engine...");
            return enableTransitEngine();

        } catch (Exception e) {
            log.error("❌ Failed to ensure transit engine is enabled", e);
            return false;
        }
    }

    /**
     * Check if transit engine is enabled
     */
    private boolean isTransitEngineEnabled() {
        try {
            HttpHeaders headers = createVaultHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                vaultUri + "/v1/sys/mounts",
                HttpMethod.GET,
                entity,
                String.class
            );

            String responseBody = response.getBody();
            boolean enabled = responseBody != null && responseBody.contains("\"" + TRANSIT_ENGINE_PATH + "/\"");
            log.debug("🔍 Transit engine enabled: {}", enabled);
            return enabled;

        } catch (Exception e) {
            log.debug("Transit engine check failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Enable transit engine
     */
    private boolean enableTransitEngine() {
        try {
            HttpHeaders headers = createVaultHeaders();

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("type", "transit");
            requestBody.put("description", "Transit engine for MySillyDreams encryption");

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                vaultUri + "/v1/sys/mounts/" + TRANSIT_ENGINE_PATH,
                HttpMethod.POST,
                entity,
                String.class
            );

            boolean success = response.getStatusCode().is2xxSuccessful();
            if (success) {
                log.info("✅ Transit engine enabled successfully");
            }
            return success;

        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.BAD_REQUEST &&
                e.getResponseBodyAsString().contains("path is already in use")) {
                log.debug("✅ Transit engine is already enabled");
                return true;
            }
            log.error("❌ Failed to enable transit engine: {}", e.getResponseBodyAsString());
            return false;
        } catch (Exception e) {
            log.error("❌ Failed to enable transit engine", e);
            return false;
        }
    }

    /**
     * Create all required encryption keys
     */
    private boolean createRequiredEncryptionKeys() {
        log.info("🔑 Creating required encryption keys...");

        boolean allKeysCreated = true;

        for (String keyName : REQUIRED_ENCRYPTION_KEYS) {
            if (!createEncryptionKey(keyName)) {
                log.error("❌ Failed to create encryption key: {}", keyName);
                allKeysCreated = false;
            }
        }

        if (allKeysCreated) {
            log.info("✅ All encryption keys created successfully");
        }

        return allKeysCreated;
    }

    /**
     * Create a single encryption key
     */
    private boolean createEncryptionKey(String keyName) {
        try {
            log.debug("🔑 Creating encryption key: {}", keyName);

            // Check if key already exists
            if (doesKeyExist(keyName)) {
                log.debug("✅ Encryption key '{}' already exists", keyName);
                return true;
            }

            HttpHeaders headers = createVaultHeaders();

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("type", "aes256-gcm96");
            requestBody.put("exportable", false);
            requestBody.put("allow_plaintext_backup", false);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                vaultUri + "/v1/" + TRANSIT_ENGINE_PATH + "/keys/" + keyName,
                HttpMethod.POST,
                entity,
                String.class
            );

            boolean success = response.getStatusCode().is2xxSuccessful();
            if (success) {
                log.debug("✅ Encryption key '{}' created successfully", keyName);
            }
            return success;

        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.BAD_REQUEST &&
                e.getResponseBodyAsString().contains("already exists")) {
                log.debug("✅ Encryption key '{}' already exists", keyName);
                return true;
            }
            log.error("❌ Failed to create encryption key '{}': {}", keyName, e.getResponseBodyAsString());
            return false;
        } catch (Exception e) {
            log.error("❌ Failed to create encryption key '{}'", keyName, e);
            return false;
        }
    }

    /**
     * Check if encryption key exists
     */
    private boolean doesKeyExist(String keyName) {
        try {
            HttpHeaders headers = createVaultHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                vaultUri + "/v1/" + TRANSIT_ENGINE_PATH + "/keys/" + keyName,
                HttpMethod.GET,
                entity,
                String.class
            );
            
            return response.getStatusCode().is2xxSuccessful();
            
        } catch (HttpClientErrorException e) {
            return false;
        } catch (Exception e) {
            log.debug("Key existence check failed for '{}': {}", keyName, e.getMessage());
            return false;
        }
    }

    /**
     * Test encryption functionality
     */
    private boolean testEncryptionFunctionality() {
        log.debug("🔍 Testing encryption functionality...");
        
        for (String keyName : REQUIRED_ENCRYPTION_KEYS) {
            if (!testEncryptionKey(keyName)) {
                log.error("❌ Encryption test failed for key: {}", keyName);
                return false;
            }
        }
        
        log.debug("✅ Encryption functionality test passed");
        return true;
    }

    /**
     * Test a single encryption key by performing a test encryption
     */
    private boolean testEncryptionKey(String keyName) {
        try {
            HttpHeaders headers = createVaultHeaders();
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("plaintext", "dGVzdA=="); // base64 encoded "test"
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                vaultUri + "/v1/" + TRANSIT_ENGINE_PATH + "/encrypt/" + keyName,
                HttpMethod.POST,
                entity,
                String.class
            );
            
            boolean success = response.getStatusCode().is2xxSuccessful();
            log.debug("🔍 Encryption key '{}' test: {}", keyName, success ? "SUCCESS" : "FAILED");
            return success;
            
        } catch (Exception e) {
            log.debug("❌ Encryption key '{}' test failed: {}", keyName, e.getMessage());
            return false;
        }
    }

    /**
     * Create HTTP headers for Vault requests
     */
    private HttpHeaders createVaultHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Vault-Token", vaultToken);
        return headers;
    }

    /**
     * Verify that all required encryption keys exist
     */
    private boolean verifyRequiredKeysExist() {
        log.debug("🔍 Verifying all required encryption keys exist...");

        for (String keyName : REQUIRED_ENCRYPTION_KEYS) {
            if (!doesKeyExist(keyName)) {
                log.error("❌ Required encryption key '{}' does not exist", keyName);
                return false;
            }
        }

        log.debug("✅ All required encryption keys exist");
        return true;
    }

    /**
     * Get initialization status for health checks
     */
    public boolean isVaultReady() {
        try {
            return checkVaultConnectivity() &&
                   isTransitEngineEnabled() &&
                   verifyRequiredKeysExist();
        } catch (Exception e) {
            return false;
        }
    }
}
