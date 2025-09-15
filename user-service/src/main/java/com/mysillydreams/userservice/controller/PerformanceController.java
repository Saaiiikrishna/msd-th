package com.mysillydreams.userservice.controller;

import com.mysillydreams.userservice.config.CacheConfig;
import com.mysillydreams.userservice.dto.ApiResponseDto;
import com.mysillydreams.userservice.service.PerformanceMonitoringService;
import com.mysillydreams.userservice.service.PerformanceMonitoringService.PerformanceMetrics;
import com.mysillydreams.userservice.service.PerformanceMonitoringService.HealthStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for performance monitoring and cache management.
 * Provides endpoints for system metrics, cache statistics, and performance health checks.
 */
@RestController
@RequestMapping("/api/v1/performance")
@Slf4j
@Tag(name = "Performance Monitoring", description = "APIs for performance monitoring and cache management")
@SecurityRequirement(name = "bearerAuth")
public class PerformanceController {

    private final PerformanceMonitoringService performanceMonitoringService;
    private final CacheConfig.CacheMetrics cacheMetricsService;

    public PerformanceController(PerformanceMonitoringService performanceMonitoringService,
                               CacheConfig.CacheMetrics cacheMetricsService) {
        this.performanceMonitoringService = performanceMonitoringService;
        this.cacheMetricsService = cacheMetricsService;
    }

