package com.mysillydreams.authservice.util;

import com.mysillydreams.authservice.config.KeycloakConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;

/**
 * Utility class for building Keycloak URLs safely
 * Replaces string concatenation with proper URL building
 */
@Slf4j
public final class KeycloakUrlBuilder {

    private KeycloakUrlBuilder() {
        // Utility class - prevent instantiation
    }

    /**
     * Build Keycloak realm base URL
     */
    public static String buildRealmUrl(String serverUrl, String realm) {
        return UriComponentsBuilder.fromHttpUrl(serverUrl)
                .path(KeycloakConstants.Endpoints.REALMS_PATH)
                .path("/{realm}")
                .buildAndExpand(realm)
                .toUriString();
    }

    /**
     * Build OAuth2 authorization URL with parameters
     */
    public static String buildAuthorizationUrl(String serverUrl, String realm, Map<String, String> params) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(serverUrl)
                .path(KeycloakConstants.Endpoints.REALMS_PATH)
                .path("/{realm}")
                .path(KeycloakConstants.Endpoints.AUTH_ENDPOINT);

        // Add query parameters
        params.forEach(builder::queryParam);

        return builder.buildAndExpand(realm).toUriString();
    }

    /**
     * Build token endpoint URL
     */
    public static String buildTokenUrl(String serverUrl, String realm) {
        return UriComponentsBuilder.fromHttpUrl(serverUrl)
                .path(KeycloakConstants.Endpoints.REALMS_PATH)
                .path("/{realm}")
                .path(KeycloakConstants.Endpoints.TOKEN_ENDPOINT)
                .buildAndExpand(realm)
                .toUriString();
    }

    /**
     * Build user info endpoint URL
     */
    public static String buildUserInfoUrl(String serverUrl, String realm) {
        return UriComponentsBuilder.fromHttpUrl(serverUrl)
                .path(KeycloakConstants.Endpoints.REALMS_PATH)
                .path("/{realm}")
                .path(KeycloakConstants.Endpoints.USERINFO_ENDPOINT)
                .buildAndExpand(realm)
                .toUriString();
    }

    /**
     * Build logout endpoint URL
     */
    public static String buildLogoutUrl(String serverUrl, String realm) {
        return UriComponentsBuilder.fromHttpUrl(serverUrl)
                .path(KeycloakConstants.Endpoints.REALMS_PATH)
                .path("/{realm}")
                .path(KeycloakConstants.Endpoints.LOGOUT_ENDPOINT)
                .buildAndExpand(realm)
                .toUriString();
    }

    /**
     * Build JWK Set URI
     */
    public static String buildJwkSetUri(String serverUrl, String realm) {
        return UriComponentsBuilder.fromHttpUrl(serverUrl)
                .path(KeycloakConstants.Endpoints.REALMS_PATH)
                .path("/{realm}")
                .path(KeycloakConstants.Endpoints.CERTS_ENDPOINT)
                .buildAndExpand(realm)
                .toUriString();
    }

    /**
     * Build OAuth2 authorization URL with PKCE parameters
     */
    public static String buildAuthorizationUrlWithPKCE(String serverUrl, String realm, String clientId, 
                                                       String redirectUri, String state, String codeChallenge) {
        return UriComponentsBuilder.fromHttpUrl(serverUrl)
                .path(KeycloakConstants.Endpoints.REALMS_PATH)
                .path("/{realm}")
                .path(KeycloakConstants.Endpoints.AUTH_ENDPOINT)
                .queryParam("client_id", clientId)
                .queryParam("response_type", KeycloakConstants.OAuth2.RESPONSE_TYPE_CODE)
                .queryParam("scope", KeycloakConstants.OAuth2.SCOPE_OPENID_PROFILE_EMAIL)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("state", state)
                .queryParam("code_challenge", codeChallenge)
                .queryParam("code_challenge_method", KeycloakConstants.OAuth2.CODE_CHALLENGE_METHOD_S256)
                .buildAndExpand(realm)
                .toUriString();
    }

    /**
     * Build form data for token exchange
     */
    public static String buildTokenExchangeFormData(String clientId, String code, String redirectUri, String codeVerifier) {
        return UriComponentsBuilder.newInstance()
                .queryParam("grant_type", KeycloakConstants.OAuth2.GRANT_TYPE_AUTHORIZATION_CODE)
                .queryParam("client_id", clientId)
                .queryParam("code", code)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("code_verifier", codeVerifier)
                .build()
                .getQuery();
    }

    /**
     * Build form data for refresh token
     */
    public static String buildRefreshTokenFormData(String clientId, String refreshToken) {
        return UriComponentsBuilder.newInstance()
                .queryParam("grant_type", KeycloakConstants.OAuth2.GRANT_TYPE_REFRESH_TOKEN)
                .queryParam("client_id", clientId)
                .queryParam("refresh_token", refreshToken)
                .build()
                .getQuery();
    }

    /**
     * Build form data for password grant
     */
    public static String buildPasswordGrantFormData(String clientId, String username, String password) {
        return UriComponentsBuilder.newInstance()
                .queryParam("grant_type", KeycloakConstants.OAuth2.GRANT_TYPE_PASSWORD)
                .queryParam("client_id", clientId)
                .queryParam("username", username)
                .queryParam("password", password)
                .build()
                .getQuery();
    }



    /**
     * Validate URL format
     */
    public static boolean isValidUrl(String url) {
        try {
            URI.create(url);
            return true;
        } catch (Exception e) {
            log.warn("Invalid URL format: {}", url);
            return false;
        }
    }
}
