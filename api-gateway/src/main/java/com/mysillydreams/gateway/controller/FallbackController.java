package com.mysillydreams.gateway.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Production-ready fallback controller for circuit breaker patterns
 * Provides graceful degradation when services are unavailable
 */
@RestController
@RequestMapping("/fallback")
@Slf4j
public class FallbackController {

    /**
     * Fallback for payment service
     */
    @RequestMapping(value = "/payment", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<Map<String, Object>> paymentServiceFallback(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();
        log.warn("Payment service is currently unavailable - returning fallback response for {} {}",
                request.getMethod(), request.getPath());

        Map<String, Object> fallbackResponse = Map.of(
                "error", "Payment service temporarily unavailable",
                "message", "Payment processing is temporarily down. Please try again in a few moments.",
                "status", "SERVICE_UNAVAILABLE",
                "timestamp", LocalDateTime.now(),
                "fallback", true,
                "retryAfter", 30,
                "correlationId", request.getHeaders().getFirst("X-Correlation-Id") != null ?
                        request.getHeaders().getFirst("X-Correlation-Id") : "unknown"
        );

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .header("Retry-After", "30")
                .body(fallbackResponse);
    }

    /**
     * Fallback for auth service
     */
    @RequestMapping(value = "/auth", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<Map<String, Object>> authServiceFallback(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();
        log.warn("Auth service is currently unavailable - returning fallback response for {} {}",
                request.getMethod(), request.getPath());

        Map<String, Object> fallbackResponse = Map.of(
                "error", "Authentication service temporarily unavailable",
                "message", "Authentication services are temporarily down. Please try again in a few moments.",
                "status", "SERVICE_UNAVAILABLE",
                "timestamp", LocalDateTime.now(),
                "fallback", true,
                "retryAfter", 30,
                "correlationId", request.getHeaders().getFirst("X-Correlation-Id") != null ?
                        request.getHeaders().getFirst("X-Correlation-Id") : "unknown"
        );

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .header("Retry-After", "30")
                .body(fallbackResponse);
    }

    /**
     * Fallback for user service
     */
    @RequestMapping(value = "/user", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<Map<String, Object>> userServiceFallback(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();
        log.warn("User service is currently unavailable - returning fallback response for {} {}",
                request.getMethod(), request.getPath());

        Map<String, Object> fallbackResponse = Map.of(
                "error", "User service temporarily unavailable",
                "message", "User management services are temporarily down. Please try again in a few moments.",
                "status", "SERVICE_UNAVAILABLE",
                "timestamp", LocalDateTime.now(),
                "fallback", true,
                "retryAfter", 30,
                "correlationId", request.getHeaders().getFirst("X-Correlation-Id") != null ?
                        request.getHeaders().getFirst("X-Correlation-Id") : "unknown"
        );

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .header("Retry-After", "30")
                .body(fallbackResponse);
    }

    /**
     * Fallback for treasure service
     */
    @RequestMapping(value = "/treasure", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<Map<String, Object>> treasureServiceFallback(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();
        log.warn("Treasure service is currently unavailable - returning fallback response for {} {}",
                request.getMethod(), request.getPath());

        Map<String, Object> fallbackResponse = Map.of(
                "error", "Treasure service temporarily unavailable",
                "message", "Trip information and enrollment services are temporarily down. Please try again in a few moments.",
                "status", "SERVICE_UNAVAILABLE",
                "timestamp", LocalDateTime.now(),
                "fallback", true,
                "retryAfter", 60,
                "correlationId", request.getHeaders().getFirst("X-Correlation-Id") != null ?
                        request.getHeaders().getFirst("X-Correlation-Id") : "unknown"
        );

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .header("Retry-After", "60")
                .body(fallbackResponse);
    }

    /**
     * Generic fallback for any service
     */
    @PostMapping("/generic")
    @GetMapping("/generic")
    public ResponseEntity<Map<String, Object>> genericServiceFallback() {
        log.warn("Service is currently unavailable - returning generic fallback response");
        
        Map<String, Object> fallbackResponse = Map.of(
                "error", "Service temporarily unavailable",
                "message", "We're experiencing high traffic. Please try again in a few moments.",
                "status", "SERVICE_UNAVAILABLE",
                "timestamp", LocalDateTime.now(),
                "fallback", true,
                "retryAfter", 30 // seconds
        );
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .header("Retry-After", "30")
                .body(fallbackResponse);
    }
}
