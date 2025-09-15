package com.mysillydreams.authservice.service;

import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Service responsible for automatically fetching and refreshing the Keycloak client secret.
 * This eliminates the need to hardcode client secrets in configuration files.
 */
@Slf4j
@Service
public class KeycloakClientSecretService {

    private final Keycloak keycloak;
    private final String realm;
    private final String clientId;
    
    // Thread-safe storage for the current client secret
    private final AtomicReference<String> currentClientSecret = new AtomicReference<>();
    
    @Value("${keycloak.client-secret.refresh-interval-minutes:30}")
    private int refreshIntervalMinutes;

    @Autowired
    public KeycloakClientSecretService(
            Keycloak keycloak,
            @Value("${keycloak.realm}") String realm,
            @Value("${keycloak.client-id}") String clientId) {
        this.keycloak = keycloak;
        this.realm = realm;
        this.clientId = clientId;
    }

    /**
     * Initialize the client secret on service startup
     */
    @PostConstruct
    public void initializeClientSecret() {
        log.info("🔧 Initializing Keycloak client secret service for client: {}", clientId);
        refreshClientSecret();
    }

    /**
     * Get the current client secret
     * @return The current client secret, or null if not available
     */
    public String getCurrentClientSecret() {
        return currentClientSecret.get();
    }

    /**
     * Refresh the client secret from Keycloak
     * This method is called on startup and periodically based on the configured interval
     */
    @Scheduled(fixedRateString = "#{${keycloak.client-secret.refresh-interval-minutes:30} * 60 * 1000}")
    public void refreshClientSecret() {
        try {
            log.debug("🔄 Refreshing client secret for client: {}", clientId);
            
            RealmResource realmResource = keycloak.realm(realm);
            List<ClientRepresentation> clients = realmResource.clients().findByClientId(clientId);
            
            if (clients == null || clients.isEmpty()) {
                log.error("❌ Client not found: {}", clientId);
                return;
            }
            
            ClientRepresentation client = clients.get(0);
            String clientUuid = client.getId();
            
            // Get the client resource to access the secret
            ClientResource clientResource = realmResource.clients().get(clientUuid);
            
            // Fetch the client secret
            String secret = fetchClientSecret(clientResource);
            
            if (secret != null && !secret.isEmpty()) {
                String previousSecret = currentClientSecret.getAndSet(secret);
                
                if (previousSecret == null) {
                    log.info("✅ Client secret initialized for client: {}", clientId);
                } else if (!secret.equals(previousSecret)) {
                    log.info("🔄 Client secret updated for client: {}", clientId);
                } else {
                    log.debug("✅ Client secret verified (no change) for client: {}", clientId);
                }
            } else {
                log.error("❌ Failed to fetch client secret for client: {}", clientId);
            }
            
        } catch (Exception e) {
            log.error("❌ Error refreshing client secret for client: {}", clientId, e);
        }
    }

    /**
     * Fetch the client secret from Keycloak
     * @param clientResource The client resource
     * @return The client secret, or null if not available
     */
    private String fetchClientSecret(ClientResource clientResource) {
        try {
            // Get the client representation which includes the secret
            ClientRepresentation client = clientResource.toRepresentation();
            return client.getSecret();
        } catch (Exception e) {
            log.error("❌ Failed to fetch client secret", e);
            return null;
        }
    }

    /**
     * Check if the client secret is available
     * @return true if the client secret is available, false otherwise
     */
    public boolean isClientSecretAvailable() {
        return currentClientSecret.get() != null;
    }

    /**
     * Get the refresh interval in minutes
     * @return The refresh interval in minutes
     */
    public int getRefreshIntervalMinutes() {
        return refreshIntervalMinutes;
    }
}
