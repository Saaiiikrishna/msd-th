package com.mysillydreams.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Custom authentication filter for JWT token validation
 */
@Component
@Slf4j
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    // Paths that don't require authentication
    private static final List<String> OPEN_API_ENDPOINTS = List.of(
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/forgot-password",
            "/api/auth/reset-password",
            "/api/payments/v1/treasure/webhook",
            "/api/payments/v1/webhooks",
            "/api/trust",
            "/health",
            "/actuator",
            "/swagger-ui",
            "/api-docs",
            "/v3/api-docs"
    );

    public AuthenticationFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getURI().getPath();

            log.debug("Processing request for path: {}", path);

            // Skip authentication for open endpoints
            if (isOpenEndpoint(path)) {
                log.debug("Skipping authentication for open endpoint: {}", path);
                return chain.filter(exchange);
            }

            // Check for Authorization header
            String authHeader = request.getHeaders().getFirst("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("Missing or invalid Authorization header for path: {}", path);
                return onError(exchange, "Missing Authorization header", HttpStatus.UNAUTHORIZED);
            }

            String token = authHeader.substring(7);

            // Validate JWT token
            return validateToken(token)
                    .flatMap(isValid -> {
                        if (isValid) {
                            log.debug("Token validation successful for path: {}", path);
                            // Add user information to request headers
                            ServerHttpRequest modifiedRequest = request.mutate()
                                    .header("X-User-ID", extractUserIdFromToken(token))
                                    .header("X-User-Role", extractUserRoleFromToken(token))
                                    .build();
                            
                            return chain.filter(exchange.mutate().request(modifiedRequest).build());
                        } else {
                            log.warn("Token validation failed for path: {}", path);
                            return onError(exchange, "Invalid token", HttpStatus.UNAUTHORIZED);
                        }
                    })
                    .onErrorResume(throwable -> {
                        log.error("Error during token validation for path: {}", path, throwable);
                        return onError(exchange, "Authentication error", HttpStatus.UNAUTHORIZED);
                    });
        };
    }

    /**
     * Check if the endpoint is open (doesn't require authentication)
     */
    private boolean isOpenEndpoint(String path) {
        return OPEN_API_ENDPOINTS.stream()
                .anyMatch(endpoint -> path.startsWith(endpoint));
    }

    /**
     * Validate JWT token
     * TODO: Implement actual JWT validation logic
     */
    private Mono<Boolean> validateToken(String token) {
        // For now, return true for development
        // In production, this should validate the JWT token
        return Mono.just(true);
    }

    /**
     * Extract user ID from JWT token
     * TODO: Implement actual JWT parsing logic
     */
    private String extractUserIdFromToken(String token) {
        // Placeholder implementation
        return "user-123";
    }

    /**
     * Extract user role from JWT token
     * TODO: Implement actual JWT parsing logic
     */
    private String extractUserRoleFromToken(String token) {
        // Placeholder implementation
        return "USER";
    }

    /**
     * Handle authentication errors
     */
    private Mono<Void> onError(ServerWebExchange exchange, String err, HttpStatus httpStatus) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(httpStatus);
        response.getHeaders().add("Content-Type", "application/json");
        
        String errorResponse = String.format(
                "{\"error\": \"%s\", \"status\": %d, \"timestamp\": \"%s\"}", 
                err, 
                httpStatus.value(), 
                java.time.Instant.now().toString()
        );
        
        org.springframework.core.io.buffer.DataBuffer buffer = response.bufferFactory()
                .wrap(errorResponse.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        
        return response.writeWith(Mono.just(buffer));
    }

    /**
     * Configuration class for the filter
     */
    public static class Config {
        // Configuration properties can be added here
        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