    /**
     * Gets comprehensive performance metrics
     */
    @GetMapping("/metrics")
    @Operation(
        summary = "Get performance metrics",
        description = "Retrieves comprehensive performance metrics including response times, cache hit rates, and system resources"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Performance metrics retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Admin access required")
    })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponseDto<PerformanceMonitoringService.PerformanceMetrics>> getPerformanceMetrics() {
        long startTime = System.currentTimeMillis();
        String requestId = getRequestId();

        try {
            log.debug("Retrieving performance metrics");

            PerformanceMonitoringService.PerformanceMetrics metrics = 
                performanceMonitoringService.getPerformanceMetrics();

            ApiResponseDto<PerformanceMonitoringService.PerformanceMetrics> response = 
                ApiResponseDto.success("Performance metrics retrieved successfully", metrics)
                    .withRequestId(requestId)
                    .withVersion("v1")
                    .withProcessingTime(System.currentTimeMillis() - startTime);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error retrieving performance metrics: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                .body(ApiResponseDto.<PerformanceMetrics>error("METRICS_RETRIEVAL_FAILED", "Failed to retrieve performance metrics")
                    .withRequestId(requestId));
        }
    }

    /**
     * Gets system health status
     */
    @GetMapping("/health")
    @Operation(
        summary = "Get system health status",
        description = "Retrieves system health status based on performance metrics and thresholds"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Health status retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Admin access required")
    })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponseDto<PerformanceMonitoringService.HealthStatus>> getHealthStatus() {
        long startTime = System.currentTimeMillis();
        String requestId = getRequestId();

        try {
            log.debug("Retrieving system health status");

            PerformanceMonitoringService.HealthStatus healthStatus = 
                performanceMonitoringService.getHealthStatus();

            ApiResponseDto<PerformanceMonitoringService.HealthStatus> response = 
                ApiResponseDto.success("Health status retrieved successfully", healthStatus)
                    .withRequestId(requestId)
                    .withVersion("v1")
                    .withProcessingTime(System.currentTimeMillis() - startTime);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error retrieving health status: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                .body(ApiResponseDto.<HealthStatus>error("HEALTH_STATUS_FAILED", "Failed to retrieve health status")
                    .withRequestId(requestId));
        }
    }

    /**
     * Gets cache statistics
     */
    @GetMapping("/cache/stats")
    @Operation(
        summary = "Get cache statistics",
        description = "Retrieves detailed cache statistics including hit rates and cache sizes"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Cache statistics retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Admin access required")
    })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponseDto<Map<String, Object>>> getCacheStatistics() {
        long startTime = System.currentTimeMillis();
        String requestId = getRequestId();

        try {
            log.debug("Retrieving cache statistics");

            Map<String, Object> cacheStats = cacheMetricsService.getCacheStatistics();

            ApiResponseDto<Map<String, Object>> response = 
                ApiResponseDto.success("Cache statistics retrieved successfully", cacheStats)
                    .withRequestId(requestId)
                    .withVersion("v1")
                    .withProcessingTime(System.currentTimeMillis() - startTime);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error retrieving cache statistics: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                .body(ApiResponseDto.<Map<String,Object>>error("CACHE_STATS_FAILED", "Failed to retrieve cache statistics")
                    .withRequestId(requestId));
        }
    }

    /**
     * Clears all caches
     */
    @PostMapping("/cache/clear")
    @Operation(
        summary = "Clear all caches",
        description = "Clears all application caches to force fresh data loading"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "All caches cleared successfully"),
        @ApiResponse(responseCode = "403", description = "Admin access required")
    })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponseDto<String>> clearAllCaches() {
        long startTime = System.currentTimeMillis();
        String requestId = getRequestId();

        try {
            log.info("Clearing all caches");

            cacheMetricsService.clearAllCaches();

            ApiResponseDto<String> response = 
                ApiResponseDto.success("All caches cleared successfully", "Cache clear operation completed")
                    .withRequestId(requestId)
                    .withVersion("v1")
                    .withProcessingTime(System.currentTimeMillis() - startTime);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error clearing caches: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                .body(ApiResponseDto.<String>error("CACHE_CLEAR_FAILED", "Failed to clear caches")
                    .withRequestId(requestId));
        }
    }

    /**
     * Clears specific cache
     */
    @PostMapping("/cache/clear/{cacheName}")
    @Operation(
        summary = "Clear specific cache",
        description = "Clears a specific cache by name"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Cache cleared successfully"),
        @ApiResponse(responseCode = "404", description = "Cache not found"),
        @ApiResponse(responseCode = "403", description = "Admin access required")
    })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponseDto<String>> clearCache(@PathVariable String cacheName) {
        long startTime = System.currentTimeMillis();
        String requestId = getRequestId();

        try {
            log.info("Clearing cache: {}", cacheName);

            cacheMetricsService.clearCache(cacheName);

            ApiResponseDto<String> response = 
                ApiResponseDto.success("Cache cleared successfully", "Cache '" + cacheName + "' cleared")
                    .withRequestId(requestId)
                    .withVersion("v1")
                    .withProcessingTime(System.currentTimeMillis() - startTime);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error clearing cache {}: {}", cacheName, e.getMessage(), e);
            return ResponseEntity.status(500)
                .body(ApiResponseDto.<String>error("CACHE_CLEAR_FAILED", "Failed to clear cache: " + cacheName)
                    .withRequestId(requestId));
        }
    }

    /**
     * Resets performance counters
     */
    @PostMapping("/counters/reset")
    @Operation(
        summary = "Reset performance counters",
        description = "Resets all performance counters to zero for fresh monitoring"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Performance counters reset successfully"),
        @ApiResponse(responseCode = "403", description = "Admin access required")
    })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponseDto<String>> resetPerformanceCounters() {
        long startTime = System.currentTimeMillis();
        String requestId = getRequestId();

        try {
            log.info("Resetting performance counters");

            performanceMonitoringService.resetCounters();

            ApiResponseDto<String> response = 
                ApiResponseDto.success("Performance counters reset successfully", "All counters reset to zero")
                    .withRequestId(requestId)
                    .withVersion("v1")
                    .withProcessingTime(System.currentTimeMillis() - startTime);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error resetting performance counters: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                .body(ApiResponseDto.<String>error("COUNTER_RESET_FAILED", "Failed to reset performance counters")
                    .withRequestId(requestId));
        }
    }

    /**
     * Gets JVM memory information
     */
    @GetMapping("/memory")
    @Operation(
        summary = "Get JVM memory information",
        description = "Retrieves detailed JVM memory usage information"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Memory information retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Admin access required")
    })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponseDto<Map<String, Object>>> getMemoryInfo() {
        long startTime = System.currentTimeMillis();
        String requestId = getRequestId();

        try {
            log.debug("Retrieving JVM memory information");

            Runtime runtime = Runtime.getRuntime();
            Map<String, Object> memoryInfo = Map.of(
                "maxMemoryMB", runtime.maxMemory() / 1024 / 1024,
                "totalMemoryMB", runtime.totalMemory() / 1024 / 1024,
                "freeMemoryMB", runtime.freeMemory() / 1024 / 1024,
                "usedMemoryMB", (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024,
                "availableProcessors", runtime.availableProcessors()
            );

            ApiResponseDto<Map<String, Object>> response = 
                ApiResponseDto.success("Memory information retrieved successfully", memoryInfo)
                    .withRequestId(requestId)
                    .withVersion("v1")
                    .withProcessingTime(System.currentTimeMillis() - startTime);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error retrieving memory information: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                .body(ApiResponseDto.<Map<String,Object>>error("MEMORY_INFO_FAILED", "Failed to retrieve memory information")
                    .withRequestId(requestId));
        }
    }

    // Helper methods
    private String getRequestId() {
        String requestId = MDC.get("requestId");
        return requestId != null ? requestId : UUID.randomUUID().toString();
    }
}
