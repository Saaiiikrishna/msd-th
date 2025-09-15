package com.mysillydreams.authservice.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Bootstrap Controller for Auth Service
 * Provides endpoints for initial setup and realm management
 * Only active in bootstrap mode
 */
@RestController
@RequestMapping("/v1")
@Profile("bootstrap")
@Slf4j
public class BootstrapController {

    @Value("${keycloak.auth-server-url}")
    private String keycloakServerUrl;

    @Value("${keycloak.realm}")
    private String realmName;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Health check endpoint for bootstrap mode
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("mode", "bootstrap");
        health.put("keycloakUrl", keycloakServerUrl);
        health.put("targetRealm", realmName);
        
        // Check if Keycloak is accessible
        try {
            restTemplate.getForEntity(keycloakServerUrl, String.class);
            health.put("keycloakStatus", "accessible");
        } catch (Exception e) {
            health.put("keycloakStatus", "not accessible");
            health.put("keycloakError", e.getMessage());
        }
        
        return ResponseEntity.ok(health);
    }

    /**
     * Check realm status
     */
    @GetMapping("/admin/realm/status")
    public ResponseEntity<Map<String, Object>> realmStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("realmName", realmName);
        
        try {
            // Try to access the realm's well-known configuration
            String wellKnownUrl = keycloakServerUrl + "/realms/" + realmName + "/.well-known/openid_configuration";
            restTemplate.getForEntity(wellKnownUrl, String.class);
            status.put("exists", true);
            status.put("wellKnownUrl", wellKnownUrl);
        } catch (Exception e) {
            status.put("exists", false);
            status.put("error", "Realm does not exist or is not accessible");
        }
        
        return ResponseEntity.ok(status);
    }

    /**
     * Instructions for manual realm creation
     */
    @GetMapping("/admin/realm/instructions")
    public ResponseEntity<Map<String, Object>> realmInstructions() {
        Map<String, Object> instructions = new HashMap<>();
        instructions.put("message", "Manual realm creation required");
        instructions.put("steps", new String[]{
            "1. Access Keycloak Admin Console: " + keycloakServerUrl + "/admin",
            "2. Login with admin/admin",
            "3. Create a new realm called '" + realmName + "'",
            "4. Create a client called 'auth-service' with:",
            "   - Client authentication: ON",
            "   - Authorization: OFF", 
            "   - Standard flow: ON",
            "   - Direct access grants: ON",
            "   - Client secret: 'your-client-secret'",
            "5. Create roles: ROLE_CUSTOMER, ROLE_ADMIN, ROLE_INTERNAL_CONSUMER",
            "6. Set ROLE_CUSTOMER as default role",
            "7. Restart auth-service with 'docker' profile"
        });
        instructions.put("adminConsoleUrl", keycloakServerUrl + "/admin");
        instructions.put("realmName", realmName);
        
        return ResponseEntity.ok(instructions);
    }

    /**
     * Service information
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> info() {
        Map<String, Object> info = new HashMap<>();
        info.put("service", "auth-service");
        info.put("mode", "bootstrap");
        info.put("description", "Auth service running in bootstrap mode - realm setup required");
        info.put("keycloakUrl", keycloakServerUrl);
        info.put("targetRealm", realmName);
        
        return ResponseEntity.ok(info);
    }
}
