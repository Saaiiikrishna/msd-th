package com.mysillydreams.userservice.controller;

import com.mysillydreams.userservice.dto.ApiResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.boot.actuator.health.Health; // Disabled for Azure deployment
// import org.springframework.boot.actuator.health.HealthIndicator; // Disabled for Azure deployment
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for Health Check operations.
 * Provides system health status and readiness checks.
 */
@RestController
@RequestMapping("/api/v1/health")
@Slf4j
@Tag(name = "Health Check", description = "APIs for system health monitoring and readiness checks")
public class HealthController {

    @Autowired(required = false)
    private DataSource dataSource;

    /**
     * Basic health check endpoint
     */
    @GetMapping
    @Operation(
        summary = "Health check",
        description = "Basic health check endpoint to verify service is running"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Service is healthy",
            content = @Content(schema = @Schema(implementation = ApiResponseDto.class)))
    })
    public ResponseEntity<ApiResponseDto<Map<String, Object>>> health() {
        long startTime = System.currentTimeMillis();
        String requestId = getRequestId();
        
        try {
            Map<String, Object> healthInfo = new HashMap<>();
            healthInfo.put("status", "UP");
            healthInfo.put("timestamp", LocalDateTime.now());
            healthInfo.put("service", "user-service");
            healthInfo.put("version", "1.0.0");
            
            ApiResponseDto<Map<String, Object>> response = ApiResponseDto.success("Service is healthy", healthInfo)
                .withRequestId(requestId)
                .withVersion("v1")
                .withProcessingTime(System.currentTimeMillis() - startTime);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Health check failed: {}", e.getMessage());
            
            Map<String, Object> healthInfo = new HashMap<>();
            healthInfo.put("status", "DOWN");
            healthInfo.put("timestamp", LocalDateTime.now());
            healthInfo.put("error", e.getMessage());
            
            return ResponseEntity.status(503)
                .body(ApiResponseDto.<Map<String,Object>>error("SERVICE_UNHEALTHY", "Service health check failed")
                    .withRequestId(requestId));
        }
    }

    /**
     * Detailed health check with dependencies
     */
    @GetMapping("/detailed")
    @Operation(
        summary = "Detailed health check",
        description = "Detailed health check including database connectivity and system resources"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Detailed health information retrieved"),
        @ApiResponse(responseCode = "503", description = "Service or dependencies are unhealthy")
    })
    public ResponseEntity<ApiResponseDto<Map<String, Object>>> detailedHealth() {
        long startTime = System.currentTimeMillis();
        String requestId = getRequestId();
        
        try {
            Map<String, Object> healthInfo = new HashMap<>();
            healthInfo.put("timestamp", LocalDateTime.now());
            healthInfo.put("service", "user-service");
            healthInfo.put("version", "1.0.0");
            
            // Check database connectivity
            Map<String, Object> databaseHealth = checkDatabaseHealth();
            healthInfo.put("database", databaseHealth);
            
            // Check system resources
            Map<String, Object> systemHealth = checkSystemHealth();
            healthInfo.put("system", systemHealth);
            
            // Determine overall status
            boolean isDatabaseHealthy = "UP".equals(databaseHealth.get("status"));
            boolean isSystemHealthy = "UP".equals(systemHealth.get("status"));
            
            String overallStatus = (isDatabaseHealthy && isSystemHealthy) ? "UP" : "DOWN";
            healthInfo.put("status", overallStatus);
            
            ApiResponseDto<Map<String, Object>> response = ApiResponseDto.success("Detailed health check completed", healthInfo)
                .withRequestId(requestId)
                .withVersion("v1")
                .withProcessingTime(System.currentTimeMillis() - startTime);
            
            if ("UP".equals(overallStatus)) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(503).body(response);
            }
            
        } catch (Exception e) {
            log.error("Detailed health check failed: {}", e.getMessage());
            
            Map<String, Object> healthInfo = new HashMap<>();
            healthInfo.put("status", "DOWN");
            healthInfo.put("timestamp", LocalDateTime.now());
            healthInfo.put("error", e.getMessage());
            
            return ResponseEntity.status(503)
                .body(ApiResponseDto.<Map<String,Object>>error("HEALTH_CHECK_FAILED", "Detailed health check failed")
                    .withRequestId(requestId));
        }
    }

    /**
     * Readiness check endpoint
     */
    @GetMapping("/ready")
    @Operation(
        summary = "Readiness check",
        description = "Readiness check to verify service is ready to accept requests"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Service is ready"),
        @ApiResponse(responseCode = "503", description = "Service is not ready")
    })
    public ResponseEntity<ApiResponseDto<Map<String, Object>>> readiness() {
        long startTime = System.currentTimeMillis();
        String requestId = getRequestId();
        
        try {
            // Check if essential dependencies are available
            boolean isDatabaseReady = checkDatabaseReadiness();
            
            Map<String, Object> readinessInfo = new HashMap<>();
            readinessInfo.put("timestamp", LocalDateTime.now());
            readinessInfo.put("database", isDatabaseReady ? "READY" : "NOT_READY");
            
            String overallStatus = isDatabaseReady ? "READY" : "NOT_READY";
            readinessInfo.put("status", overallStatus);
            
            ApiResponseDto<Map<String, Object>> response = ApiResponseDto.success("Readiness check completed", readinessInfo)
                .withRequestId(requestId)
                .withVersion("v1")
                .withProcessingTime(System.currentTimeMillis() - startTime);
            
            if ("READY".equals(overallStatus)) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(503).body(response);
            }
            
        } catch (Exception e) {
            log.error("Readiness check failed: {}", e.getMessage());
            
            Map<String, Object> readinessInfo = new HashMap<>();
            readinessInfo.put("status", "NOT_READY");
            readinessInfo.put("timestamp", LocalDateTime.now());
            readinessInfo.put("error", e.getMessage());
            
            return ResponseEntity.status(503)
                .body(ApiResponseDto.<Map<String,Object>>error("READINESS_CHECK_FAILED", "Readiness check failed")
                    .withRequestId(requestId));
        }
    }

    /**
     * Liveness check endpoint
     */
    @GetMapping("/live")
    @Operation(
        summary = "Liveness check",
        description = "Liveness check to verify service is alive and not deadlocked"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Service is alive")
    })
    public ResponseEntity<ApiResponseDto<Map<String, Object>>> liveness() {
        long startTime = System.currentTimeMillis();
        String requestId = getRequestId();
        
        Map<String, Object> livenessInfo = new HashMap<>();
        livenessInfo.put("status", "ALIVE");
        livenessInfo.put("timestamp", LocalDateTime.now());
        livenessInfo.put("uptime", getUptime());
        
        ApiResponseDto<Map<String, Object>> response = ApiResponseDto.success("Service is alive", livenessInfo)
            .withRequestId(requestId)
            .withVersion("v1")
            .withProcessingTime(System.currentTimeMillis() - startTime);
        
        return ResponseEntity.ok(response);
    }

    // Helper methods
    private Map<String, Object> checkDatabaseHealth() {
        Map<String, Object> dbHealth = new HashMap<>();
        
        try {
            if (dataSource != null) {
                try (Connection connection = dataSource.getConnection()) {
                    boolean isValid = connection.isValid(5); // 5 second timeout
                    dbHealth.put("status", isValid ? "UP" : "DOWN");
                    dbHealth.put("database", connection.getMetaData().getDatabaseProductName());
                    dbHealth.put("url", connection.getMetaData().getURL());
                }
            } else {
                dbHealth.put("status", "DOWN");
                dbHealth.put("error", "DataSource not available");
            }
        } catch (Exception e) {
            dbHealth.put("status", "DOWN");
            dbHealth.put("error", e.getMessage());
        }
        
        return dbHealth;
    }

    private Map<String, Object> checkSystemHealth() {
        Map<String, Object> systemHealth = new HashMap<>();
        
        try {
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            
            double memoryUsagePercent = (double) usedMemory / maxMemory * 100;
            
            systemHealth.put("status", memoryUsagePercent < 90 ? "UP" : "DOWN");
            systemHealth.put("memory", Map.of(
                "max", maxMemory,
                "total", totalMemory,
                "used", usedMemory,
                "free", freeMemory,
                "usagePercent", Math.round(memoryUsagePercent * 100.0) / 100.0
            ));
            systemHealth.put("processors", runtime.availableProcessors());
            
        } catch (Exception e) {
            systemHealth.put("status", "DOWN");
            systemHealth.put("error", e.getMessage());
        }
        
        return systemHealth;
    }

    private boolean checkDatabaseReadiness() {
        try {
            if (dataSource != null) {
                try (Connection connection = dataSource.getConnection()) {
                    return connection.isValid(2); // 2 second timeout for readiness
                }
            }
            return false;
        } catch (Exception e) {
            log.debug("Database readiness check failed: {}", e.getMessage());
            return false;
        }
    }

    private String getUptime() {
        long uptimeMs = System.currentTimeMillis() - getStartTime();
        long seconds = uptimeMs / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        return String.format("%dd %dh %dm %ds", days, hours % 24, minutes % 60, seconds % 60);
    }

    private long getStartTime() {
        // This is a simplified implementation
        // In a real application, you might store the start time in a static field
        return System.currentTimeMillis() - 3600000; // Assume 1 hour uptime for demo
    }

    private String getRequestId() {
        String requestId = MDC.get("requestId");
        return requestId != null ? requestId : UUID.randomUUID().toString();
    }
}
