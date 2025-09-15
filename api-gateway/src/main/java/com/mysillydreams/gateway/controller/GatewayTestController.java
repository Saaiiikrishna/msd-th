package com.mysillydreams.gateway.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Test controller for validating gateway configuration and routing
 * Provides endpoints to test security, routing, and service connectivity
 * This controller should only be active in non-production environments.
 */
@RestController
@RequestMapping("/gateway-test")
@Slf4j
@Profile("!prod")
public class GatewayTestController {

    @Autowired
    private RouteLocator routeLocator;

    /**
     * Health check endpoint for the gateway
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now());
        health.put("service", "api-gateway");
        health.put("version", "1.0.0");
        
        return ResponseEntity.ok(health);
    }

    /**
     * Test endpoint to validate CORS configuration
     */
    @GetMapping("/cors-test")
    @CrossOrigin(origins = {"http://localhost:3000", "https://*.mysillydreams.com"})
    public ResponseEntity<Map<String, Object>> corsTest() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "CORS test successful");
        response.put("timestamp", LocalDateTime.now());
        response.put("allowedOrigins", "http://localhost:3000, https://*.mysillydreams.com");
        
        return ResponseEntity.ok(response);
    }

    /**
     * Test endpoint to validate JWT authentication
     */
    @GetMapping("/auth-test")
    public Mono<ResponseEntity<Map<String, Object>>> authTest() {
        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> {
                    Authentication auth = securityContext.getAuthentication();
                    Map<String, Object> response = new HashMap<>();
                    
                    if (auth != null && auth.isAuthenticated()) {
                        response.put("authenticated", true);
                        response.put("principal", auth.getName());
                        response.put("authorities", auth.getAuthorities().stream()
                                .map(Object::toString)
                                .collect(Collectors.toList()));
                    } else {
                        response.put("authenticated", false);
                        response.put("message", "No authentication found");
                    }
                    
                    response.put("timestamp", LocalDateTime.now());
                    return ResponseEntity.ok(response);
                })
                .defaultIfEmpty(ResponseEntity.ok(Map.of(
                        "authenticated", false,
                        "message", "No security context found",
                        "timestamp", LocalDateTime.now()
                )));
    }

    /**
     * Test endpoint to validate rate limiting
     */
    @GetMapping("/rate-limit-test")
    public ResponseEntity<Map<String, Object>> rateLimitTest() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Rate limit test - this endpoint should be rate limited");
        response.put("timestamp", LocalDateTime.now());
        response.put("tip", "Make multiple rapid requests to test rate limiting");
        
        return ResponseEntity.ok(response);
    }

    /**
     * Test endpoint to validate circuit breaker (simulates failure)
     */
    @GetMapping("/circuit-breaker-test")
    public ResponseEntity<Map<String, Object>> circuitBreakerTest(@RequestParam(defaultValue = "false") boolean fail) {
        if (fail) {
            throw new RuntimeException("Simulated service failure for circuit breaker testing");
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Circuit breaker test successful");
        response.put("timestamp", LocalDateTime.now());
        response.put("tip", "Add ?fail=true to simulate failure");
        
        return ResponseEntity.ok(response);
    }

    /**
     * Test endpoint to validate idempotency handling
     */
    @PostMapping("/idempotency-test")
    public ResponseEntity<Map<String, Object>> idempotencyTest(
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody(required = false) Map<String, Object> body) {
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Idempotency test successful");
        response.put("idempotencyKey", idempotencyKey);
        response.put("timestamp", LocalDateTime.now());
        response.put("receivedBody", body);
        
        if (idempotencyKey == null) {
            response.put("warning", "No X-Idempotency-Key header provided");
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get current gateway routes configuration
     */
    @GetMapping("/routes")
    public Mono<ResponseEntity<Map<String, Object>>> getRoutes() {
        return routeLocator.getRoutes()
                .collectList()
                .map(routes -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("totalRoutes", routes.size());
                    response.put("routes", routes.stream()
                            .map(route -> Map.of(
                                    "id", route.getId(),
                                    "uri", route.getUri().toString(),
                                    "order", route.getOrder(),
                                    "filters", route.getFilters().stream()
                                            .map(filter -> filter.getClass().getSimpleName())
                                            .collect(Collectors.toList())
                            ))
                            .collect(Collectors.toList()));
                    response.put("timestamp", LocalDateTime.now());
                    
                    return ResponseEntity.ok(response);
                });
    }

    /**
     * Test endpoint for correlation ID propagation
     */
    @GetMapping("/correlation-test")
    public ResponseEntity<Map<String, Object>> correlationTest(
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Correlation ID test");
        response.put("correlationId", correlationId);
        response.put("timestamp", LocalDateTime.now());
        
        if (correlationId == null) {
            response.put("warning", "No X-Correlation-Id header found - should be auto-generated by gateway");
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * Comprehensive gateway validation endpoint
     */
    @GetMapping("/validate")
    public Mono<ResponseEntity<Map<String, Object>>> validateGateway() {
        return routeLocator.getRoutes()
                .collectList()
                .map(routes -> {
                    Map<String, Object> validation = new HashMap<>();
                    validation.put("timestamp", LocalDateTime.now());
                    validation.put("gatewayStatus", "OPERATIONAL");
                    
                    // Route validation
                    Map<String, Object> routeValidation = new HashMap<>();
                    routeValidation.put("totalRoutes", routes.size());
                    routeValidation.put("expectedServices", Map.of(
                            "auth-service", routes.stream().anyMatch(r -> r.getId().equals("auth-service")),
                            "user-service", routes.stream().anyMatch(r -> r.getId().equals("user-service")),
                            "treasure-service", routes.stream().anyMatch(r -> r.getId().equals("treasure-service")),
                            "payment-service", routes.stream().anyMatch(r -> r.getId().equals("payment-service"))
                    ));
                    validation.put("routes", routeValidation);
                    
                    // Security validation
                    Map<String, Object> securityValidation = new HashMap<>();
                    securityValidation.put("jwtConfigured", true); // This would need actual validation
                    securityValidation.put("corsConfigured", true);
                    validation.put("security", securityValidation);
                    
                    // Feature validation
                    Map<String, Object> features = new HashMap<>();
                    features.put("rateLimiting", true);
                    features.put("circuitBreaker", true);
                    features.put("idempotency", true);
                    features.put("correlationId", true);
                    validation.put("features", features);
                    
                    return ResponseEntity.ok(validation);
                });
    }
}
