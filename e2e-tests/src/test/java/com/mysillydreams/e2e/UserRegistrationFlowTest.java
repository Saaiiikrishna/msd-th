package com.mysillydreams.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.Map;

/**
 * End-to-end test for complete user registration flow
 * Tests the integration between Auth Service and User Service
 */
@SpringBootTest
@ActiveProfiles("e2e")
class UserRegistrationFlowTest {

    private final WebClient webClient = WebClient.builder()
            .baseUrl("http://localhost:8080") // API Gateway URL
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void completeUserRegistrationFlow_ShouldCreateUserInBothServices() {
        String testEmail = "e2e-test-" + System.currentTimeMillis() + "@example.com";
        
        // Step 1: Register user through Auth Service
        Map<String, Object> registrationRequest = Map.of(
            "email", testEmail,
            "password", "SecurePassword123",
            "firstName", "E2E",
            "lastName", "Test"
        );

        Mono<String> registrationFlow = webClient
            .post()
            .uri("/api/auth/v1/register")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(registrationRequest)
            .retrieve()
            .bodyToMono(String.class)
            .map(response -> {
                try {
                    JsonNode node = objectMapper.readTree(response);
                    return node.get("userRef").asText();
                } catch (Exception e) {
                    throw new RuntimeException("Failed to parse registration response", e);
                }
            })
            // Step 2: Verify user exists in User Service
            .flatMap(userRef -> 
                webClient
                    .get()
                    .uri("/api/user-service/v1/users/{userRef}", userRef)
                    .retrieve()
                    .bodyToMono(String.class)
                    .map(userResponse -> {
                        try {
                            JsonNode userNode = objectMapper.readTree(userResponse);
                            String retrievedEmail = userNode.get("email").asText();
                            if (!testEmail.equals(retrievedEmail)) {
                                throw new RuntimeException("Email mismatch: expected " + testEmail + ", got " + retrievedEmail);
                            }
                            return userRef;
                        } catch (Exception e) {
                            throw new RuntimeException("Failed to parse user response", e);
                        }
                    })
            )
            // Step 3: Verify user can be found by email search
            .flatMap(userRef ->
                webClient
                    .get()
                    .uri(uriBuilder -> uriBuilder
                        .path("/api/user-service/v1/users/search")
                        .queryParam("email", testEmail)
                        .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .map(searchResponse -> {
                        try {
                            JsonNode searchNode = objectMapper.readTree(searchResponse);
                            if (!searchNode.isArray() || searchNode.size() == 0) {
                                throw new RuntimeException("User not found in search results");
                            }
                            return userRef;
                        } catch (Exception e) {
                            throw new RuntimeException("Failed to parse search response", e);
                        }
                    })
            );

        // Execute and verify the complete flow
        StepVerifier.create(registrationFlow)
            .expectNextMatches(userRef -> userRef != null && userRef.matches(
                "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
            ))
            .verifyComplete();
    }

    @Test
    void userRegistrationWithDuplicateEmail_ShouldFail() {
        String duplicateEmail = "duplicate-test@example.com";
        
        Map<String, Object> registrationRequest = Map.of(
            "email", duplicateEmail,
            "password", "SecurePassword123",
            "firstName", "Duplicate",
            "lastName", "Test"
        );

        // First registration should succeed
        Mono<Void> firstRegistration = webClient
            .post()
            .uri("/api/auth/v1/register")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(registrationRequest)
            .retrieve()
            .bodyToMono(Void.class);

        // Second registration with same email should fail
        Mono<Void> secondRegistration = webClient
            .post()
            .uri("/api/auth/v1/register")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(registrationRequest)
            .retrieve()
            .onStatus(HttpStatus::is4xxClientError, response -> Mono.empty())
            .bodyToMono(Void.class);

        // Execute first registration
        StepVerifier.create(firstRegistration)
            .verifyComplete();

        // Execute second registration and expect failure
        StepVerifier.create(secondRegistration)
            .expectError()
            .verify();
    }

    @Test
    void authenticationFlow_ShouldWorkWithRegisteredUser() {
        String testEmail = "auth-test-" + System.currentTimeMillis() + "@example.com";
        
        // Step 1: Register user
        Map<String, Object> registrationRequest = Map.of(
            "email", testEmail,
            "password", "SecurePassword123",
            "firstName", "Auth",
            "lastName", "Test"
        );

        Mono<String> authenticationFlow = webClient
            .post()
            .uri("/api/auth/v1/register")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(registrationRequest)
            .retrieve()
            .bodyToMono(String.class)
            .map(response -> {
                try {
                    JsonNode node = objectMapper.readTree(response);
                    return node.get("userRef").asText();
                } catch (Exception e) {
                    throw new RuntimeException("Failed to parse registration response", e);
                }
            })
            // Step 2: Get login info for OAuth2 flow
            .flatMap(userRef ->
                webClient
                    .get()
                    .uri("/api/auth/v1/login-info")
                    .retrieve()
                    .bodyToMono(String.class)
                    .map(loginInfo -> {
                        try {
                            JsonNode loginNode = objectMapper.readTree(loginInfo);
                            String authUrl = loginNode.get("authUrl").asText();
                            String clientId = loginNode.get("clientId").asText();
                            
                            if (authUrl == null || authUrl.isEmpty()) {
                                throw new RuntimeException("Auth URL not provided");
                            }
                            if (clientId == null || clientId.isEmpty()) {
                                throw new RuntimeException("Client ID not provided");
                            }
                            
                            return userRef;
                        } catch (Exception e) {
                            throw new RuntimeException("Failed to parse login info", e);
                        }
                    })
            );

        StepVerifier.create(authenticationFlow)
            .expectNextMatches(userRef -> userRef != null && !userRef.isEmpty())
            .verifyComplete();
    }

    @Test
    void sessionIntrospection_ShouldValidateTokens() {
        // This test would require a valid JWT token from Keycloak
        // For now, we test the endpoint availability
        
        Map<String, String> introspectionRequest = Map.of(
            "sessionToken", "invalid-token-for-testing"
        );

        Mono<String> introspectionFlow = webClient
            .post()
            .uri("/api/auth/v1/sessions/introspect")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(introspectionRequest)
            .retrieve()
            .bodyToMono(String.class)
            .map(response -> {
                try {
                    JsonNode node = objectMapper.readTree(response);
                    boolean active = node.get("active").asBoolean();
                    
                    // Invalid token should return active: false
                    if (active) {
                        throw new RuntimeException("Invalid token should not be active");
                    }
                    
                    return "success";
                } catch (Exception e) {
                    throw new RuntimeException("Failed to parse introspection response", e);
                }
            });

        StepVerifier.create(introspectionFlow)
            .expectNext("success")
            .verifyComplete();
    }

    @Test
    void healthChecks_ShouldReturnHealthyStatus() {
        // Test all service health endpoints
        Mono<String> healthCheckFlow = Mono.zip(
            // Auth Service health
            webClient.get().uri("/api/auth/v1/health").retrieve().bodyToMono(String.class),
            // User Service health  
            webClient.get().uri("/api/user-service/v1/users/health").retrieve().bodyToMono(String.class),
            // Treasure Service health
            webClient.get().uri("/api/treasure/v1/health").retrieve().bodyToMono(String.class)
        ).map(tuple -> {
            try {
                // Verify all health checks return healthy status
                JsonNode authHealth = objectMapper.readTree(tuple.getT1());
                JsonNode userHealth = objectMapper.readTree(tuple.getT2());
                JsonNode treasureHealth = objectMapper.readTree(tuple.getT3());
                
                if (!"UP".equals(authHealth.get("status").asText()) ||
                    !"UP".equals(userHealth.get("status").asText()) ||
                    !"UP".equals(treasureHealth.get("status").asText())) {
                    throw new RuntimeException("One or more services are not healthy");
                }
                
                return "all-healthy";
            } catch (Exception e) {
                throw new RuntimeException("Failed to parse health responses", e);
            }
        });

        StepVerifier.create(healthCheckFlow)
            .expectNext("all-healthy")
            .verifyComplete();
    }
}
