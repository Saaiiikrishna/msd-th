package com.mysillydreams.authservice.service;

import com.mysillydreams.authservice.config.KeycloakProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.IdentityProviderRepresentation;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for programmatic Keycloak configuration
 * Automatically configures Google Identity Provider and user attributes
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Profile("!bootstrap")
public class KeycloakConfigurationService {

    private final Keycloak keycloak;
    private final KeycloakProperties keycloakProperties;

    @Value("${google.oauth.client-id}")
    private String googleClientId;

    @Value("${google.oauth.client-secret}")
    private String googleClientSecret;

    @Value("${google.oauth.project-id}")
    private String googleProjectId;

    @Value("${google.oauth.enabled:true}")
    private boolean googleOAuthEnabled;

    /**
     * Initialize Keycloak configuration on startup
     */
    @PostConstruct
    public void initializeKeycloakConfiguration() {
        if (!googleOAuthEnabled) {
            log.info("🔧 Google OAuth is disabled, skipping Keycloak configuration");
            return;
        }

        try {
            log.info("🔧 Initializing Keycloak configuration for Google OAuth...");
            
            configureGoogleIdentityProvider();
            configureUserAttributes();
            
            log.info("✅ Keycloak configuration completed successfully");
        } catch (Exception e) {
            log.error("❌ Failed to configure Keycloak: {}", e.getMessage(), e);
            // Don't fail startup if configuration fails
        }
    }

    /**
     * Configure Google as an Identity Provider in Keycloak
     */
    public void configureGoogleIdentityProvider() {
        try {
            if (googleClientId == null || googleClientId.isEmpty() || 
                googleClientSecret == null || googleClientSecret.isEmpty()) {
                log.warn("⚠️ Google OAuth credentials not configured, skipping identity provider setup");
                return;
            }

            RealmResource realmResource = keycloak.realm(keycloakProperties.getRealm());
            
            // Check if Google identity provider already exists
            try {
                IdentityProviderRepresentation existingProvider = realmResource.identityProviders().get("google").toRepresentation();
                if (existingProvider != null) {
                    log.info("ℹ️ Google identity provider already exists, updating configuration");
                    updateGoogleIdentityProvider(realmResource, existingProvider);
                    return;
                }
            } catch (Exception e) {
                // Provider doesn't exist, create new one
                log.info("🆕 Creating new Google identity provider");
            }

            // Create Google Identity Provider
            IdentityProviderRepresentation googleProvider = new IdentityProviderRepresentation();
            googleProvider.setAlias("google");
            googleProvider.setProviderId("google");
            googleProvider.setDisplayName("Google");
            googleProvider.setEnabled(true);
            googleProvider.setTrustEmail(true);
            googleProvider.setStoreToken(false);
            googleProvider.setAddReadTokenRoleOnCreate(false);
            googleProvider.setFirstBrokerLoginFlowAlias("first broker login");
            googleProvider.setPostBrokerLoginFlowAlias("");

            // Configure Google-specific settings
            Map<String, String> config = new HashMap<>();
            config.put("clientId", googleClientId);
            config.put("clientSecret", googleClientSecret);
            config.put("hostedDomain", ""); // Leave empty for any domain
            config.put("useJwksUrl", "true");
            config.put("syncMode", "IMPORT"); // Import user data from Google
            googleProvider.setConfig(config);

            // Create the identity provider
            realmResource.identityProviders().create(googleProvider);
            
            log.info("✅ Google identity provider created successfully");

        } catch (Exception e) {
            log.error("❌ Failed to configure Google identity provider: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to configure Google identity provider", e);
        }
    }

    /**
     * Update existing Google Identity Provider
     */
    private void updateGoogleIdentityProvider(RealmResource realmResource, IdentityProviderRepresentation existingProvider) {
        try {
            // Update configuration
            Map<String, String> config = existingProvider.getConfig();
            if (config == null) {
                config = new HashMap<>();
            }
            
            config.put("clientId", googleClientId);
            config.put("clientSecret", googleClientSecret);
            existingProvider.setConfig(config);

            // Update the provider
            realmResource.identityProviders().get("google").update(existingProvider);
            
            log.info("✅ Google identity provider updated successfully");
        } catch (Exception e) {
            log.error("❌ Failed to update Google identity provider: {}", e.getMessage(), e);
        }
    }

    /**
     * Configure user attributes for Google integration
     */
    public void configureUserAttributes() {
        try {
            log.info("🔧 Configuring user attributes for Google integration...");
            
            // Note: Keycloak automatically handles user attributes when they are set
            // We don't need to pre-configure them in the admin API
            // The attributes will be created when users are created/updated
            
            log.info("✅ User attributes configuration completed");
            
        } catch (Exception e) {
            log.error("❌ Failed to configure user attributes: {}", e.getMessage(), e);
        }
    }

    /**
     * Check if Google Identity Provider is configured
     */
    public boolean isGoogleIdentityProviderConfigured() {
        try {
            RealmResource realmResource = keycloak.realm(keycloakProperties.getRealm());
            IdentityProviderRepresentation provider = realmResource.identityProviders().get("google").toRepresentation();
            return provider != null && provider.isEnabled();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get Google Identity Provider configuration status
     */
    public Map<String, Object> getGoogleConfigurationStatus() {
        Map<String, Object> status = new HashMap<>();
        
        try {
            status.put("googleOAuthEnabled", googleOAuthEnabled);
            status.put("googleClientIdConfigured", googleClientId != null && !googleClientId.isEmpty());
            status.put("googleClientSecretConfigured", googleClientSecret != null && !googleClientSecret.isEmpty());
            status.put("identityProviderConfigured", isGoogleIdentityProviderConfigured());
            
            if (isGoogleIdentityProviderConfigured()) {
                RealmResource realmResource = keycloak.realm(keycloakProperties.getRealm());
                IdentityProviderRepresentation provider = realmResource.identityProviders().get("google").toRepresentation();
                status.put("identityProviderEnabled", provider.isEnabled());
                status.put("trustEmail", provider.isTrustEmail());
            }
            
        } catch (Exception e) {
            status.put("error", e.getMessage());
        }
        
        return status;
    }

    /**
     * Manually trigger Google Identity Provider configuration
     * Useful for admin endpoints or configuration updates
     */
    public void reconfigureGoogleIdentityProvider() {
        log.info("🔄 Manually reconfiguring Google Identity Provider...");
        configureGoogleIdentityProvider();
    }

    /**
     * Remove Google Identity Provider (for cleanup/testing)
     */
    public void removeGoogleIdentityProvider() {
        try {
            RealmResource realmResource = keycloak.realm(keycloakProperties.getRealm());
            realmResource.identityProviders().get("google").remove();
            log.info("🗑️ Google identity provider removed successfully");
        } catch (Exception e) {
            log.error("❌ Failed to remove Google identity provider: {}", e.getMessage(), e);
        }
    }
}
