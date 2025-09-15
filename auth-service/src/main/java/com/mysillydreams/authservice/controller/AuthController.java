package com.mysillydreams.authservice.controller;

import com.mysillydreams.authservice.service.KeycloakService;
import com.mysillydreams.authservice.service.KeycloakConfigurationService;
import com.mysillydreams.authservice.service.KeycloakOAuthService;
import com.mysillydreams.authservice.service.UserManagementService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Authentication controller
 * Handles user registration and JWT token management
 * Only active when NOT in bootstrap mode
 */
@RestController
@RequestMapping("/api/auth/v1")
@Validated
@Slf4j
@Profile("!bootstrap")
@Tag(name = "Authentication", description = "Authentication and JWT token management APIs")
public class AuthController {

    private final KeycloakService keycloakService;
    private final KeycloakOAuthService keycloakOAuthService;
    private final KeycloakConfigurationService keycloakConfigurationService;
    private final UserManagementService userManagementService;


    public AuthController(KeycloakService keycloakService,
                         KeycloakOAuthService keycloakOAuthService,
                         KeycloakConfigurationService keycloakConfigurationService,
                         UserManagementService userManagementService) {
        this.keycloakService = keycloakService;
        this.keycloakOAuthService = keycloakOAuthService;
        this.keycloakConfigurationService = keycloakConfigurationService;
        this.userManagementService = userManagementService;
    }

