package com.mysillydreams.gateway.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Mock controller for services that haven't been implemented yet
 * This allows frontend development to proceed while backend services are being built
 */
@RestController
@RequestMapping("/mock")
@Slf4j
public class MockServiceController {

    /**
     * Mock Auth Service Endpoints
     */
    @PostMapping("/auth/login")
    public ResponseEntity<Map<String, Object>> mockLogin(@RequestBody Map<String, Object> loginRequest) {
        log.info("Mock login request: {}", loginRequest);
        
        Map<String, Object> response = Map.of(
                "success", true,
                "user", Map.of(
                        "id", UUID.randomUUID().toString(),
                        "email", loginRequest.get("email"),
                        "name", "Mock User",
                        "role", "USER"
                ),
                "accessToken", "mock-jwt-token-" + System.currentTimeMillis(),
                "refreshToken", "mock-refresh-token-" + System.currentTimeMillis(),
                "expiresIn", 3600
        );
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/auth/register")
    public ResponseEntity<Map<String, Object>> mockRegister(@RequestBody Map<String, Object> registerRequest) {
        log.info("Mock register request: {}", registerRequest);
        
        Map<String, Object> response = Map.of(
                "success", true,
                "message", "User registered successfully",
                "user", Map.of(
                        "id", UUID.randomUUID().toString(),
                        "email", registerRequest.get("email"),
                        "name", registerRequest.get("name"),
                        "role", "USER"
                ),
                "accessToken", "mock-jwt-token-" + System.currentTimeMillis(),
                "refreshToken", "mock-refresh-token-" + System.currentTimeMillis()
        );
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/auth/refresh")
    public ResponseEntity<Map<String, Object>> mockRefreshToken(@RequestBody Map<String, Object> refreshRequest) {
        log.info("Mock refresh token request");
        
        Map<String, Object> response = Map.of(
                "success", true,
                "accessToken", "mock-jwt-token-refreshed-" + System.currentTimeMillis(),
                "expiresIn", 3600
        );
        
        return ResponseEntity.ok(response);
    }

    /**
     * Mock User Service Endpoints
     */
    @GetMapping("/users/profile/{userId}")
    public ResponseEntity<Map<String, Object>> mockGetUserProfile(@PathVariable String userId) {
        log.info("Mock get user profile: {}", userId);
        
        Map<String, Object> response = Map.of(
                "id", userId,
                "email", "user@example.com",
                "name", "Mock User",
                "phone", "+91-9876543210",
                "avatar", "https://via.placeholder.com/150",
                "preferences", Map.of(
                        "notifications", true,
                        "theme", "light",
                        "language", "en"
                ),
                "createdAt", LocalDateTime.now().minusDays(30),
                "updatedAt", LocalDateTime.now()
        );
        
        return ResponseEntity.ok(response);
    }

    @PutMapping("/users/profile/{userId}")
    public ResponseEntity<Map<String, Object>> mockUpdateUserProfile(
            @PathVariable String userId, 
            @RequestBody Map<String, Object> updateRequest) {
        log.info("Mock update user profile: {} with data: {}", userId, updateRequest);
        
        Map<String, Object> response = Map.of(
                "success", true,
                "message", "Profile updated successfully",
                "user", Map.of(
                        "id", userId,
                        "email", updateRequest.getOrDefault("email", "user@example.com"),
                        "name", updateRequest.getOrDefault("name", "Mock User"),
                        "phone", updateRequest.getOrDefault("phone", "+91-9876543210"),
                        "updatedAt", LocalDateTime.now()
                )
        );
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/users/{userId}/bookings")
    public ResponseEntity<Map<String, Object>> mockGetUserBookings(@PathVariable String userId) {
        log.info("Mock get user bookings: {}", userId);
        
        Map<String, Object> response = Map.of(
                "bookings", java.util.List.of(
                        Map.of(
                                "id", UUID.randomUUID().toString(),
                                "huntName", "Mumbai Heritage Hunt",
                                "date", LocalDateTime.now().plusDays(7),
                                "participants", 4,
                                "status", "CONFIRMED",
                                "amount", 2000.00
                        ),
                        Map.of(
                                "id", UUID.randomUUID().toString(),
                                "huntName", "Delhi Food Trail",
                                "date", LocalDateTime.now().minusDays(15),
                                "participants", 2,
                                "status", "COMPLETED",
                                "amount", 1500.00
                        )
                ),
                "totalBookings", 2,
                "totalSpent", 3500.00
        );
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/users/{userId}/payments")
    public ResponseEntity<Map<String, Object>> mockGetUserPayments(@PathVariable String userId) {
        log.info("Mock get user payments: {}", userId);
        
        Map<String, Object> response = Map.of(
                "payments", java.util.List.of(
                        Map.of(
                                "id", UUID.randomUUID().toString(),
                                "amount", 2000.00,
                                "currency", "INR",
                                "status", "SUCCESS",
                                "method", "card",
                                "date", LocalDateTime.now().minusDays(1),
                                "description", "Mumbai Heritage Hunt booking"
                        ),
                        Map.of(
                                "id", UUID.randomUUID().toString(),
                                "amount", 1500.00,
                                "currency", "INR",
                                "status", "SUCCESS",
                                "method", "upi",
                                "date", LocalDateTime.now().minusDays(15),
                                "description", "Delhi Food Trail booking"
                        )
                ),
                "totalPayments", 2,
                "totalAmount", 3500.00
        );
        
        return ResponseEntity.ok(response);
    }

    /**
     * Mock Hunt Service Endpoints (for future use)
     */
    @GetMapping("/hunts")
    public ResponseEntity<Map<String, Object>> mockGetHunts() {
        log.info("Mock get hunts");
        
        Map<String, Object> response = Map.of(
                "hunts", java.util.List.of(
                        Map.of(
                                "id", UUID.randomUUID().toString(),
                                "title", "Mumbai Heritage Hunt",
                                "description", "Explore the rich heritage of Mumbai",
                                "price", 500.00,
                                "duration", "3 hours",
                                "difficulty", "Easy",
                                "location", "Mumbai",
                                "image", "https://via.placeholder.com/400x300"
                        ),
                        Map.of(
                                "id", UUID.randomUUID().toString(),
                                "title", "Delhi Food Trail",
                                "description", "Discover the culinary delights of Old Delhi",
                                "price", 750.00,
                                "duration", "4 hours",
                                "difficulty", "Medium",
                                "location", "Delhi",
                                "image", "https://via.placeholder.com/400x300"
                        )
                ),
                "totalHunts", 2
        );
        
        return ResponseEntity.ok(response);
    }
}
