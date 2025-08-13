package com.mysillydreams.treasure.api.rest.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.Map;

@RestController
public class RootController {

    @GetMapping("/")
    public Map<String, Object> root() {
        return Map.of(
            "service", "Treasure Hunt API",
            "version", "1.0.0",
            "description", "Treasure Hunt gaming platform API for managing plans, enrollments, and user progress",
            "timestamp", OffsetDateTime.now(),
            "endpoints", Map.of(
                "catalog", "/api/treasure/v1/catalog",
                "plans", "/api/treasure/v1/plans", 
                "admin", "/api/treasure/v1/admin",
                "health", "/actuator/health"
            ),
            "documentation", "API documentation available at /swagger-ui.html (when enabled)"
        );
    }
}
