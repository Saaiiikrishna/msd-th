package com.mysillydreams.authservice.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Client for communicating with User Service
 * Creates user profiles after successful authentication registration
 */
@Service
@Slf4j
public class UserServiceClient {

    private final WebClient webClient;
    private final String userServiceUrl;
    private final String createProfileEndpoint;
    private final Duration requestTimeout;
    private final String internalApiKey;

    public UserServiceClient(
            WebClient.Builder webClientBuilder,
            @Value("${auth.user-service.base-url}") String userServiceUrl,
            @Value("${auth.user-service.create-profile-endpoint}") String createProfileEndpoint,
            @Value("${auth.user-service.timeout}") Duration requestTimeout,
            @Value("${auth.user-service.internal-api-key}") String internalApiKey) {

        this.userServiceUrl = userServiceUrl;
        this.createProfileEndpoint = createProfileEndpoint;
        this.requestTimeout = requestTimeout;
        this.internalApiKey = internalApiKey;

        this.webClient = webClientBuilder
                .baseUrl(userServiceUrl)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("User-Agent", "AuthService-UserClient/1.0")
                .build();

        log.info("🔧 UserServiceClient initialized successfully");
        log.info("🔧 User Service URL: {}", userServiceUrl);
        log.info("🔧 Create Profile Endpoint: {}", createProfileEndpoint);
        log.info("🔧 Request Timeout: {}", requestTimeout);
        log.info("🔧 Internal API Key configured: {}", internalApiKey != null && !internalApiKey.isEmpty() ? "YES" : "NO");
    }

