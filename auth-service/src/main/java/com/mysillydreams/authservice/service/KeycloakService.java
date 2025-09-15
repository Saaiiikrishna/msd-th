package com.mysillydreams.authservice.service;

import com.mysillydreams.authservice.config.KeycloakConstants;
import com.mysillydreams.authservice.config.KeycloakProperties;
import com.mysillydreams.authservice.controller.AuthController;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.context.annotation.Profile;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.ws.rs.core.Response;
import java.util.*;

/**
 * Keycloak Admin Service - Thin wrapper around Keycloak Admin Client
 *
 * SINGLE RESPONSIBILITY: Keycloak administrative operations only
 *
 * What this service does:
 * - Create/delete/update users in Keycloak
 * - Manage roles and role assignments
 * - Manage clients and realm configuration
 * - Basic user lookups
 *
 * What this service does NOT do:
 * - Business logic validation (Keycloak handles this)
 * - Integration with other services (use orchestration services)
 * - Metrics (use AOP or orchestration layer)
 * - Complex error handling (let exceptions bubble up)
 *
 * OAuth2/OIDC flows are handled by KeycloakOAuthService
 * Only active when NOT in bootstrap mode
 */
@Service
@Profile("!bootstrap")
@Slf4j
@RequiredArgsConstructor
public class KeycloakService {

    private final Keycloak keycloak;
    private final KeycloakProperties keycloakProperties;
    private final RestTemplate restTemplate;
    private final KeycloakClientSecretService clientSecretService;
    private final ObjectMapper objectMapper;
    private final GoogleTokenVerificationService googleTokenVerificationService;

    // Constructor handled by @RequiredArgsConstructor
    @PostConstruct
    public void init() {
        log.info("🔧 KeycloakService initialized with realm: {}", keycloakProperties.getRealm());
    }

    /**
     * Create a user in Keycloak only
     * Keycloak handles ALL validation (email format, password policy, required fields, etc.)
     *
     * @param email User email (will be used as username)
     * @param password User password
     * @param firstName User first name
     * @param lastName User last name
     * @param userRef External user reference (UUID)
     * @return Keycloak user ID
     * @throws RuntimeException if user creation fails (with Keycloak's validation errors)
     */
    public String createUser(String email, String password, String firstName, String lastName, String userRef) {
        log.debug("Creating user in Keycloak: {}", email);

        RealmResource realmResource = keycloak.realm(keycloakProperties.getRealm());

        // Create user representation - NO validation, let Keycloak handle it
        UserRepresentation user = new UserRepresentation();
        user.setUsername(email);
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEnabled(true);
        user.setEmailVerified(false);

        // Set user attributes including user_ref
        if (userRef != null) {
            Map<String, List<String>> attributes = new HashMap<>();
            attributes.put(KeycloakConstants.UserAttributes.USER_REF, Arrays.asList(userRef));
            user.setAttributes(attributes);
        }

        // Create user - Keycloak will validate everything and return appropriate errors
        try (Response response = realmResource.users().create(user)) {
            if (response.getStatus() == 201) {
                String userId = extractUserIdFromLocation(response.getLocation().getPath());

                // Set password - Keycloak will validate password policy
                setUserPassword(userId, password);

                // Assign default role
                assignRoleToUser(userId, KeycloakConstants.Roles.ROLE_CUSTOMER);

                log.debug("Successfully created user in Keycloak with ID: {}", userId);
                return userId;

            } else if (response.getStatus() == 409) {
                throw new RuntimeException("User already exists: " + email);
            } else if (response.getStatus() == 400) {
                throw new RuntimeException("Invalid user data - check Keycloak validation rules");
            } else {
                throw new RuntimeException("Failed to create user. Status: " + response.getStatus());
            }
        } catch (Exception e) {
            log.error("Error creating user in Keycloak: {}", email, e);
            throw new RuntimeException("Failed to create user in Keycloak: " + e.getMessage(), e);
        }
    }

