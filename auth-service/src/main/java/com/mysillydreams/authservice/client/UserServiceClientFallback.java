package com.mysillydreams.authservice.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Fallback implementation for UserServiceClient
 * Provides graceful degradation when user-service is unavailable
 */
@Component
@Slf4j
public class UserServiceClientFallback implements UserServiceClient {

    @Override
    public UserProfileResponse createUserProfile(String authToken, CreateUserProfileRequest request) {
        log.warn("🚨 User service is unavailable, using fallback for user: {}", request.getEmail());
        
        // Return a response indicating the service is unavailable
        // The user account will still be created in Keycloak, but profile creation is deferred
        UserProfileResponse response = new UserProfileResponse();
        response.setUserRef(request.getUserRef());
        response.setEmail(request.getEmail());
        response.setFirstName(request.getFirstName());
        response.setLastName(request.getLastName());
        response.setStatus("DEFERRED");
        response.setMessage("User profile creation deferred - user service unavailable");
        
        return response;
    }
}
