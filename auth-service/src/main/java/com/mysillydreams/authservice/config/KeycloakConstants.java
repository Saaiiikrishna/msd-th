package com.mysillydreams.authservice.config;

/**
 * Constants for Keycloak integration
 * Centralizes all hardcoded values and magic strings
 */
public final class KeycloakConstants {

    private KeycloakConstants() {
        // Utility class - prevent instantiation
    }

    // Keycloak Endpoints
    public static final class Endpoints {
        public static final String REALMS_PATH = "/realms";
        public static final String PROTOCOL_OPENID_CONNECT = "/protocol/openid-connect";
        public static final String AUTH_ENDPOINT = PROTOCOL_OPENID_CONNECT + "/auth";
        public static final String TOKEN_ENDPOINT = PROTOCOL_OPENID_CONNECT + "/token";
        public static final String USERINFO_ENDPOINT = PROTOCOL_OPENID_CONNECT + "/userinfo";
        public static final String LOGOUT_ENDPOINT = PROTOCOL_OPENID_CONNECT + "/logout";
        public static final String CERTS_ENDPOINT = PROTOCOL_OPENID_CONNECT + "/certs";
        
        private Endpoints() {}
    }

    // OAuth2 Parameters
    public static final class OAuth2 {
        public static final String GRANT_TYPE = "grant_type";
        public static final String GRANT_TYPE_AUTHORIZATION_CODE = "authorization_code";
        public static final String GRANT_TYPE_REFRESH_TOKEN = "refresh_token";
        public static final String GRANT_TYPE_PASSWORD = "password";
        public static final String RESPONSE_TYPE_CODE = "code";
        public static final String SCOPE_OPENID_PROFILE_EMAIL = "openid profile email";
        public static final String CODE_CHALLENGE_METHOD_S256 = "S256";
        public static final String TOKEN_TYPE_BEARER = "Bearer";
        public static final String CLIENT_ID = "client_id";
        public static final String REFRESH_TOKEN = "refresh_token";
        public static final String CODE = "code";
        public static final String REDIRECT_URI = "redirect_uri";
        public static final String CODE_VERIFIER = "code_verifier";
        public static final String STATE = "state";
        public static final String CODE_CHALLENGE = "code_challenge";
        public static final String CODE_CHALLENGE_METHOD = "code_challenge_method";
        public static final String RESPONSE_TYPE = "response_type";
        public static final String SCOPE = "scope";

        private OAuth2() {}
    }

    // User Attributes
    public static final class UserAttributes {
        public static final String USER_REF = "user_ref";
        public static final String EMAIL_VERIFIED = "email_verified";
        public static final String PREFERRED_USERNAME = "preferred_username";
        
        private UserAttributes() {}
    }

    // Default Roles
    public static final class Roles {
        public static final String ROLE_CUSTOMER = "ROLE_CUSTOMER";
        public static final String ROLE_USER = "USER";
        public static final String ROLE_ADMIN = "ADMIN";
        public static final String ROLE_INTERNAL_CONSUMER = "ROLE_INTERNAL_CONSUMER";

        private Roles() {}
    }

    // Client Configuration
    public static final class ClientConfig {
        public static final String FRONTEND_CLIENT_ID = "frontend-client";
        public static final String ADMIN_CLI_CLIENT_ID = "admin-cli";
        public static final String MASTER_REALM = "master";
        
        private ClientConfig() {}
    }

    // HTTP Headers
    public static final class Headers {
        public static final String AUTHORIZATION = "Authorization";
        public static final String CONTENT_TYPE = "Content-Type";
        public static final String ACCEPT = "Accept";
        public static final String USER_AGENT = "User-Agent";
        
        private Headers() {}
    }

    // Content Types
    public static final class ContentTypes {
        public static final String APPLICATION_JSON = "application/json";
        public static final String APPLICATION_FORM_URLENCODED = "application/x-www-form-urlencoded";
        
        private ContentTypes() {}
    }

    // Default Values
    public static final class Defaults {
        public static final int DEFAULT_TOKEN_EXPIRY_SECONDS = 3600;
        public static final int DEFAULT_TIMEOUT_SECONDS = 30;
        public static final String DEFAULT_USER_AGENT = "AuthService/1.0";
        
        private Defaults() {}
    }

    // Validation
    public static final class Validation {
        public static final int MIN_PASSWORD_LENGTH = 8;
        public static final int MAX_EMAIL_LENGTH = 255;
        public static final int MAX_NAME_LENGTH = 100;
        
        private Validation() {}
    }
}
