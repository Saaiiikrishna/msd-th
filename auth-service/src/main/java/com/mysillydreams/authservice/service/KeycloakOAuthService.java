package com.mysillydreams.authservice.service;

import com.mysillydreams.authservice.config.KeycloakConstants;
import com.mysillydreams.authservice.config.KeycloakProperties;
import com.mysillydreams.authservice.controller.AuthController;
import com.mysillydreams.authservice.util.KeycloakUrlBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;

/**
 * Service for handling OAuth2/OIDC flows with Keycloak
 * Separated from KeycloakService to follow single responsibility principle
 */
@Service
@Profile("!bootstrap")
@Slf4j
public class KeycloakOAuthService {

    private final KeycloakProperties keycloakProperties;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public KeycloakOAuthService(KeycloakProperties keycloakProperties) {
        this.keycloakProperties = keycloakProperties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(keycloakProperties.getTimeouts().getConnectionTimeout())
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Start OIDC login flow - generate authorization URL with PKCE
     */
    public AuthController.LoginStartResponse startLoginFlow(String redirectUri) {
        try {
            // Generate PKCE parameters
            String codeVerifier = generateCodeVerifier();
            String codeChallenge = generateCodeChallenge(codeVerifier);
            String state = UUID.randomUUID().toString();

            // Use configured redirect URI if none provided
            String finalRedirectUri = redirectUri != null ? redirectUri : 
                    keycloakProperties.getFrontend().getDefaultRedirectUri();

            // Validate redirect URI is allowed
            if (!isAllowedRedirectUri(finalRedirectUri)) {
                log.warn("Unauthorized redirect URI attempted: {}", finalRedirectUri);
                return new AuthController.LoginStartResponse(null, null, null, 
                        "Unauthorized redirect URI");
            }

            // Build authorization URL using utility
            String authorizationUrl = KeycloakUrlBuilder.buildAuthorizationUrlWithPKCE(
                    keycloakProperties.getServerUrl(),
                    keycloakProperties.getRealm(),
                    keycloakProperties.getFrontend().getClientId(),
                    finalRedirectUri,
                    state,
                    codeChallenge
            );

            log.info("Generated authorization URL for OIDC flow");
            return new AuthController.LoginStartResponse(authorizationUrl, codeVerifier, state, null);

        } catch (Exception e) {
            log.error("Failed to start login flow", e);
            return new AuthController.LoginStartResponse(null, null, null, "Failed to start login flow");
        }
    }

    /**
     * Exchange authorization code for tokens
     */
    public AuthController.TokenResponse exchangeCodeForTokens(String code, String state, String codeVerifier) {
        log.info("🔄 Starting token exchange with Keycloak");
        log.info("🔄 Authorization Code: {}...", code.substring(0, Math.min(code.length(), 10)));
        log.info("🔄 State: {}", state);

        try {
            String tokenUrl = keycloakProperties.getTokenEndpointUrl();
            log.info("🔄 Token URL: {}", tokenUrl);

            // Build form data using utility
            String formData = KeycloakUrlBuilder.buildTokenExchangeFormData(
                    keycloakProperties.getFrontend().getClientId(),
                    code,
                    keycloakProperties.getFrontend().getDefaultRedirectUri(),
                    codeVerifier
            );

            // Make HTTP request to Keycloak token endpoint
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(tokenUrl))
                    .timeout(keycloakProperties.getTimeouts().getRequestTimeout())
                    .header(KeycloakConstants.Headers.CONTENT_TYPE, 
                            KeycloakConstants.ContentTypes.APPLICATION_FORM_URLENCODED)
                    .header(KeycloakConstants.Headers.ACCEPT, 
                            KeycloakConstants.ContentTypes.APPLICATION_JSON)
                    .POST(HttpRequest.BodyPublishers.ofString(formData))
                    .build();

            log.info("📡 Sending token exchange request to Keycloak...");
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            log.info("📡 Keycloak token response status: {}", response.statusCode());

            if (response.statusCode() == 200) {
                return parseTokenResponse(response.body());
            } else {
                log.error("❌ Token exchange failed - Status: {}, Body: {}", response.statusCode(), response.body());
                throw new RuntimeException("Token exchange failed with status: " + response.statusCode());
            }

        } catch (Exception e) {
            log.error("❌ Failed to exchange code for tokens", e);
            throw new RuntimeException("Token exchange failed", e);
        }
    }

