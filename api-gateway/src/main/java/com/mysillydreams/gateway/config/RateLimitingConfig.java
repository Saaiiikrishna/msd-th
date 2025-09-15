package com.mysillydreams.gateway.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

/**
 * Rate limiting configuration for API Gateway
 * Implements different rate limits based on endpoint types as per architecture specification
 */
@Configuration
@ConfigurationProperties(prefix = "gateway.rate-limiting")
@Data
@Slf4j
public class RateLimitingConfig {

    private boolean enabled = true;
    private int publicCatalogRps = 200;
    private int enrollmentRps = 20;
    private int defaultRps = 100;
    private int burstMultiplier = 2;

    /**
     * Rate limiter for public catalog endpoints (200 RPS as per architecture)
     */
    @Bean("publicCatalogRateLimiter")
    public RedisRateLimiter publicCatalogRateLimiter() {
        if (!enabled) {
            log.warn("Rate limiting is globally DISABLED. Public catalog endpoint will have no rate limit.");
            return new RedisRateLimiter(Integer.MAX_VALUE, Integer.MAX_VALUE, 1);
        }
        int burstCapacity = publicCatalogRps * burstMultiplier;
        log.info("Configuring public catalog rate limiter: {} RPS, {} burst capacity", 
                publicCatalogRps, burstCapacity);
        return new RedisRateLimiter(publicCatalogRps, burstCapacity, 1);
    }

    /**
     * Rate limiter for enrollment endpoints (20 RPS as per architecture)
     */
    @Bean("enrollmentRateLimiter")
    public RedisRateLimiter enrollmentRateLimiter() {
        if (!enabled) {
            log.warn("Rate limiting is globally DISABLED. Enrollment endpoint will have no rate limit.");
            return new RedisRateLimiter(Integer.MAX_VALUE, Integer.MAX_VALUE, 1);
        }
        int burstCapacity = enrollmentRps * burstMultiplier;
        log.info("Configuring enrollment rate limiter: {} RPS, {} burst capacity", 
                enrollmentRps, burstCapacity);
        return new RedisRateLimiter(enrollmentRps, burstCapacity, 1);
    }

    /**
     * General rate limiter for other endpoints (marked as primary)
     */
    @Bean("generalRateLimiter")
    @Primary
    public RedisRateLimiter generalRateLimiter() {
        if (!enabled) {
            log.warn("Rate limiting is globally DISABLED. General endpoints will have no rate limit.");
            return new RedisRateLimiter(Integer.MAX_VALUE, Integer.MAX_VALUE, 1);
        }
        int burstCapacity = defaultRps * burstMultiplier;
        log.info("Configuring general rate limiter: {} RPS, {} burst capacity",
                defaultRps, burstCapacity);
        return new RedisRateLimiter(defaultRps, burstCapacity, 1);
    }

    /**
     * Key resolver that uses the authenticated user principal or falls back to the IP address.
     * This is the primary and recommended resolver for most routes.
     */
    @Bean
    @Primary
    public KeyResolver principalOrIpKeyResolver() {
        return exchange -> {
            // Attempt to resolve by authenticated principal
            return exchange.getPrincipal()
                .map(principal -> {
                    log.info("-> [RateLimit] Resolved by user principal: {}", principal.getName());
                    return "user:" + principal.getName();
                })
                .switchIfEmpty(Mono.defer(() -> {
                    // Fallback to IP address if no principal is found
                    String clientIp = getClientIp(exchange);
                    log.info("-> [RateLimit] No principal found. Resolved by IP address: {}", clientIp);
                    return Mono.just("ip:" + clientIp);
                }));
        };
    }

    /**
     * Key resolver for API key based rate limiting (intended for future use).
     * This can be used for server-to-server communications where no user is present.
     */
    @Bean
    public KeyResolver apiKeyResolver() {
        return exchange -> {
            String apiKey = exchange.getRequest().getHeaders().getFirst("X-API-Key");
            if (apiKey != null && !apiKey.isEmpty()) {
                log.info("-> [RateLimit] Resolved by API Key: {}", apiKey);
                return Mono.just("api:" + apiKey);
            }
            // If no API key, do not resolve. Let another resolver handle it.
            return Mono.empty();
        };
    }

    /**
     * Extracts the client IP address from the request, considering proxy headers like X-Forwarded-For.
     * This is crucial for correctly identifying the client when running behind a reverse proxy or load balancer.
     */
    private String getClientIp(org.springframework.web.server.ServerWebExchange exchange) {
        String xForwardedFor = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // The X-Forwarded-For header can contain a chain of IPs. The first one is the original client.
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = exchange.getRequest().getHeaders().getFirst("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        // Fallback to the direct remote address if no proxy headers are present.
        return exchange.getRequest().getRemoteAddress() != null
                ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                : "unknown";
    }
}
