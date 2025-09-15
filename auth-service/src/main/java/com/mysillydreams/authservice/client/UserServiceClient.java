package com.mysillydreams.authservice.client;

import lombok.Data;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * Feign client for communicating with user-service
 * Handles user profile creation and management
 */
@FeignClient(
    name = "user-service",
    url = "${services.user-service.url:http://localhost:8082}",
    fallback = UserServiceClientFallback.class
)
public interface UserServiceClient {

    /**
     * Create user profile in user-service
     */
    @PostMapping("/api/v1/users")
    UserProfileResponse createUserProfile(
        @RequestHeader("Authorization") String authToken,
        @RequestBody CreateUserProfileRequest request
    );

    @Data
    class CreateUserProfileRequest {
        private String userRef;
        private String email;
        private String firstName;
        private String lastName;
        private String phone;
        private String gender;
        private boolean acceptTerms;
        private boolean acceptPrivacy;
        private boolean marketingConsent;
    }

    @Data
    class UserProfileResponse {
        private String userRef;
        private String email;
        private String firstName;
        private String lastName;
        private String status;
        private String message;
    }
}