    /**
     * Refresh access token using refresh token
     */
    public AuthController.TokenResponse refreshTokens(String refreshToken) {
        log.info("🔄 Starting token refresh with Keycloak");
        log.info("🔄 Refresh Token: {}...", refreshToken.substring(0, Math.min(refreshToken.length(), 10)));

        try {
            String tokenUrl = keycloakProperties.getTokenEndpointUrl();
            log.info("🔄 Token URL: {}", tokenUrl);

            // Build form data using utility
            String formData = KeycloakUrlBuilder.buildRefreshTokenFormData(
                    keycloakProperties.getFrontend().getClientId(),
                    refreshToken
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(tokenUrl))
                    .timeout(keycloakProperties.getTimeouts().getRequestTimeout())
                    .header(KeycloakConstants.Headers.CONTENT_TYPE, 
                            KeycloakConstants.ContentTypes.APPLICATION_FORM_URLENCODED)
                    .header(KeycloakConstants.Headers.ACCEPT, 
                            KeycloakConstants.ContentTypes.APPLICATION_JSON)
                    .POST(HttpRequest.BodyPublishers.ofString(formData))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return parseTokenResponse(response.body());
            } else {
                log.error("❌ Token refresh failed - Status: {}, Body: {}", response.statusCode(), response.body());
                throw new RuntimeException("Token refresh failed with status: " + response.statusCode());
            }

        } catch (Exception e) {
            log.error("❌ Failed to refresh tokens", e);
            throw new RuntimeException("Token refresh failed", e);
        }
    }

    /**
     * Get user info from access token
     */
    public AuthController.UserInfoResponse getUserInfo(String accessToken) {
        log.info("🔍 Getting user info from access token");
        log.info("🔍 Access Token: {}...", accessToken.substring(0, Math.min(accessToken.length(), 20)));

        try {
            String userInfoUrl = keycloakProperties.getUserInfoEndpointUrl();
            log.info("🔍 UserInfo URL: {}", userInfoUrl);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(userInfoUrl))
                    .timeout(keycloakProperties.getTimeouts().getTokenValidationTimeout())
                    .header(KeycloakConstants.Headers.AUTHORIZATION, 
                            KeycloakConstants.OAuth2.TOKEN_TYPE_BEARER + " " + accessToken)
                    .header(KeycloakConstants.Headers.ACCEPT, 
                            KeycloakConstants.ContentTypes.APPLICATION_JSON)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return parseUserInfoResponse(response.body());
            } else {
                log.error("❌ User info request failed - Status: {}, Body: {}", response.statusCode(), response.body());
                return new AuthController.UserInfoResponse(null, null, null, null, false, "Failed to get user info");
            }

        } catch (Exception e) {
            log.error("❌ Failed to get user info", e);
            return new AuthController.UserInfoResponse(null, null, null, null, false, "Failed to get user info");
        }
    }

    // Private helper methods

    private boolean isAllowedRedirectUri(String redirectUri) {
        return keycloakProperties.getFrontend().getRedirectUris().stream()
                .anyMatch(allowedUri -> redirectUri.matches(allowedUri.replace("*", ".*")));
    }

    private String generateCodeVerifier() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] codeVerifier = new byte[32];
        secureRandom.nextBytes(codeVerifier);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(codeVerifier);
    }

    private String generateCodeChallenge(String codeVerifier) throws Exception {
        byte[] bytes = codeVerifier.getBytes(StandardCharsets.US_ASCII);
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        messageDigest.update(bytes, 0, bytes.length);
        byte[] digest = messageDigest.digest();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
    }

    private AuthController.TokenResponse parseTokenResponse(String responseBody) throws Exception {
        log.info("🔍 Parsing token response from Keycloak");

        JsonNode tokenResponse = objectMapper.readTree(responseBody);

        String accessToken = tokenResponse.get("access_token").asText();
        String refreshToken = tokenResponse.has("refresh_token") ?
                tokenResponse.get("refresh_token").asText() : null;
        int expiresIn = tokenResponse.has("expires_in") ?
                tokenResponse.get("expires_in").asInt() : KeycloakConstants.Defaults.DEFAULT_TOKEN_EXPIRY_SECONDS;
        String tokenType = tokenResponse.has("token_type") ?
                tokenResponse.get("token_type").asText() : KeycloakConstants.OAuth2.TOKEN_TYPE_BEARER;

        log.info("✅ Token response parsed successfully");
        log.info("✅ Token Type: {}", tokenType);
        log.info("✅ Expires In: {} seconds", expiresIn);

        return new AuthController.TokenResponse(accessToken, refreshToken, tokenType, expiresIn);
    }

    private AuthController.UserInfoResponse parseUserInfoResponse(String responseBody) throws Exception {
        log.info("🔍 Parsing user info response from Keycloak");

        JsonNode userInfoResponse = objectMapper.readTree(responseBody);

        String userRef = null;
        String email = userInfoResponse.has("email") ? userInfoResponse.get("email").asText() : null;
        String firstName = userInfoResponse.has("given_name") ? userInfoResponse.get("given_name").asText() : null;
        String lastName = userInfoResponse.has("family_name") ? userInfoResponse.get("family_name").asText() : null;

        // Try to get user_ref from custom attributes
        if (userInfoResponse.has(KeycloakConstants.UserAttributes.USER_REF)) {
            userRef = userInfoResponse.get(KeycloakConstants.UserAttributes.USER_REF).asText();
        }

        log.info("✅ User info parsed successfully");
        return new AuthController.UserInfoResponse(userRef, email, firstName, lastName, true, null);
    }
}
