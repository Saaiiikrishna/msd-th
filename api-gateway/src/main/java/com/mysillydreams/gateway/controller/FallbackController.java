package com.mysillydreams.gateway.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Fallback controller for circuit breaker patterns
 */
@RestController
@RequestMapping("/fallback")
@Slf4j
public class FallbackController {

    /**
     * Fallback for payment service
     */
    @PostMapping("/payment")
    @GetMapping("/payment")
    public ResponseEntity<Map<String, Object>> paymentServiceFallback() {
        log.warn("Payment service is currently unavailable - returning fallback response");
        
        Map<String, Object> fallbackResponse = Map.of(
                "error", "Payment service temporarily unavailable",
                "message", "Please try again in a few moments",
                "status", "SERVICE_UNAVAILABLE",
                "timestamp", LocalDateTime.now(),
                "fallback", true
        );
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(fallbackResponse);
    }

    /**
     * Fallback for auth service
     */
    @PostMapping("/auth")
    @GetMapping("/auth")
    public ResponseEntity<Map<String, Object>> authServiceFallback() {
        log.warn("Auth service is currently unavailable - returning fallback response");
        
        Map<String, Object> fallbackResponse = Map.of(
                "error", "Authentication service temporarily unavailable",
                "message", "Please try again in a few moments",
                "status", "SERVICE_UNAVAILABLE",
                "timestamp", LocalDateTime.now(),
                "fallback", true
        );
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(fallbackResponse);
    }

    /**
     * Fallback for user service
     */
    @PostMapping("/user")
    @GetMapping("/user")
    public ResponseEntity<Map<String, Object>> userServiceFallback() {
        log.warn("User service is currently unavailable - returning fallback response");
        
        Map<String, Object> fallbackResponse = Map.of(
                "error", "User service temporarily unavailable",
                "message", "Please try again in a few moments",
                "status", "SERVICE_UNAVAILABLE",
                "timestamp", LocalDateTime.now(),
                "fallback", true
        );
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
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
