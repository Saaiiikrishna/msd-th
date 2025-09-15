package com.mysillydreams.authservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Duration;
import java.util.List;

/**
 * Configuration properties for Keycloak integration
 * Replaces hardcoded values with proper configuration
 */
@Data
@Component
@ConfigurationProperties(prefix = "keycloak")
@Validated
public class KeycloakProperties {

    @NotBlank(message = "Keycloak realm is required")
    private String realm;

    @NotBlank(message = "Keycloak server URL is required")
    private String serverUrl;

    @NotBlank(message = "Keycloak auth server URL is required")
    private String authServerUrl;

    @NotBlank(message = "Keycloak client ID is required")
    private String clientId;

    @Valid
    @NotNull
    private Credentials credentials = new Credentials();

    @Valid
    @NotNull
    private Admin admin = new Admin();

    @Valid
    @NotNull
    private Frontend frontend = new Frontend();

    @Valid
    @NotNull
    private Timeouts timeouts = new Timeouts();

    @Data
    public static class Credentials {
        @NotBlank(message = "Keycloak client secret is required")
        private String secret;
    }

    @Data
    public static class Admin {
        @NotBlank(message = "Keycloak admin username is required")
        private String username;

        @NotBlank(message = "Keycloak admin password is required")
        private String password;

        @NotBlank(message = "Keycloak admin client ID is required")
        private String clientId = "admin-cli";
    }

    @Data
    public static class Frontend {
        @NotBlank(message = "Frontend client ID is required")
        private String clientId = "frontend-client";

        @NotEmpty(message = "At least one redirect URI is required")
        private List<String> redirectUris = List.of(
            "http://localhost:3000/auth/callback",
            "https://app.mysillydreams.com/auth/callback"
        );

        @NotEmpty(message = "At least one web origin is required")
        private List<String> webOrigins = List.of(
            "http://localhost:3000",
            "https://app.mysillydreams.com"
        );

        @NotBlank(message = "Default redirect URI is required")
        private String defaultRedirectUri = "http://localhost:3000/auth/callback";
    }

    @Data
    public static class Timeouts {
        @NotNull
        private Duration connectionTimeout = Duration.ofSeconds(10);

        @NotNull
        private Duration requestTimeout = Duration.ofSeconds(30);

        @NotNull
        private Duration tokenValidationTimeout = Duration.ofSeconds(5);
    }

    // Computed properties for convenience
    public String getRealmUrl() {
        return serverUrl + "/realms/" + realm;
    }

    public String getTokenEndpointUrl() {
        return getRealmUrl() + "/protocol/openid-connect/token";
    }

    public String getUserInfoEndpointUrl() {
        return getRealmUrl() + "/protocol/openid-connect/userinfo";
    }

    public String getAuthEndpointUrl() {
        return getRealmUrl() + "/protocol/openid-connect/auth";
    }

    public String getLogoutEndpointUrl() {
        return getRealmUrl() + "/protocol/openid-connect/logout";
    }

    public String getJwkSetUri() {
        return getRealmUrl() + "/protocol/openid-connect/certs";
    }
}