    /**
     * Create user profile in User Service
     * Called after successful Keycloak user creation
     */
    public Mono<UserProfileResponse> createUserProfile(String userRef, String email, String firstName, String lastName,
                                                      String phone, String gender, Boolean acceptTerms,
                                                      Boolean acceptPrivacy, Boolean marketingConsent) {
        log.info("🚀 Starting user profile creation in User Service");
        log.info("🚀 User Reference ID: {}", userRef);
        log.info("🚀 Email: {}", maskEmail(email));
        log.info("🚀 Name: {} {}", firstName, lastName);
        log.info("🚀 Phone: {}", phone != null ? maskPhone(phone) : "N/A");
        log.info("🚀 Gender: {}", gender);
        log.info("🚀 Consents - Terms: {}, Privacy: {}, Marketing: {}", acceptTerms, acceptPrivacy, marketingConsent);
        log.info("🚀 Target Endpoint: {}{}", userServiceUrl, createProfileEndpoint);

        UserCreateRequest request = createUserCreateRequest(userRef, email, firstName, lastName,
                                                           phone, gender, acceptTerms, acceptPrivacy, marketingConsent);

        return webClient
                .post()
                .uri(createProfileEndpoint)
                .header("X-Internal-API-Key", internalApiKey)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(UserProfileResponse.class)
                .timeout(requestTimeout)
                .doOnSubscribe(subscription -> {
                    log.info("📡 Sending HTTP POST request to User Service...");
                })
                .doOnSuccess(response -> {
                    log.info("✅ Successfully created user profile for user_ref: {}", userRef);
                    log.info("✅ User Service Response: {}", response.getMessage());
                })
                .onErrorResume(WebClientResponseException.class, e -> {
                    log.error("❌ === USER SERVICE HTTP ERROR ===");
                    log.error("❌ Status Code: {}", e.getStatusCode());
                    log.error("❌ Status Text: {}", e.getStatusText());
                    log.error("❌ Request URL: {}{}", userServiceUrl, createProfileEndpoint);
                    log.error("❌ User Reference: {}", userRef);
                    log.error("❌ Email: {}", maskEmail(email));
                    log.error("❌ Response Body: {}", e.getResponseBodyAsString());
                    log.error("❌ Request Headers: X-Internal-API-Key: [MASKED]");

                    if (e.getStatusCode() == HttpStatus.CONFLICT) {
                        // User already exists, this is okay
                        log.warn("⚠️ === USER ALREADY EXISTS ===");
                        log.warn("⚠️ User profile already exists for user_ref: {} - treating as success", userRef);
                        log.warn("⚠️ This may indicate a retry scenario or race condition");
                        return Mono.just(new UserProfileResponse("User profile already exists", userRef));
                    } else if (e.getStatusCode() == HttpStatus.BAD_REQUEST) {
                        log.error("❌ Bad Request - Invalid user data provided");
                        return Mono.error(new UserServiceException("Invalid user data: " + e.getResponseBodyAsString()));
                    } else if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                        log.error("❌ Unauthorized - Invalid internal API key");
                        return Mono.error(new UserServiceException("Authentication failed with User Service"));
                    } else if (e.getStatusCode().is5xxServerError()) {
                        log.error("❌ User Service internal error - may be temporary");
                        return Mono.error(new UserServiceException("User Service internal error: " + e.getMessage()));
                    } else {
                        log.error("❌ Unexpected HTTP status: {}", e.getStatusCode());
                        return Mono.error(new UserServiceException("Unexpected User Service error: " + e.getMessage()));
                    }
                })
                .onErrorResume(Exception.class, e -> {
                    log.error("❌ === USER SERVICE COMMUNICATION ERROR ===");
                    log.error("❌ User Reference: {}", userRef);
                    log.error("❌ Email: {}", maskEmail(email));
                    log.error("❌ Error Type: {}", e.getClass().getSimpleName());
                    log.error("❌ Error Message: {}", e.getMessage());
                    log.error("❌ This may indicate network issues or User Service unavailability", e);
                    return Mono.error(new UserServiceException("User Service communication failed: " + e.getMessage(), e));
                });
    }

    /**
     * Check if user profile exists in User Service
     */
    public Mono<Boolean> userProfileExists(String userRef) {
        log.debug("Checking if user profile exists for user_ref: {}", userRef);
        
        return webClient
                .get()
                .uri("/api/v1/users/{userRef}", userRef)
                .header("X-Internal-API-Key", internalApiKey)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(requestTimeout)
                .map(response -> true)
                .onErrorResume(WebClientResponseException.class, e -> {
                    if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                        return Mono.just(false);
                    } else {
                        log.warn("Error checking user profile existence: {}", e.getMessage());
                        return Mono.just(false);
                    }
                })
                .onErrorReturn(false);
    }

    /**
     * Create UserCreateRequest from registration data
     */
    private UserCreateRequest createUserCreateRequest(String userRef, String email, String firstName, String lastName,
                                                     String phone, String gender, Boolean acceptTerms,
                                                     Boolean acceptPrivacy, Boolean marketingConsent) {
        UserCreateRequest request = new UserCreateRequest();
        request.setReferenceId(userRef);
        request.setEmail(email);
        request.setFirstName(firstName);
        request.setLastName(lastName);
        request.setPhone(phone);
        request.setGender(gender);

        // Create consent list
        List<ConsentRequest> consents = new ArrayList<>();

        // Add terms of service consent
        if (acceptTerms != null) {
            consents.add(new ConsentRequest("terms_of_service", acceptTerms));
        }

        // Add privacy policy consent
        if (acceptPrivacy != null) {
            consents.add(new ConsentRequest("privacy_policy", acceptPrivacy));
        }

        // Add marketing consent
        if (marketingConsent != null) {
            consents.add(new ConsentRequest("marketing_emails", marketingConsent));
        }

        request.setConsents(consents);
        return request;
    }

    /**
     * Request DTO for user profile creation - matches UserCreateRequestDto from user service
     */
    @Data
    public static class UserCreateRequest {
        private String referenceId;
        private String email;
        private String firstName;
        private String lastName;
        private String phone;
        private String gender;
        private List<ConsentRequest> consents;
    }

    /**
     * Consent request DTO - matches ConsentRequestDto from user service
     */
    @Data
    public static class ConsentRequest {
        private String consentKey;
        private Boolean granted;

        public ConsentRequest(String consentKey, Boolean granted) {
            this.consentKey = consentKey;
            this.granted = granted;
        }
    }

    /**
     * Response DTO for user profile creation
     */
    @Data
    public static class UserProfileResponse {
        private String message;
        private String referenceId;
        
        public UserProfileResponse() {}
        
        public UserProfileResponse(String message, String referenceId) {
            this.message = message;
            this.referenceId = referenceId;
        }
    }

    /**
     * Mask email for logging privacy
     */
    private String maskEmail(String email) {
        if (email == null || email.isEmpty()) {
            return "null";
        }
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) {
            return email.charAt(0) + "***@***";
        }
        return email.substring(0, 2) + "***" + email.substring(atIndex);
    }

    /**
     * Mask phone for logging privacy
     */
    private String maskPhone(String phone) {
        if (phone == null || phone.isEmpty()) {
            return "null";
        }
        if (phone.length() <= 4) {
            return "***";
        }
        return phone.substring(0, 2) + "***" + phone.substring(phone.length() - 2);
    }

    /**
     * Exception for User Service communication errors
     */
    public static class UserServiceException extends RuntimeException {
        public UserServiceException(String message) {
            super(message);
        }

        public UserServiceException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