    /**
     * User registration endpoint
     * Creates a new user in Keycloak and returns user_ref
     */
    @PostMapping("/register")
    @Operation(
        summary = "Register new user",
        description = "Creates a new user account in Keycloak and returns user reference ID"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User registered successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid registration data"),
        @ApiResponse(responseCode = "409", description = "User already exists")
    })
    public ResponseEntity<RegistrationResponse> register(@RequestHeader HttpHeaders headers, @Valid @RequestBody RegistrationRequest request) {
        log.info("🎯 === REGISTRATION REQUEST REACHED CONTROLLER ===");
        log.info("🎯 Headers from Gateway: X-Gateway={}, X-Service={}", headers.getFirst("X-Gateway"), headers.getFirst("X-Service"));
        log.info("🎯 User registration request for email: {}", request.getEmail());
        log.info("🎯 Request details: firstName={}, lastName={}, phone={}, gender={}",
                request.getFirstName(), request.getLastName(), request.getPhone(), request.getGender());
        log.info("🎯 Consent details: terms={}, privacy={}, marketing={}",
                request.getAcceptTerms(), request.getAcceptPrivacy(), request.getMarketingConsent());

        String userRef = userManagementService.createUser(
                request.getEmail(),
                request.getPassword(),
                request.getFirstName(),
                request.getLastName(),
                request.getPhone(),
                request.getDateOfBirth(),
                request.getGender(),
                request.getAcceptTerms(),
                request.getAcceptPrivacy(),
                request.getMarketingConsent()
        );

        RegistrationResponse response = new RegistrationResponse(
                "User registered successfully",
                userRef,
                request.getEmail()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Login start endpoint - returns authorization URL for OIDC flow
     * Part of BFF (Backend for Frontend) pattern
     */
    @GetMapping("/login-start")
    @Operation(
        summary = "Start OIDC login flow",
        description = "Returns authorization URL and PKCE parameters for frontend to initiate login"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Login flow started successfully"),
        @ApiResponse(responseCode = "500", description = "Failed to start login flow")
    })
    public ResponseEntity<LoginStartResponse> loginStart(
            @RequestParam(required = false) String redirectUri) {

        try {
            LoginStartResponse response = keycloakOAuthService.startLoginFlow(redirectUri);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to start login flow", e);
            return ResponseEntity.status(500)
                    .body(new LoginStartResponse(null, null, null, "Failed to start login flow"));
        }
    }

    /**
     * OAuth2 callback endpoint - handles authorization code exchange
     * Part of BFF (Backend for Frontend) pattern
     */
    @GetMapping("/callback")
    @Operation(
        summary = "Handle OAuth2 callback",
        description = "Exchanges authorization code for tokens and sets secure cookies"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "302", description = "Redirect to frontend with tokens set as cookies"),
        @ApiResponse(responseCode = "400", description = "Invalid authorization code or state"),
        @ApiResponse(responseCode = "500", description = "Token exchange failed")
    })
    public ResponseEntity<Void> handleCallback(
            @RequestParam String code,
            @RequestParam String state,
            @RequestParam(required = false) String codeVerifier,
            HttpServletResponse response) {

        try {
            TokenResponse tokenResponse = keycloakOAuthService.exchangeCodeForTokens(code, state, codeVerifier);

            // Set secure HTTP-only cookies
            setTokenCookies(response, tokenResponse);

            // Redirect to frontend
            String redirectUrl = extractRedirectUrlFromState(state);
            return ResponseEntity.status(302)
                    .header("Location", redirectUrl != null ? redirectUrl : "http://localhost:3000/")
                    .build();

        } catch (Exception e) {
            log.error("OAuth2 callback failed", e);
            return ResponseEntity.status(302)
                    .header("Location", "http://localhost:3000/login?error=callback_failed")
                    .build();
        }
    }

    /**
     * Token refresh endpoint
     * Part of BFF (Backend for Frontend) pattern
     */
    @PostMapping("/refresh")
    @Operation(
        summary = "Refresh access token",
        description = "Uses refresh token from cookie to get new access token"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Token refreshed successfully"),
        @ApiResponse(responseCode = "401", description = "Invalid or expired refresh token")
    })
    public ResponseEntity<RefreshResponse> refreshToken(
            HttpServletRequest request,
            HttpServletResponse response) {

        try {
            String refreshToken = extractRefreshTokenFromCookie(request);
            if (refreshToken == null) {
                return ResponseEntity.status(401)
                        .body(new RefreshResponse(false, "No refresh token found"));
            }

            TokenResponse tokenResponse = keycloakOAuthService.refreshTokens(refreshToken);

            // Set new secure HTTP-only cookies
            setTokenCookies(response, tokenResponse);

            return ResponseEntity.ok(new RefreshResponse(true, "Token refreshed successfully"));

        } catch (Exception e) {
            log.error("Token refresh failed", e);
            return ResponseEntity.status(401)
                    .body(new RefreshResponse(false, "Token refresh failed"));
        }
    }

    /**
     * Logout endpoint
     * Part of BFF (Backend for Frontend) pattern
     */
    @PostMapping("/logout")
    @Operation(
        summary = "Logout user",
        description = "Invalidates tokens and clears cookies"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Logout successful"),
        @ApiResponse(responseCode = "500", description = "Logout failed")
    })
    public ResponseEntity<LogoutResponse> logout(
            HttpServletRequest request,
            HttpServletResponse response) {

        try {
            String refreshToken = extractRefreshTokenFromCookie(request);
            if (refreshToken != null) {
                keycloakService.logout(refreshToken);
            }

            // Clear cookies
            clearTokenCookies(response);

            return ResponseEntity.ok(new LogoutResponse(true, "Logout successful"));

        } catch (Exception e) {
            log.error("Logout failed", e);
            return ResponseEntity.ok(new LogoutResponse(true, "Logout completed")); // Always return success for logout
        }
    }

    /**
     * Get current user info endpoint
     * Part of BFF (Backend for Frontend) pattern
     */
    @GetMapping("/me")
    @Operation(
        summary = "Get current user info",
        description = "Returns user profile information from access token"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User info retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "No valid access token found")
    })
    public ResponseEntity<UserInfoResponse> getCurrentUser(
            HttpServletRequest request) {

        try {
            String accessToken = extractAccessTokenFromCookie(request);
            if (accessToken == null) {
                return ResponseEntity.status(401)
                        .body(new UserInfoResponse(null, null, null, null, false, "No access token found"));
            }

            UserInfoResponse userInfo = keycloakOAuthService.getUserInfo(accessToken);
            return ResponseEntity.ok(userInfo);

        } catch (Exception e) {
            log.error("Failed to get user info", e);
            return ResponseEntity.status(401)
                    .body(new UserInfoResponse(null, null, null, null, false, "Failed to get user info"));
        }
    }

    /**
     * Direct login endpoint
     * Authenticates user credentials directly against Keycloak
     */
    @PostMapping("/login")
    @Operation(
        summary = "Direct login with credentials",
        description = "Authenticates user with email/password and returns access tokens. " +
                     "For web clients with cookie consent, also sets secure HTTP-only cookies."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Login successful"),
        @ApiResponse(responseCode = "401", description = "Invalid credentials"),
        @ApiResponse(responseCode = "500", description = "Login failed")
    })
    public ResponseEntity<LoginResponse> login(
            @RequestBody @Valid LoginRequest request,
            @RequestHeader(value = "X-Client-Type", defaultValue = "web") String clientType,
            @RequestHeader(value = "X-Cookie-Consent", defaultValue = "false") boolean cookieConsent,
            @RequestHeader(value = "User-Agent", required = false) String userAgent,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        log.info("🔐 Direct login attempt for email: {} from client: {}", request.getEmail(), clientType);

        try {
            LoginResponse response = keycloakService.authenticateUser(request.getEmail(), request.getPassword());

            // Enhanced Hybrid Approach: Set cookies for web clients with consent
            if (shouldSetCookies(clientType, cookieConsent, userAgent)) {
                log.debug("🍪 Setting secure cookies for web client with consent");
                TokenResponse tokenResponse = new TokenResponse(
                    response.getAccessToken(),
                    response.getRefreshToken(),
                    response.getTokenType(),
                    response.getExpiresIn()
                );
                setTokenCookies(httpResponse, tokenResponse);

                // Add cookie info to response
                response = new LoginResponse(
                    response.getAccessToken(),
                    response.getRefreshToken(),
                    response.getTokenType(),
                    response.getExpiresIn(),
                    response.getUser(),
                    response.getError(),
                    true, // cookiesSet
                    detectClientInfo(clientType, userAgent)
                );
            } else {
                log.debug("📱 Token-only authentication for client: {}", clientType);
                // Add client info to response
                response = new LoginResponse(
                    response.getAccessToken(),
                    response.getRefreshToken(),
                    response.getTokenType(),
                    response.getExpiresIn(),
                    response.getUser(),
                    response.getError(),
                    false, // cookiesSet
                    detectClientInfo(clientType, userAgent)
                );
            }

            log.info("✅ Login successful for user: {} (client: {})", request.getEmail(), clientType);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ Login failed for user: {} (client: {})", request.getEmail(), clientType, e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new LoginResponse(null, null, null, 0, null, "Invalid credentials", false, null));
        }
    }

    /**
     * Google OAuth authentication endpoint
     * Handles Google ID token verification and user authentication
     */
    @PostMapping("/google-oauth")
    @Operation(
        summary = "Authenticate with Google OAuth",
        description = "Verifies Google ID token and authenticates/registers user"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Authentication successful"),
        @ApiResponse(responseCode = "401", description = "Invalid Google token"),
        @ApiResponse(responseCode = "500", description = "Authentication failed")
    })
    public ResponseEntity<LoginResponse> googleOAuth(
            @RequestBody @Valid GoogleOAuthRequest request,
            @RequestHeader(value = "X-Client-Type", defaultValue = "web") String clientType,
            @RequestHeader(value = "X-Cookie-Consent", defaultValue = "false") boolean cookieConsent,
            @RequestHeader(value = "User-Agent", required = false) String userAgent,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        log.info("🔐 Google OAuth authentication attempt from client: {}", clientType);

        try {
            LoginResponse response = keycloakService.authenticateWithGoogle(request.getCredential());

            // Enhanced Hybrid Approach: Set cookies for web clients with consent
            if (shouldSetCookies(clientType, cookieConsent, userAgent)) {
                log.debug("🍪 Setting secure cookies for Google OAuth web client");
                TokenResponse tokenResponse = new TokenResponse(
                    response.getAccessToken(),
                    response.getRefreshToken(),
                    response.getTokenType(),
                    response.getExpiresIn()
                );
                setTokenCookies(httpResponse, tokenResponse);

                response = new LoginResponse(
                    response.getAccessToken(),
                    response.getRefreshToken(),
                    response.getTokenType(),
                    response.getExpiresIn(),
                    response.getUser(),
                    response.getError(),
                    true, // cookiesSet
                    detectClientInfo(clientType, userAgent)
                );
            } else {
                response = new LoginResponse(
                    response.getAccessToken(),
                    response.getRefreshToken(),
                    response.getTokenType(),
                    response.getExpiresIn(),
                    response.getUser(),
                    response.getError(),
                    false, // cookiesSet
                    detectClientInfo(clientType, userAgent)
                );
            }

            log.info("✅ Google OAuth successful (client: {})", clientType);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ Google OAuth failed (client: {})", clientType, e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new LoginResponse(null, null, null, 0, null, "Google authentication failed", false, null));
        }
    }

    /**
     * Google OAuth configuration status endpoint
     * Returns current Google OAuth configuration status
     */
    @GetMapping("/google-config-status")
    @Operation(
        summary = "Get Google OAuth configuration status",
        description = "Returns the current status of Google OAuth configuration in Keycloak"
    )
    public ResponseEntity<Map<String, Object>> getGoogleConfigStatus() {
        try {
            Map<String, Object> status = keycloakConfigurationService.getGoogleConfigurationStatus();
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            log.error("Failed to get Google configuration status", e);
            Map<String, Object> errorStatus = new HashMap<>();
            errorStatus.put("error", "Failed to get configuration status");
            errorStatus.put("message", e.getMessage());
            return ResponseEntity.status(500).body(errorStatus);
        }
    }

    /**
     * Reconfigure Google Identity Provider endpoint
     * Manually triggers Google Identity Provider configuration
     */
    @PostMapping("/reconfigure-google")
    @Operation(
        summary = "Reconfigure Google Identity Provider",
        description = "Manually triggers Google Identity Provider configuration in Keycloak"
    )
    public ResponseEntity<Map<String, String>> reconfigureGoogle() {
        try {
            keycloakConfigurationService.reconfigureGoogleIdentityProvider();
            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Google Identity Provider reconfigured successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to reconfigure Google Identity Provider", e);
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Failed to reconfigure Google Identity Provider: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Login configuration endpoint
     * Returns Keycloak login URLs and configuration
     */
    @GetMapping("/login-info")
    @Operation(
        summary = "Get login configuration",
        description = "Returns Keycloak login URLs and configuration for frontend applications"
    )
    public ResponseEntity<LoginInfoResponse> getLoginInfo() {
        String keycloakUrl = "http://localhost:8080"; // This should come from configuration
        String realm = "treasure-hunt";
        String clientId = "frontend-client";

        LoginInfoResponse response = new LoginInfoResponse(
            keycloakUrl + "/realms/" + realm + "/protocol/openid_connect/auth",
            keycloakUrl + "/realms/" + realm + "/protocol/openid_connect/token",
            keycloakUrl + "/realms/" + realm + "/protocol/openid_connect/logout",
            clientId,
            realm
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Returns the health status of the auth service")
    public ResponseEntity<HealthResponse> health() {
        return ResponseEntity.ok(new HealthResponse("Auth service is healthy", "UP"));
    }

    /**
     * Test endpoint
     */
    @GetMapping("/test")
    @Operation(summary = "Test endpoint", description = "Simple test endpoint to verify connectivity")
    public ResponseEntity<Map<String, Object>> test() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Auth service is working");
        response.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(response);
    }

    // Helper methods for BFF pattern

    private void setTokenCookies(HttpServletResponse response, TokenResponse tokenResponse) {
        // Set access token cookie (short-lived, 5-10 minutes)
        Cookie accessTokenCookie = new Cookie("access_token", tokenResponse.getAccessToken());
        accessTokenCookie.setHttpOnly(true);
        accessTokenCookie.setSecure(true); // Use HTTPS in production
        accessTokenCookie.setPath("/");
        accessTokenCookie.setMaxAge(600); // 10 minutes
        accessTokenCookie.setAttribute("SameSite", "Lax");
        response.addCookie(accessTokenCookie);

        // Set refresh token cookie (longer-lived, 8-12 hours)
        if (tokenResponse.getRefreshToken() != null) {
            Cookie refreshTokenCookie = new Cookie("refresh_token", tokenResponse.getRefreshToken());
            refreshTokenCookie.setHttpOnly(true);
            refreshTokenCookie.setSecure(true); // Use HTTPS in production
            refreshTokenCookie.setPath("/api/auth");
            refreshTokenCookie.setMaxAge(28800); // 8 hours
            refreshTokenCookie.setAttribute("SameSite", "Lax");
            response.addCookie(refreshTokenCookie);
        }
    }

    private void clearTokenCookies(HttpServletResponse response) {
        // Clear access token cookie
        Cookie accessTokenCookie = new Cookie("access_token", "");
        accessTokenCookie.setHttpOnly(true);
        accessTokenCookie.setSecure(true);
        accessTokenCookie.setPath("/");
        accessTokenCookie.setMaxAge(0);
        response.addCookie(accessTokenCookie);

        // Clear refresh token cookie
        Cookie refreshTokenCookie = new Cookie("refresh_token", "");
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setSecure(true);
        refreshTokenCookie.setPath("/api/auth");
        refreshTokenCookie.setMaxAge(0);
        response.addCookie(refreshTokenCookie);
    }

    private String extractAccessTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("access_token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    private String extractRefreshTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("refresh_token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    private String extractRedirectUrlFromState(String state) {
        // In a real implementation, you would decode the state parameter
        // For now, return default redirect
        return "http://localhost:3000/dashboard";
    }

    /**
     * Determines if cookies should be set based on client type and consent
     */
    private boolean shouldSetCookies(String clientType, boolean cookieConsent, String userAgent) {
        // Only set cookies for web clients
        if (!"web".equalsIgnoreCase(clientType)) {
            return false;
        }

        // Check if user agent suggests a web browser
        if (userAgent != null && isWebBrowser(userAgent)) {
            // Require explicit consent for cookie usage
            return cookieConsent;
        }

        return false;
    }

    /**
     * Detects if the user agent is a web browser
     */
    private boolean isWebBrowser(String userAgent) {
        if (userAgent == null) return false;

        String ua = userAgent.toLowerCase();
        // Check for mobile app patterns first
        if (ua.contains("mysillydreams-android") || ua.contains("mysillydreams-ios") ||
            ua.contains("okhttp") || ua.contains("alamofire")) {
            return false;
        }

        // Check for web browser patterns
        return ua.contains("mozilla") || ua.contains("chrome") || ua.contains("safari") ||
               ua.contains("firefox") || ua.contains("edge") || ua.contains("opera");
    }

    /**
     * Detects client information for response
     */
    private ClientInfo detectClientInfo(String clientType, String userAgent) {
        String detectedType = clientType;
        String platform = "unknown";

        if (userAgent != null) {
            String ua = userAgent.toLowerCase();

            // Mobile app detection
            if (ua.contains("mysillydreams-android") || ua.contains("android")) {
                detectedType = "mobile";
                platform = "android";
            } else if (ua.contains("mysillydreams-ios") || ua.contains("ios") || ua.contains("iphone") || ua.contains("ipad")) {
                detectedType = "mobile";
                platform = "ios";
            } else if (isWebBrowser(userAgent)) {
                detectedType = "web";
                if (ua.contains("chrome")) platform = "chrome";
                else if (ua.contains("firefox")) platform = "firefox";
                else if (ua.contains("safari")) platform = "safari";
                else if (ua.contains("edge")) platform = "edge";
                else platform = "browser";
            }
        }

        return new ClientInfo(detectedType, platform, userAgent);
    }

    // DTOs
    
    @Data
    @Schema(description = "User registration request")
    public static class RegistrationRequest {

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        @Schema(description = "User email address", example = "user@example.com")
        private String email;

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        @Schema(description = "User password", example = "SecurePassword123")
        private String password;

        @NotBlank(message = "First name is required")
        @Size(max = 50, message = "First name must not exceed 50 characters")
        @Schema(description = "User first name", example = "John")
        private String firstName;

        @NotBlank(message = "Last name is required")
        @Size(max = 50, message = "Last name must not exceed 50 characters")
        @Schema(description = "User last name", example = "Doe")
        private String lastName;

        @Schema(description = "User phone number", example = "+91 9876543210")
        private String phone;

        @Schema(description = "User date of birth", example = "1990-01-01")
        @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "Date of birth must be in YYYY-MM-DD format")
        private String dateOfBirth;

        @Schema(description = "User gender", example = "MALE", allowableValues = {"MALE", "FEMALE", "OTHER"})
        private String gender;

        @Schema(description = "Accept terms of service", example = "true")
        private Boolean acceptTerms;

        @Schema(description = "Accept privacy policy", example = "true")
        private Boolean acceptPrivacy;

        @Schema(description = "Marketing consent", example = "false")
        private Boolean marketingConsent;
    }

    @Data
    @Schema(description = "User login request")
    public static class LoginRequest {
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        @Schema(description = "User email address", example = "user@example.com")
        private String email;

        @NotBlank(message = "Password is required")
        @Schema(description = "User password")
        private String password;

        @Schema(description = "Remember me flag", example = "false")
        private Boolean rememberMe;
    }

    @Data
    @Schema(description = "Google OAuth authentication request")
    public static class GoogleOAuthRequest {
        @NotBlank(message = "Google credential is required")
        @Schema(description = "Google ID token credential", example = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...")
        private String credential;
    }

    @Data
    @Schema(description = "User registration response")
    public static class RegistrationResponse {
        private final String message;
        private final String userRef;
        private final String email;
    }

    @Data
    @Schema(description = "User login response")
    public static class LoginResponse {
        private final String accessToken;
        private final String refreshToken;
        private final String tokenType;
        private final Integer expiresIn;
        private final UserInfo user;
        private final String error;

        @Schema(description = "Whether secure cookies were set for this login")
        private final Boolean cookiesSet;

        @Schema(description = "Client information detected from request")
        private final ClientInfo clientInfo;

        public LoginResponse(String accessToken, String refreshToken, String tokenType, Integer expiresIn, UserInfo user, String error) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.tokenType = tokenType;
            this.expiresIn = expiresIn;
            this.user = user;
            this.error = error;
            this.cookiesSet = false;
            this.clientInfo = null;
        }

        public LoginResponse(String accessToken, String refreshToken, String tokenType, Integer expiresIn, UserInfo user, String error, Boolean cookiesSet, ClientInfo clientInfo) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.tokenType = tokenType;
            this.expiresIn = expiresIn;
            this.user = user;
            this.error = error;
            this.cookiesSet = cookiesSet;
            this.clientInfo = clientInfo;
        }

        @Data
        @Schema(description = "User information")
        public static class UserInfo {
            private final String id;
            private final String referenceId;
            private final String firstName;
            private final String lastName;
            private final String email;
            private final List<String> roles;
            private final Boolean active;
        }
    }

    @Data
    @Schema(description = "Client information")
    public static class ClientInfo {
        @Schema(description = "Client type (web, mobile, api)")
        private final String type;

        @Schema(description = "Platform (chrome, firefox, android, ios, etc.)")
        private final String platform;

        @Schema(description = "User agent string")
        private final String userAgent;
    }

    @Data
    @Schema(description = "Login information response")
    public static class LoginInfoResponse {
        private final String authUrl;
        private final String tokenUrl;
        private final String logoutUrl;
        private final String clientId;
        private final String realm;
    }

    @Data
    @Schema(description = "Health check response")
    public static class HealthResponse {
        private final String message;
        private final String status;
    }

    // BFF Response DTOs

    @Data
    @Schema(description = "Login start response")
    public static class LoginStartResponse {
        private final String authorizationUrl;
        private final String codeVerifier;
        private final String state;
        private final String error;
    }

    @Data
    @Schema(description = "Token response")
    public static class TokenResponse {
        private final String accessToken;
        private final String refreshToken;
        private final String tokenType;
        private final Integer expiresIn;
    }

    @Data
    @Schema(description = "Refresh response")
    public static class RefreshResponse {
        private final boolean success;
        private final String message;
    }

    @Data
    @Schema(description = "Logout response")
    public static class LogoutResponse {
        private final boolean success;
        private final String message;
    }

    @Data
    @Schema(description = "User info response")
    public static class UserInfoResponse {
        private final String userRef;
        private final String email;
        private final String firstName;
        private final String lastName;
        private final boolean active;
        private final String error;
    }
}
