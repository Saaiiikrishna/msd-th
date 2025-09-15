package com.mysillydreams.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Idempotency filter for handling duplicate requests
 * Ensures that requests with the same idempotency key are processed only once
 */
@Component
@Slf4j
public class IdempotencyFilter extends AbstractGatewayFilterFactory<IdempotencyFilter.Config> {

    private static final String IDEMPOTENCY_KEY_HEADER = "X-Idempotency-Key";
    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    private static final Duration IDEMPOTENCY_TTL = Duration.ofHours(24);
    private static final String REDIS_KEY_PREFIX = "idempotency:";

    private final RedisTemplate<String, String> redisTemplate;

    public IdempotencyFilter(RedisTemplate<String, String> redisTemplate) {
        super(Config.class);
        this.redisTemplate = redisTemplate;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String method = request.getMethod().name();
            String path = request.getPath().value();

            // Only apply idempotency to configured methods and paths
            if (!config.getMethods().contains(method) || !isIdempotentPath(path, config.getPaths())) {
                return chain.filter(exchange);
            }

            String idempotencyKey = request.getHeaders().getFirst(IDEMPOTENCY_KEY_HEADER);
            
            // If no idempotency key provided, generate one for critical operations
            if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
                if (isCriticalOperation(path)) {
                    log.warn("Missing idempotency key for critical operation: {} {}", method, path);
                    return handleMissingIdempotencyKey(exchange);
                }
                // For non-critical operations, continue without idempotency check
                return chain.filter(exchange);
            }

            // Validate idempotency key format (should be UUID)
            if (!isValidUUID(idempotencyKey)) {
                log.warn("Invalid idempotency key format: {}", idempotencyKey);
                return handleInvalidIdempotencyKey(exchange);
            }

            String correlationId = request.getHeaders().getFirst(CORRELATION_ID_HEADER);
            String redisKey = REDIS_KEY_PREFIX + idempotencyKey;

            try {
                // Check if request with this idempotency key was already processed
                String existingResult = redisTemplate.opsForValue().get(redisKey);
                
                if (existingResult != null) {
                    log.info("Duplicate request detected for idempotency key: {} (correlation: {})", 
                            idempotencyKey, correlationId);
                    return handleDuplicateRequest(exchange, existingResult);
                }

                // Store the idempotency key to prevent duplicates
                redisTemplate.opsForValue().set(redisKey, "PROCESSING", IDEMPOTENCY_TTL);

                // Continue with the request
                return chain.filter(exchange).doOnSuccess(aVoid -> {
                    // Update Redis with successful completion
                    redisTemplate.opsForValue().set(redisKey, "COMPLETED", IDEMPOTENCY_TTL);
                    log.debug("Request completed successfully for idempotency key: {}", idempotencyKey);
                }).doOnError(throwable -> {
                    // Remove the key on error to allow retry
                    redisTemplate.delete(redisKey);
                    log.warn("Request failed for idempotency key: {}, allowing retry", idempotencyKey);
                });

            } catch (Exception e) {
                log.error("Error checking idempotency for key: {}", idempotencyKey, e);
                // On Redis error, continue without idempotency check
                return chain.filter(exchange);
            }
        };
    }

    private boolean isIdempotentPath(String path, List<String> configuredPaths) {
        return configuredPaths.stream().anyMatch(pattern -> 
                path.matches(pattern.replace("*", ".*")));
    }

    private boolean isCriticalOperation(String path) {
        return path.contains("/enrollments") || 
               path.contains("/payment-intents") || 
               path.contains("/custom-plans");
    }

    private boolean isValidUUID(String idempotencyKey) {
        try {
            UUID.fromString(idempotencyKey);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private Mono<Void> handleMissingIdempotencyKey(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.BAD_REQUEST);
        response.getHeaders().add("Content-Type", "application/json");
        
        String errorResponse = """
                {
                    "error": "Missing idempotency key",
                    "message": "X-Idempotency-Key header is required for this operation",
                    "status": 400
                }
                """;
        
        return response.writeWith(Mono.just(response.bufferFactory()
                .wrap(errorResponse.getBytes())));
    }

    private Mono<Void> handleInvalidIdempotencyKey(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.BAD_REQUEST);
        response.getHeaders().add("Content-Type", "application/json");
        
        String errorResponse = """
                {
                    "error": "Invalid idempotency key",
                    "message": "X-Idempotency-Key must be a valid UUID",
                    "status": 400
                }
                """;
        
        return response.writeWith(Mono.just(response.bufferFactory()
                .wrap(errorResponse.getBytes())));
    }

    private Mono<Void> handleDuplicateRequest(ServerWebExchange exchange, String existingResult) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.CONFLICT);
        response.getHeaders().add("Content-Type", "application/json");
        
        String errorResponse = String.format("""
                {
                    "error": "Duplicate request",
                    "message": "Request with this idempotency key is already %s",
                    "status": 409
                }
                """, existingResult.toLowerCase());
        
        return response.writeWith(Mono.just(response.bufferFactory()
                .wrap(errorResponse.getBytes())));
    }

    public static class Config {
        private List<String> methods = Arrays.asList("POST", "PUT");
        private List<String> paths = Arrays.asList(
                "/api/treasure/v1/trips/.*/enrollments",
                "/api/payments/v1/payment-intents",
                "/api/treasure/v1/custom-plans"
        );

        public List<String> getMethods() {
            return methods;
        }

        public void setMethods(List<String> methods) {
            this.methods = methods;
        }

        public List<String> getPaths() {
            return paths;
        }

        public void setPaths(List<String> paths) {
            this.paths = paths;
        }
    }
}
