package com.mysillydreams.authservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing user lifecycle across multiple services
 * Orchestrates user creation between Keycloak and user-service
 *
 * This service ensures:
 * 1. Atomic user creation across auth and profile services
 * 2. Proper error handling and rollback mechanisms
 * 3. Consent management and compliance
 * 4. Service-to-service authentication
 * 5. Comprehensive audit logging
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserManagementService {

    private final KeycloakService keycloakService;
    private final WebClient.Builder webClientBuilder;

    @Value("${services.user-service.url:http://localhost:8082}")
    private String userServiceUrl;

    @Value("${services.user-service.internal-api-key:auth-service-internal-key-12345}")
    private String internalApiKey;

    @Value("${services.user-service.timeout:30s}")
    private Duration requestTimeout;

    /**
     * Create a complete user account across all services
     *
     * This method implements the complete user registration workflow:
     * 1. Generate unique user reference ID
     * 2. Create user in Keycloak (authentication & authorization)
     * 3. Create user profile in user-service (profile data & consents)
     * 4. Handle rollback if any step fails
     * 5. Return user reference for client use
     *
     * @param email User email address
     * @param password User password (will be hashed by Keycloak)
     * @param firstName User first name
     * @param lastName User last name
     * @param phone User phone number (optional)
     * @param gender User gender (optional)
     * @param acceptTerms Whether user accepted terms of service
     * @param acceptPrivacy Whether user accepted privacy policy
     * @param marketingConsent Whether user consented to marketing emails
     * @return User reference ID for the created user
     * @throws UserCreationException if user creation fails
     */
    @Transactional
    public String createUser(String email, String password, String firstName, String lastName,
                           String phone, String dateOfBirth, String gender, boolean acceptTerms,
                           boolean acceptPrivacy, boolean marketingConsent) {

        long startTime = System.currentTimeMillis();
        String maskedEmail = maskEmail(email);

        log.info("🎯 === STARTING COMPLETE USER REGISTRATION ===");
        log.info("🎯 Email: {}", maskedEmail);
        log.info("🎯 Name: {} {}", firstName, lastName);
        log.info("🎯 Phone: {}", phone != null ? maskPhone(phone) : "N/A");
        log.info("🎯 Gender: {}", gender);
        log.info("🎯 Consents - Terms: {}, Privacy: {}, Marketing: {}", acceptTerms, acceptPrivacy, marketingConsent);

        String userRef = null;
        String keycloakUserId = null;

        try {
            // Step 1: Generate unique user reference
            userRef = generateUserReference();
            log.info("📋 Generated user reference: {}", userRef);

            // Step 2: Create user in Keycloak
            log.info("🔐 Step 1: Creating user in Keycloak...");
            keycloakUserId = keycloakService.createUser(email, password, firstName, lastName, userRef);
            log.info("✅ User created in Keycloak - ID: {}, Ref: {}", keycloakUserId, userRef);

            // Step 3: Create user profile in user-service
            log.info("👤 Step 2: Creating user profile in user-service...");
            try {
                UserProfileResponse profileResponse = createUserProfileInUserService(
                    userRef, email, firstName, lastName, phone, dateOfBirth, gender,
                    acceptTerms, acceptPrivacy, marketingConsent
                ).block(requestTimeout);

                if (profileResponse != null && "SUCCESS".equals(profileResponse.getStatus())) {
                    log.info("✅ User profile created successfully: {}", profileResponse.getMessage());
                } else {
                    log.warn("⚠️ User profile creation returned unexpected status: {}",
                        profileResponse != null ? profileResponse.getStatus() : "null");
                }

            } catch (Exception e) {
                log.error("❌ Failed to create user profile in user-service: {} - {}",
                    maskedEmail, e.getMessage());

                // Rollback Keycloak user creation
                log.warn("🔄 Rolling back Keycloak user creation due to profile creation failure...");
                try {
                    keycloakService.deleteUser(keycloakUserId);
                    log.info("✅ Keycloak user rollback completed");
                } catch (Exception rollbackException) {
                    log.error("❌ Failed to rollback Keycloak user: {}", rollbackException.getMessage());
                }

                throw new UserCreationException(
                    "Failed to create user profile: " + e.getMessage(), e);
            }

            long processingTime = System.currentTimeMillis() - startTime;
            log.info("🎉 === USER REGISTRATION COMPLETED SUCCESSFULLY ===");
            log.info("🎉 User Reference: {}", userRef);
            log.info("🎉 Processing Time: {}ms", processingTime);

            return userRef;

        } catch (UserCreationException e) {
            // Re-throw user creation exceptions as-is
            throw e;
        } catch (Exception e) {
            log.error("❌ Unexpected error during user creation: {} - {}", maskedEmail, e.getMessage(), e);

            // Attempt cleanup if we have a Keycloak user ID
            if (keycloakUserId != null) {
                log.warn("🔄 Attempting cleanup of Keycloak user due to unexpected error...");
                try {
                    keycloakService.deleteUser(keycloakUserId);
                    log.info("✅ Cleanup completed");
                } catch (Exception cleanupException) {
                    log.error("❌ Failed to cleanup Keycloak user: {}", cleanupException.getMessage());
                }
            }

            throw new UserCreationException("Unexpected error during user creation: " + e.getMessage(), e);
        }
    }

    /**
     * Create user profile in user-service using WebClient
     * Implements proper retry logic and error handling
     */
    private Mono<UserProfileResponse> createUserProfileInUserService(
            String userRef, String email, String firstName, String lastName,
            String phone, String dateOfBirth, String gender, boolean acceptTerms,
            boolean acceptPrivacy, boolean marketingConsent) {

        log.info("🚀 Starting user profile creation in User Service");
        log.info("🚀 User Reference ID: {}", userRef);
        log.info("🚀 Target Endpoint: {}/api/v1/users", userServiceUrl);

        UserCreateRequest request = createUserCreateRequest(
            userRef, email, firstName, lastName, phone, dateOfBirth, gender,
            acceptTerms, acceptPrivacy, marketingConsent
        );

        return webClientBuilder.build()
                .post()
                .uri(userServiceUrl + "/api/v1/users")
                .header("X-Internal-API-Key", internalApiKey)
                .header("Content-Type", "application/json")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(UserServiceApiResponse.class)
                .map(apiResponse -> {
                    if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                        UserProfileResponse response = new UserProfileResponse();
                        response.setUserRef(apiResponse.getData().getReferenceId());
                        response.setEmail(apiResponse.getData().getEmail());
                        response.setFirstName(apiResponse.getData().getFirstName());
                        response.setLastName(apiResponse.getData().getLastName());
                        response.setStatus("SUCCESS");
                        response.setMessage(apiResponse.getMessage());
                        return response;
                    } else {
                        throw new RuntimeException("User service returned error: " + apiResponse.getMessage());
                    }
                })
                .timeout(requestTimeout)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                    .filter(throwable -> !(throwable instanceof WebClientResponseException.BadRequest)))
                .doOnSubscribe(subscription ->
                    log.info("📡 Sending HTTP POST request to User Service..."))
                .doOnSuccess(response ->
                    log.info("✅ Successfully created user profile for user_ref: {}", userRef))
                .doOnError(error ->
                    log.error("❌ Failed to create user profile: {}", error.getMessage()));
    }

    /**
     * Create UserCreateRequest from registration data
     * Maps auth-service registration data to user-service format
     */
    private UserCreateRequest createUserCreateRequest(
            String userRef, String email, String firstName, String lastName,
            String phone, String dateOfBirth, String gender, boolean acceptTerms,
            boolean acceptPrivacy, boolean marketingConsent) {

        UserCreateRequest request = new UserCreateRequest();
        request.setReferenceId(userRef);
        request.setEmail(email);
        request.setFirstName(firstName);
        request.setLastName(lastName);
        request.setPhone(phone);
        request.setDob(dateOfBirth);
        request.setGender(gender);

        // Create consent list based on user choices
        List<ConsentRequest> consents = new ArrayList<>();

        if (acceptTerms) {
            consents.add(new ConsentRequest("terms_of_service", true));
        }

        if (acceptPrivacy) {
            consents.add(new ConsentRequest("privacy_policy", true));
        }

        if (marketingConsent) {
            consents.add(new ConsentRequest("marketing_emails", true));
        }

        request.setConsents(consents);
        return request;
    }

    /**
     * Generate unique user reference ID
     */
    private String generateUserReference() {
        return UUID.randomUUID().toString();
    }

    /**
     * Mask email for logging (GDPR compliance)
     */
    private String maskEmail(String email) {
        if (email == null || email.length() < 3) {
            return "***";
        }
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) {
            return "***@" + email.substring(atIndex + 1);
        }
        return email.substring(0, 1) + "***@" + email.substring(atIndex + 1);
    }

    /**
     * Mask phone for logging (GDPR compliance)
     */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) {
            return "***";
        }
        return "***" + phone.substring(phone.length() - 4);
    }

    // ===== DTOs and Data Classes =====

    /**
     * Request DTO for user profile creation - matches UserCreateRequestDto from user service
     */
    public static class UserCreateRequest {
        private String referenceId;
        private String email;
        private String firstName;
        private String lastName;
        private String phone;
        private String dob;
        private String gender;
        private List<ConsentRequest> consents;

        // Getters and setters
        public String getReferenceId() { return referenceId; }
        public void setReferenceId(String referenceId) { this.referenceId = referenceId; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }

        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }

        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }

        public String getDob() { return dob; }
        public void setDob(String dob) { this.dob = dob; }

        public String getGender() { return gender; }
        public void setGender(String gender) { this.gender = gender; }

        public List<ConsentRequest> getConsents() { return consents; }
        public void setConsents(List<ConsentRequest> consents) { this.consents = consents; }
    }

    /**
     * Consent request DTO
     */
    public static class ConsentRequest {
        private String consentKey;
        private boolean granted;

        public ConsentRequest() {}

        public ConsentRequest(String consentKey, boolean granted) {
            this.consentKey = consentKey;
            this.granted = granted;
        }

        public String getConsentKey() { return consentKey; }
        public void setConsentKey(String consentKey) { this.consentKey = consentKey; }

        public boolean isGranted() { return granted; }
        public void setGranted(boolean granted) { this.granted = granted; }
    }

    /**
     * User profile response DTO
     */
    public static class UserProfileResponse {
        private String userRef;
        private String email;
        private String firstName;
        private String lastName;
        private String status;
        private String message;

        public String getUserRef() { return userRef; }
        public void setUserRef(String userRef) { this.userRef = userRef; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }

        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    /**
     * User service API response wrapper
     */
    public static class UserServiceApiResponse {
        private boolean success;
        private String message;
        private UserData data;

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public UserData getData() { return data; }
        public void setData(UserData data) { this.data = data; }
    }

    /**
     * User data from user service response
     */
    public static class UserData {
        private String referenceId;
        private String email;
        private String firstName;
        private String lastName;

        public String getReferenceId() { return referenceId; }
        public void setReferenceId(String referenceId) { this.referenceId = referenceId; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }

        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }
    }

    // ===== Exception Classes =====

    /**
     * Exception thrown when user creation fails
     */
    public static class UserCreationException extends RuntimeException {
        public UserCreationException(String message) {
            super(message);
        }

        public UserCreationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