    /**
     * Authenticate user with email and password using Keycloak token endpoint
     * Uses Resource Owner Password Credentials flow
     */
    public AuthController.LoginResponse authenticateUser(String email, String password) {
        log.info("🔐 Authenticating user with Keycloak: {}", email);

        try {
            // Build token endpoint URL
            String tokenUrl = String.format("%s/realms/%s/protocol/openid-connect/token",
                    keycloakProperties.getServerUrl(),
                    keycloakProperties.getRealm());

            // Prepare request headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            // Prepare request body for Resource Owner Password Credentials flow
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "password");
            body.add("client_id", keycloakProperties.getClientId());
            body.add("client_secret", getClientSecret());
            body.add("username", email);
            body.add("password", password);
            body.add("scope", "openid profile email");

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

            // Make token request
            ResponseEntity<String> response = restTemplate.postForEntity(tokenUrl, request, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                // Parse token response
                TokenResponse tokenResponse = objectMapper.readValue(response.getBody(), TokenResponse.class);

                // Get user info from Keycloak
                Optional<UserRepresentation> userOpt = getUserByEmail(email);
                if (userOpt.isEmpty()) {
                    throw new RuntimeException("User not found after successful authentication");
                }

                UserRepresentation keycloakUser = userOpt.get();

                // Get user_ref from attributes
                String userRef = null;
                if (keycloakUser.getAttributes() != null && keycloakUser.getAttributes().containsKey("user_ref")) {
                    List<String> userRefList = keycloakUser.getAttributes().get("user_ref");
                    if (userRefList != null && !userRefList.isEmpty()) {
                        userRef = userRefList.get(0);
                    }
                }

                // Get user roles
                List<String> roles = getUserRoles(keycloakUser.getId());

                // Build user info
                AuthController.LoginResponse.UserInfo userInfo = new AuthController.LoginResponse.UserInfo(
                        keycloakUser.getId(),
                        userRef,
                        keycloakUser.getFirstName(),
                        keycloakUser.getLastName(),
                        keycloakUser.getEmail(),
                        roles,
                        keycloakUser.isEnabled()
                );

                // Build login response
                return new AuthController.LoginResponse(
                        tokenResponse.getAccessToken(),
                        tokenResponse.getRefreshToken(),
                        tokenResponse.getTokenType(),
                        tokenResponse.getExpiresIn(),
                        userInfo,
                        null
                );

            } else {
                log.warn("Authentication failed for user: {} - Status: {}", email, response.getStatusCode());
                throw new RuntimeException("Invalid credentials");
            }

        } catch (Exception e) {
            log.error("Authentication error for user: {}", email, e);
            throw new RuntimeException("Authentication failed: " + e.getMessage(), e);
        }
    }

    /**
     * Logout user by invalidating refresh token
     * Calls Keycloak's logout endpoint to invalidate the session
     */
    public void logout(String refreshToken) {
        log.info("🚪 Logging out user with refresh token");

        try {
            // Build logout endpoint URL
            String logoutUrl = String.format("%s/realms/%s/protocol/openid-connect/logout",
                    keycloakProperties.getServerUrl(),
                    keycloakProperties.getRealm());

            // Prepare logout request
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("client_id", keycloakProperties.getClientId());
            body.add("client_secret", getClientSecret());
            body.add("refresh_token", refreshToken);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

            // Make logout request
            ResponseEntity<String> response = restTemplate.postForEntity(logoutUrl, request, String.class);

            if (response.getStatusCode() == HttpStatus.OK || response.getStatusCode() == HttpStatus.NO_CONTENT) {
                log.info("✅ User logged out successfully");
            } else {
                log.warn("⚠️ Logout request returned status: {}", response.getStatusCode());
            }

        } catch (Exception e) {
            log.error("❌ Logout failed: {}", e.getMessage(), e);
            // Don't throw exception for logout failures - always allow logout to complete
        }
    }

    /**
     * Authenticate user with Google ID token
     * Handles both new user registration and existing user login scenarios
     */
    public AuthController.LoginResponse authenticateWithGoogle(String googleIdToken) {
        log.info("🔐 Authenticating user with Google ID token");

        try {
            // Verify and decode Google ID token using secure verification service
            GoogleTokenVerificationService.GoogleUserInfo googleUser = googleTokenVerificationService.verifyGoogleIdToken(googleIdToken);

            // Check if user exists in Keycloak
            Optional<UserRepresentation> existingUserOpt = getUserByEmail(googleUser.getEmail());
            UserRepresentation existingUser = existingUserOpt.orElse(null);

            if (existingUser != null) {
                // Scenario 2: Existing user logging in with Google
                log.info("👤 Existing user found, linking Google account: {}", googleUser.getEmail());
                return handleExistingUserGoogleLogin(existingUser, googleUser);
            } else {
                // Scenario 1: New user registering with Google
                log.info("🆕 New user registering with Google: {}", googleUser.getEmail());
                return handleNewUserGoogleRegistration(googleUser);
            }

        } catch (Exception e) {
            log.error("❌ Google authentication failed: {}", e.getMessage(), e);
            throw new RuntimeException("Google authentication failed: " + e.getMessage(), e);
        }
    }



    /**
     * Handle existing user logging in with Google
     * This ensures the SAME USER ACCOUNT is used regardless of login method
     */
    private AuthController.LoginResponse handleExistingUserGoogleLogin(UserRepresentation existingUser, GoogleTokenVerificationService.GoogleUserInfo googleUser) {
        try {
            log.info("🔗 Linking Google account to existing user: {} (ID: {})", existingUser.getEmail(), existingUser.getId());

            // Check if Google account is already linked
            Map<String, List<String>> attributes = existingUser.getAttributes();
            if (attributes == null) {
                attributes = new HashMap<>();
            }

            boolean alreadyLinked = attributes.containsKey("google_id") &&
                                 attributes.get("google_id").contains(googleUser.getGoogleId());

            if (!alreadyLinked) {
                // Link Google account to existing user
                attributes.put("google_id", List.of(googleUser.getGoogleId()));
                attributes.put("google_picture", List.of(googleUser.getPicture()));
                attributes.put("google_linked_at", List.of(String.valueOf(System.currentTimeMillis())));

                // Update profile with Google data if not already set
                if (existingUser.getFirstName() == null || existingUser.getFirstName().isEmpty()) {
                    existingUser.setFirstName(googleUser.getFirstName());
                }
                if (existingUser.getLastName() == null || existingUser.getLastName().isEmpty()) {
                    existingUser.setLastName(googleUser.getLastName());
                }

                existingUser.setAttributes(attributes);

                // Update user in Keycloak
                UsersResource usersResource = keycloak.realm(keycloakProperties.getRealm()).users();
                usersResource.get(existingUser.getId()).update(existingUser);

                log.info("✅ Google account successfully linked to existing user: {}", existingUser.getEmail());
            } else {
                log.info("ℹ️ Google account already linked to user: {}", existingUser.getEmail());
            }

            // Generate tokens for the SAME user account
            return generateTokensForUser(existingUser, googleUser);

        } catch (Exception e) {
            log.error("❌ Failed to handle existing user Google login: {}", e.getMessage());
            throw new RuntimeException("Failed to link Google account", e);
        }
    }

    /**
     * Handle new user registration with Google
     * Creates a NEW user account that can later be linked with email/password
     */
    private AuthController.LoginResponse handleNewUserGoogleRegistration(GoogleTokenVerificationService.GoogleUserInfo googleUser) {
        try {
            log.info("🆕 Creating new user account for Google user: {}", googleUser.getEmail());

            // Double-check user doesn't exist (race condition protection)
            Optional<UserRepresentation> existingUserOpt = getUserByEmail(googleUser.getEmail());
            if (existingUserOpt.isPresent()) {
                log.info("🔄 User was created by another process, linking Google account instead");
                return handleExistingUserGoogleLogin(existingUserOpt.get(), googleUser);
            }

            // Create new user in Keycloak
            UserRepresentation newUser = new UserRepresentation();
            newUser.setUsername(googleUser.getEmail()); // Use email as username for consistency
            newUser.setEmail(googleUser.getEmail());
            newUser.setFirstName(googleUser.getFirstName());
            newUser.setLastName(googleUser.getLastName());
            newUser.setEmailVerified(googleUser.isEmailVerified());
            newUser.setEnabled(true);

            // Add Google-specific attributes for account linking
            Map<String, List<String>> attributes = new HashMap<>();
            attributes.put("google_id", List.of(googleUser.getGoogleId()));
            attributes.put("google_picture", List.of(googleUser.getPicture()));
            attributes.put("registration_method", List.of("google"));
            attributes.put("google_registered_at", List.of(String.valueOf(System.currentTimeMillis())));
            attributes.put("account_linking_enabled", List.of("true")); // Allow future email/password linking
            newUser.setAttributes(attributes);

            // Create user in Keycloak
            UsersResource usersResource = keycloak.realm(keycloakProperties.getRealm()).users();
            Response response = usersResource.create(newUser);

            if (response.getStatus() == 201) {
                // Get the created user
                String userId = extractUserIdFromLocation(response.getLocation().toString());
                UserRepresentation createdUser = usersResource.get(userId).toRepresentation();

                // Assign default role
                assignRoleToUser(userId, KeycloakConstants.Roles.ROLE_CUSTOMER);

                // Create user profile in user service
                createUserProfileInUserService(createdUser, googleUser);

                log.info("✅ New user account created successfully: {} (ID: {})", createdUser.getEmail(), userId);

                // Generate tokens for the new user
                return generateTokensForUser(createdUser, googleUser);

            } else {
                String errorMsg = "Failed to create user in Keycloak. Status: " + response.getStatus();
                log.error("❌ {}", errorMsg);
                throw new RuntimeException(errorMsg);
            }

        } catch (Exception e) {
            log.error("❌ Failed to register new user with Google: {}", e.getMessage());
            throw new RuntimeException("Failed to register user with Google", e);
        }
    }

    /**
     * Generate tokens for authenticated user
     */
    private AuthController.LoginResponse generateTokensForUser(UserRepresentation user, GoogleTokenVerificationService.GoogleUserInfo googleUser) {
        try {
            // For Google OAuth, we need to generate tokens differently
            // This is a simplified approach - in production, use proper OAuth flow

            // Create user info for response
            AuthController.LoginResponse.UserInfo userInfo = new AuthController.LoginResponse.UserInfo(
                user.getId(),
                user.getUsername(), // This will be the reference ID
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                getUserRoles(user.getId()),
                user.isEnabled()
            );

            // For now, return a success response without actual tokens
            // In production, you would generate proper JWT tokens here
            return new AuthController.LoginResponse(
                "google_oauth_token_placeholder", // TODO: Generate actual access token
                "google_oauth_refresh_placeholder", // TODO: Generate actual refresh token
                "Bearer",
                3600, // 1 hour
                userInfo,
                null
            );

        } catch (Exception e) {
            log.error("❌ Failed to generate tokens for user: {}", e.getMessage());
            throw new RuntimeException("Failed to generate tokens", e);
        }
    }

    /**
     * Create user profile in user service
     */
    private void createUserProfileInUserService(UserRepresentation user, GoogleTokenVerificationService.GoogleUserInfo googleUser) {
        try {
            // TODO: Call user service to create user profile
            log.info("📝 Creating user profile in user service for: {}", user.getEmail());

            // This would typically make an HTTP call to the user service
            // For now, just log the action

        } catch (Exception e) {
            log.warn("⚠️ Failed to create user profile in user service: {}", e.getMessage());
            // Don't fail the authentication if user service is down
        }
    }



    /**
     * Get user roles from Keycloak
     */
    private List<String> getUserRoles(String userId) {
        try {
            RealmResource realmResource = keycloak.realm(keycloakProperties.getRealm());
            return realmResource.users().get(userId).roles().realmLevel().listAll()
                    .stream()
                    .map(RoleRepresentation::getName)
                    .filter(role -> !role.startsWith("default-") && !role.equals("offline_access") && !role.equals("uma_authorization"))
                    .toList();
        } catch (Exception e) {
            log.warn("Failed to get user roles for user: {}", userId, e);
            return List.of("USER"); // Default role
        }
    }

    /**
     * Token response from Keycloak
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class TokenResponse {
        @JsonProperty("access_token")
        private String accessToken;

        @JsonProperty("refresh_token")
        private String refreshToken;

        @JsonProperty("token_type")
        private String tokenType;

        @JsonProperty("expires_in")
        private Integer expiresIn;

        // Getters
        public String getAccessToken() { return accessToken; }
        public String getRefreshToken() { return refreshToken; }
        public String getTokenType() { return tokenType; }
        public Integer getExpiresIn() { return expiresIn; }
    }

    /**
     * Check if a user exists in Keycloak by email
     */
    public boolean userExists(String email) {
        try {
            RealmResource realmResource = keycloak.realm(keycloakProperties.getRealm());
            List<UserRepresentation> users = realmResource.users().search(email, true);
            return users != null && !users.isEmpty();
        } catch (Exception e) {
            log.error("Error checking if user exists: {}", email, e);
            return false;
        }
    }

    /**
     * Get user by email
     */
    public Optional<UserRepresentation> getUserByEmail(String email) {
        try {
            RealmResource realmResource = keycloak.realm(keycloakProperties.getRealm());
            List<UserRepresentation> users = realmResource.users().search(email, true);
            return users.isEmpty() ? Optional.empty() : Optional.of(users.get(0));
        } catch (Exception e) {
            log.error("Error getting user by email: {}", email, e);
            return Optional.empty();
        }
    }

    /**
     * Get user by ID
     */
    public Optional<UserRepresentation> getUserById(String userId) {
        try {
            RealmResource realmResource = keycloak.realm(keycloakProperties.getRealm());
            UserRepresentation user = realmResource.users().get(userId).toRepresentation();
            return Optional.of(user);
        } catch (Exception e) {
            log.error("Error getting user by ID: {}", userId, e);
            return Optional.empty();
        }
    }

    /**
     * Delete user from Keycloak
     */
    public void deleteUser(String userId) {
        try {
            keycloak.realm(keycloakProperties.getRealm()).users().delete(userId);
            log.debug("Successfully deleted user: {}", userId);
        } catch (Exception e) {
            log.error("Error deleting user: {}", userId, e);
            throw new RuntimeException("Failed to delete user", e);
        }
    }

    /**
     * Set user password in Keycloak
     * Keycloak will validate password against configured password policies
     */
    private void setUserPassword(String userId, String password) {
        try {
            CredentialRepresentation credential = new CredentialRepresentation();
            credential.setType(CredentialRepresentation.PASSWORD);
            credential.setValue(password);
            credential.setTemporary(false);

            // Keycloak will validate password policy here
            keycloak.realm(keycloakProperties.getRealm()).users().get(userId).resetPassword(credential);
            log.debug("Successfully set password for user: {}", userId);
        } catch (Exception e) {
            log.error("Error setting password for user: {} - Keycloak validation failed", userId, e);
            throw new RuntimeException("Failed to set user password - check Keycloak password policy: " + e.getMessage(), e);
        }
    }

    /**
     * Assign role to user
     */
    public void assignRoleToUser(String userId, String roleName) {
        try {
            RealmResource realmResource = keycloak.realm(keycloakProperties.getRealm());
            RoleRepresentation role = realmResource.roles().get(roleName).toRepresentation();
            realmResource.users().get(userId).roles().realmLevel().add(Arrays.asList(role));
            log.debug("Successfully assigned role {} to user: {}", roleName, userId);
        } catch (Exception e) {
            log.error("Error assigning role {} to user: {}", roleName, userId, e);
            throw new RuntimeException("Failed to assign role to user", e);
        }
    }

    /**
     * Remove role from user
     */
    public void removeRoleFromUser(String userId, String roleName) {
        try {
            RealmResource realmResource = keycloak.realm(keycloakProperties.getRealm());
            RoleRepresentation role = realmResource.roles().get(roleName).toRepresentation();
            realmResource.users().get(userId).roles().realmLevel().remove(Arrays.asList(role));
            log.debug("Successfully removed role {} from user: {}", roleName, userId);
        } catch (Exception e) {
            log.error("Error removing role {} from user: {}", roleName, userId, e);
            throw new RuntimeException("Failed to remove role from user", e);
        }
    }

    /**
     * Extract user ID from Keycloak location header
     */
    private String extractUserIdFromLocation(String locationPath) {
        String[] parts = locationPath.split("/");
        return parts[parts.length - 1];
    }

    // === REALM MANAGEMENT METHODS ===

    /**
     * Initialize realm with default roles and clients
     */
    public void initializeRealm() {
        try {
            RealmResource realmResource = keycloak.realm(keycloakProperties.getRealm());

            // Create default roles
            createRoleIfNotExists(KeycloakConstants.Roles.ROLE_CUSTOMER, "Default customer role");
            createRoleIfNotExists(KeycloakConstants.Roles.ROLE_ADMIN, "Administrator role");
            createRoleIfNotExists(KeycloakConstants.Roles.ROLE_INTERNAL_CONSUMER, "Internal service consumer role");

            // Create auth-service client for password grant authentication
            log.info("🔧 About to create auth-service client...");
            createAuthServiceClientIfNotExists();
            log.info("🔧 Auth-service client creation completed");

            // Create frontend client if configured
            if (keycloakProperties.getFrontend() != null &&
                keycloakProperties.getFrontend().getClientId() != null) {
                createClientIfNotExists(keycloakProperties.getFrontend().getClientId(), "Frontend application client");
            }

            log.info("Realm initialization completed for: {}", keycloakProperties.getRealm());
        } catch (Exception e) {
            log.error("Error initializing realm: {}", keycloakProperties.getRealm(), e);
            throw new RuntimeException("Failed to initialize realm", e);
        }
    }

    /**
     * Create role if it doesn't exist
     */
    public void createRoleIfNotExists(String roleName, String description) {
        try {
            RealmResource realmResource = keycloak.realm(keycloakProperties.getRealm());
            realmResource.roles().get(roleName).toRepresentation();
            log.debug("Role already exists: {}", roleName);
        } catch (Exception e) {
            // Role doesn't exist, create it
            try {
                RealmResource realmResource = keycloak.realm(keycloakProperties.getRealm());
                RoleRepresentation role = new RoleRepresentation();
                role.setName(roleName);
                role.setDescription(description);
                realmResource.roles().create(role);
                log.info("Created role: {}", roleName);
            } catch (Exception createException) {
                log.error("Failed to create role: {}", roleName, createException);
                throw new RuntimeException("Failed to create role: " + roleName, createException);
            }
        }
    }

    /**
     * Create client if it doesn't exist
     */
    public void createClientIfNotExists(String clientId, String description) {
        try {
            RealmResource realmResource = keycloak.realm(keycloakProperties.getRealm());
            List<ClientRepresentation> existingClients = realmResource.clients().findByClientId(clientId);
            if (existingClients != null && !existingClients.isEmpty()) {
                log.debug("Client already exists: {}", clientId);
                return;
            }

            // Create new client
            ClientRepresentation client = new ClientRepresentation();
            client.setClientId(clientId);
            client.setName(description);
            client.setDescription(description);
            client.setEnabled(true);
            client.setPublicClient(true);
            client.setStandardFlowEnabled(true);
            client.setDirectAccessGrantsEnabled(true);
            client.setImplicitFlowEnabled(false);
            client.setServiceAccountsEnabled(false);

            // Set redirect URIs and web origins if configured
            if (keycloakProperties.getFrontend() != null) {
                if (keycloakProperties.getFrontend().getRedirectUris() != null) {
                    client.setRedirectUris(keycloakProperties.getFrontend().getRedirectUris());
                }
                if (keycloakProperties.getFrontend().getWebOrigins() != null) {
                    client.setWebOrigins(keycloakProperties.getFrontend().getWebOrigins());
                }
            }

            realmResource.clients().create(client);
            log.info("Created client: {}", clientId);
        } catch (Exception e) {
            log.error("Failed to create client: {}", clientId, e);
            throw new RuntimeException("Failed to create client: " + clientId, e);
        }
    }

    /**
     * Create auth-service client for password grant authentication
     */
    public void createAuthServiceClientIfNotExists() {
        String clientId = keycloakProperties.getClientId();
        String clientSecret = getClientSecret();

        log.info("🔧 Creating/updating auth-service client: {} with secret: {}", clientId, clientSecret != null ? "YES" : "NO");

        try {
            RealmResource realmResource = keycloak.realm(keycloakProperties.getRealm());
            List<ClientRepresentation> existingClients = realmResource.clients().findByClientId(clientId);

            if (existingClients != null && !existingClients.isEmpty()) {
                // Client exists, update its secret and configuration
                ClientRepresentation existingClient = existingClients.get(0);
                String existingClientUuid = existingClient.getId();

                log.info("🔧 Auth service client already exists, updating configuration and secret");

                // Update the client configuration
                existingClient.setName("Auth Service Backend Client");
                existingClient.setDescription("Backend service client for password grant authentication");
                existingClient.setEnabled(true);
                existingClient.setPublicClient(false); // Confidential client
                existingClient.setStandardFlowEnabled(true);
                existingClient.setDirectAccessGrantsEnabled(true); // Enable password grant
                existingClient.setImplicitFlowEnabled(false);
                existingClient.setServiceAccountsEnabled(true); // Enable service accounts
                existingClient.setSecret(clientSecret);

                // Update the client
                realmResource.clients().get(existingClientUuid).update(existingClient);
                log.info("✅ Updated auth-service client: {} with new secret", clientId);
                return;
            }

            // Create confidential client for auth-service
            ClientRepresentation client = new ClientRepresentation();
            client.setClientId(clientId);
            client.setName("Auth Service Backend Client");
            client.setDescription("Backend service client for password grant authentication");
            client.setEnabled(true);
            client.setPublicClient(false); // Confidential client
            client.setStandardFlowEnabled(true);
            client.setDirectAccessGrantsEnabled(true); // Enable password grant
            client.setImplicitFlowEnabled(false);
            client.setServiceAccountsEnabled(true); // Enable service accounts
            client.setSecret(clientSecret);

            // Set protocol mappers for user info
            client.setProtocolMappers(new ArrayList<>());

            realmResource.clients().create(client);
            log.info("✅ Created auth-service client: {} with secret", clientId);
        } catch (Exception e) {
            log.error("❌ Failed to create/update auth-service client: {}", clientId, e);
            throw new RuntimeException("Failed to create/update auth-service client: " + clientId, e);
        }
    }

    /**
     * Get user_ref from Keycloak user attributes
     */
    public Optional<String> getUserRefFromKeycloak(String email) {
        try {
            Optional<UserRepresentation> userOpt = getUserByEmail(email);
            if (userOpt.isPresent()) {
                UserRepresentation user = userOpt.get();
                Map<String, List<String>> attributes = user.getAttributes();
                if (attributes != null && attributes.containsKey(KeycloakConstants.UserAttributes.USER_REF)) {
                    List<String> userRefList = attributes.get(KeycloakConstants.UserAttributes.USER_REF);
                    if (userRefList != null && !userRefList.isEmpty()) {
                        return Optional.of(userRefList.get(0));
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error getting user_ref from Keycloak for email: {}", email, e);
        }
        return Optional.empty();
    }

    /**
     * Get the current client secret from the client secret service
     * Falls back to configuration if the service is not available
     * @return The client secret
     */
    private String getClientSecret() {
        // Try to get the secret from the dynamic service first
        if (clientSecretService.isClientSecretAvailable()) {
            String dynamicSecret = clientSecretService.getCurrentClientSecret();
            if (dynamicSecret != null && !dynamicSecret.isEmpty()) {
                return dynamicSecret;
            }
        }

        // Fallback to configuration
        log.warn("⚠️ Using fallback client secret from configuration. Dynamic secret service may not be ready.");
        return keycloakProperties.getCredentials().getSecret();
    }

}
