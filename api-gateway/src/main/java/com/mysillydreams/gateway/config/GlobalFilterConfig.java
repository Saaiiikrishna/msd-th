package com.mysillydreams.gateway.config;

import com.mysillydreams.gateway.filter.IdempotencyFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;

import java.util.Arrays;

/**
 * Advanced global filter configuration for API Gateway
 * Provides authentication, logging, correlation ID, security headers, and idempotency functionality
 */
@Configuration
@Slf4j
public class GlobalFilterConfig {

    @Autowired
    private IdempotencyFilter idempotencyFilter;

    /**
     * Global correlation ID filter to ensure all requests have correlation IDs
     */
    @Bean
    @Order(0)
    public GlobalFilter correlationIdFilter() {
        return (exchange, chain) -> {
            String correlationId = exchange.getRequest().getHeaders().getFirst("X-Correlation-Id");
            
            if (correlationId == null || correlationId.isEmpty()) {
                correlationId = java.util.UUID.randomUUID().toString();
                exchange = exchange.mutate()
                        .request(exchange.getRequest().mutate()
                                .header("X-Correlation-Id", correlationId)
                                .build())
                        .build();
            }
            
            // Add correlation ID to response headers
            exchange.getResponse().getHeaders().add("X-Correlation-Id", correlationId);
            
            log.debug("Request correlation ID: {}", correlationId);
            return chain.filter(exchange);
        };
    }

    /**
     * Global request logging filter
     */
    @Bean
    @Order(1)
    public GlobalFilter requestLoggingFilter() {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String correlationId = request.getHeaders().getFirst("X-Correlation-Id");
            
            log.info("Gateway Request - Method: {}, Path: {}, Correlation-ID: {}", 
                    request.getMethod(), 
                    request.getPath().value(), 
                    correlationId);
            
            long startTime = System.currentTimeMillis();
            
            return chain.filter(exchange).doFinally(signalType -> {
                ServerHttpResponse response = exchange.getResponse();
                long duration = System.currentTimeMillis() - startTime;
                
                log.info("Gateway Response - Status: {}, Duration: {}ms, Correlation-ID: {}", 
                        response.getStatusCode(), 
                        duration, 
                        correlationId);
            });
        };
    }

    /**
     * Global idempotency filter for critical operations
     * Applied to POST, PUT, PATCH operations that require idempotency
     */
    @Bean
    @Order(2)
    public GlobalFilter idempotencyGlobalFilter() {
        return (exchange, chain) -> {
            String method = exchange.getRequest().getMethod().name();
            String path = exchange.getRequest().getPath().value();

            // Apply idempotency filter only to critical operations
            if (isCriticalOperation(method, path)) {
                return idempotencyFilter.apply(new IdempotencyFilter.Config()).filter(exchange, chain);
            }

            return chain.filter(exchange);
        };
    }

    /**
     * Global security headers filter
     */
    @Bean
    @Order(3)
    public GlobalFilter securityHeadersFilter() {
        return (exchange, chain) -> {
            return chain.filter(exchange).doFinally(signalType -> {
                ServerHttpResponse response = exchange.getResponse();
                response.getHeaders().add("X-Content-Type-Options", "nosniff");
                response.getHeaders().add("X-Frame-Options", "DENY");
                response.getHeaders().add("X-XSS-Protection", "1; mode=block");
                response.getHeaders().add("Referrer-Policy", "strict-origin-when-cross-origin");
            });
        };
    }

    /**
     * Determine if the operation requires idempotency checking
     */
    private boolean isCriticalOperation(String method, String path) {
        // Apply to POST, PUT, PATCH operations on critical endpoints
        if (!Arrays.asList("POST", "PUT", "PATCH").contains(method)) {
            return false;
        }

        // Critical paths that require idempotency
        return path.contains("/enrollments") ||
               path.contains("/payments") ||
               path.contains("/bookings") ||
               path.contains("/transactions");
    }
}
