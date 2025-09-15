package com.mysillydreams.userservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.boot.actuator.metrics.MetricsEndpoint; // Disabled for Azure deployment
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service for monitoring application performance metrics.
 * Tracks database connections, cache hit rates, response times, and system resources.
 */
@Service
@Slf4j
public class PerformanceMonitoringService {

    private final DataSource dataSource;
    private final CacheManager cacheManager;
    private final RedisTemplate<String, Object> redisTemplate;
    
    @Autowired(required = false)
    // private MetricsEndpoint metricsEndpoint; // Disabled for Azure deployment

    // Performance counters
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong totalResponseTime = new AtomicLong(0);
    private final AtomicLong cacheHits = new AtomicLong(0);
    private final AtomicLong cacheMisses = new AtomicLong(0);
    private final AtomicLong databaseQueries = new AtomicLong(0);
    private final AtomicLong databaseQueryTime = new AtomicLong(0);

    public PerformanceMonitoringService(DataSource dataSource, 
                                      CacheManager cacheManager,
                                      RedisTemplate<String, Object> redisTemplate) {
        this.dataSource = dataSource;
        this.cacheManager = cacheManager;
        this.redisTemplate = redisTemplate;
    }

    /**
     * Records a request with its response time
     */
    public void recordRequest(long responseTimeMs) {
        totalRequests.incrementAndGet();
        totalResponseTime.addAndGet(responseTimeMs);
        
        if (responseTimeMs > 1000) { // Log slow requests
            log.warn("Slow request detected: {}ms", responseTimeMs);
        }
    }

    /**
     * Records a cache hit
     */
    public void recordCacheHit() {
        cacheHits.incrementAndGet();
    }

    /**
     * Records a cache miss
     */
    public void recordCacheMiss() {
        cacheMisses.incrementAndGet();
    }

    /**
     * Records a database query with its execution time
     */
    public void recordDatabaseQuery(long queryTimeMs) {
        databaseQueries.incrementAndGet();
        databaseQueryTime.addAndGet(queryTimeMs);
        
        if (queryTimeMs > 500) { // Log slow queries
            log.warn("Slow database query detected: {}ms", queryTimeMs);
        }
    }

    /**
     * Gets comprehensive performance metrics
     */
    public PerformanceMetrics getPerformanceMetrics() {
        PerformanceMetrics metrics = new PerformanceMetrics();
        
        // Request metrics
        long requests = totalRequests.get();
        metrics.setTotalRequests(requests);
        metrics.setAverageResponseTime(requests > 0 ? (double) totalResponseTime.get() / requests : 0);
        
        // Cache metrics
        long hits = cacheHits.get();
        long misses = cacheMisses.get();
        long totalCacheRequests = hits + misses;
        metrics.setCacheHits(hits);
        metrics.setCacheMisses(misses);
        metrics.setCacheHitRate(totalCacheRequests > 0 ? (double) hits / totalCacheRequests : 0);
        
        // Database metrics
        long queries = databaseQueries.get();
        metrics.setDatabaseQueries(queries);
        metrics.setAverageDatabaseQueryTime(queries > 0 ? (double) databaseQueryTime.get() / queries : 0);
        
        // System metrics
        metrics.setSystemMetrics(getSystemMetrics());
        
        // Database connection metrics
        metrics.setDatabaseMetrics(getDatabaseMetrics());
        
        // Cache system metrics
        metrics.setCacheSystemMetrics(getCacheSystemMetrics());
        
        metrics.setTimestamp(LocalDateTime.now());
        
        return metrics;
    }

    /**
     * Gets system resource metrics
     */
    private Map<String, Object> getSystemMetrics() {
        Map<String, Object> systemMetrics = new HashMap<>();
        
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        systemMetrics.put("maxMemoryMB", maxMemory / 1024 / 1024);
        systemMetrics.put("totalMemoryMB", totalMemory / 1024 / 1024);
        systemMetrics.put("usedMemoryMB", usedMemory / 1024 / 1024);
        systemMetrics.put("freeMemoryMB", freeMemory / 1024 / 1024);
        systemMetrics.put("memoryUsagePercent", (double) usedMemory / maxMemory * 100);
        systemMetrics.put("availableProcessors", runtime.availableProcessors());
        
        return systemMetrics;
    }

    /**
     * Gets database connection metrics
     */
    private Map<String, Object> getDatabaseMetrics() {
        Map<String, Object> dbMetrics = new HashMap<>();
        
        try (Connection connection = dataSource.getConnection()) {
            dbMetrics.put("connected", true);
            dbMetrics.put("autoCommit", connection.getAutoCommit());
            dbMetrics.put("readOnly", connection.isReadOnly());
            dbMetrics.put("transactionIsolation", connection.getTransactionIsolation());
            dbMetrics.put("catalog", connection.getCatalog());
            
            // Database metadata
            var metaData = connection.getMetaData();
            dbMetrics.put("databaseProductName", metaData.getDatabaseProductName());
            dbMetrics.put("databaseProductVersion", metaData.getDatabaseProductVersion());
            dbMetrics.put("driverName", metaData.getDriverName());
            dbMetrics.put("driverVersion", metaData.getDriverVersion());
            dbMetrics.put("url", metaData.getURL());
            
        } catch (SQLException e) {
            dbMetrics.put("connected", false);
            dbMetrics.put("error", e.getMessage());
            log.error("Error getting database metrics: {}", e.getMessage());
        }
        
        return dbMetrics;
    }

    /**
     * Gets cache system metrics
     */
    private Map<String, Object> getCacheSystemMetrics() {
        Map<String, Object> cacheMetrics = new HashMap<>();
        
        try {
            // Cache manager info
            cacheMetrics.put("cacheManagerType", cacheManager.getClass().getSimpleName());
            cacheMetrics.put("cacheNames", cacheManager.getCacheNames());
            
            // Redis connection info
            String ping = redisTemplate.getConnectionFactory().getConnection().ping();
            cacheMetrics.put("redisConnected", "PONG".equals(ping));
            cacheMetrics.put("redisPing", ping);
            
            // Redis info (if available)
            try {
                var redisConnection = redisTemplate.getConnectionFactory().getConnection();
                var info = redisConnection.info();
                cacheMetrics.put("redisInfo", info.toString());
            } catch (Exception e) {
                log.debug("Could not get Redis info: {}", e.getMessage());
            }
            
        } catch (Exception e) {
            cacheMetrics.put("error", e.getMessage());
            log.error("Error getting cache metrics: {}", e.getMessage());
        }
        
        return cacheMetrics;
    }

    /**
     * Gets health status based on performance metrics
     */
    public HealthStatus getHealthStatus() {
        PerformanceMetrics metrics = getPerformanceMetrics();
        HealthStatus status = new HealthStatus();
        
        // Check response time health
        if (metrics.getAverageResponseTime() > 2000) {
            status.addIssue("High average response time: " + metrics.getAverageResponseTime() + "ms");
        }
        
        // Check cache hit rate health
        if (metrics.getCacheHitRate() < 0.7) {
            status.addIssue("Low cache hit rate: " + (metrics.getCacheHitRate() * 100) + "%");
        }
        
        // Check memory usage health
        Map<String, Object> systemMetrics = metrics.getSystemMetrics();
        Double memoryUsage = (Double) systemMetrics.get("memoryUsagePercent");
        if (memoryUsage != null && memoryUsage > 85) {
            status.addIssue("High memory usage: " + memoryUsage + "%");
        }
        
        // Check database health
        Map<String, Object> dbMetrics = metrics.getDatabaseMetrics();
        Boolean dbConnected = (Boolean) dbMetrics.get("connected");
        if (dbConnected == null || !dbConnected) {
            status.addIssue("Database connection issue");
        }
        
        // Check cache health
        Map<String, Object> cacheSystemMetrics = metrics.getCacheSystemMetrics();
        Boolean redisConnected = (Boolean) cacheSystemMetrics.get("redisConnected");
        if (redisConnected == null || !redisConnected) {
            status.addIssue("Redis connection issue");
        }
        
        status.setHealthy(status.getIssues().isEmpty());
        status.setTimestamp(LocalDateTime.now());
        
        return status;
    }

    /**
     * Resets performance counters
     */
    public void resetCounters() {
        totalRequests.set(0);
        totalResponseTime.set(0);
        cacheHits.set(0);
        cacheMisses.set(0);
        databaseQueries.set(0);
        databaseQueryTime.set(0);
        
        log.info("Performance counters reset");
    }

    // Supporting classes

    public static class PerformanceMetrics {
        private long totalRequests;
        private double averageResponseTime;
        private long cacheHits;
        private long cacheMisses;
        private double cacheHitRate;
        private long databaseQueries;
        private double averageDatabaseQueryTime;
        private Map<String, Object> systemMetrics;
        private Map<String, Object> databaseMetrics;
        private Map<String, Object> cacheSystemMetrics;
        private LocalDateTime timestamp;

        // Getters and setters
        public long getTotalRequests() { return totalRequests; }
        public void setTotalRequests(long totalRequests) { this.totalRequests = totalRequests; }
        public double getAverageResponseTime() { return averageResponseTime; }
        public void setAverageResponseTime(double averageResponseTime) { this.averageResponseTime = averageResponseTime; }
        public long getCacheHits() { return cacheHits; }
        public void setCacheHits(long cacheHits) { this.cacheHits = cacheHits; }
        public long getCacheMisses() { return cacheMisses; }
        public void setCacheMisses(long cacheMisses) { this.cacheMisses = cacheMisses; }
        public double getCacheHitRate() { return cacheHitRate; }
        public void setCacheHitRate(double cacheHitRate) { this.cacheHitRate = cacheHitRate; }
        public long getDatabaseQueries() { return databaseQueries; }
        public void setDatabaseQueries(long databaseQueries) { this.databaseQueries = databaseQueries; }
        public double getAverageDatabaseQueryTime() { return averageDatabaseQueryTime; }
        public void setAverageDatabaseQueryTime(double averageDatabaseQueryTime) { this.averageDatabaseQueryTime = averageDatabaseQueryTime; }
        public Map<String, Object> getSystemMetrics() { return systemMetrics; }
        public void setSystemMetrics(Map<String, Object> systemMetrics) { this.systemMetrics = systemMetrics; }
        public Map<String, Object> getDatabaseMetrics() { return databaseMetrics; }
        public void setDatabaseMetrics(Map<String, Object> databaseMetrics) { this.databaseMetrics = databaseMetrics; }
        public Map<String, Object> getCacheSystemMetrics() { return cacheSystemMetrics; }
        public void setCacheSystemMetrics(Map<String, Object> cacheSystemMetrics) { this.cacheSystemMetrics = cacheSystemMetrics; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    }

    public static class HealthStatus {
        private boolean healthy;
        private java.util.List<String> issues = new java.util.ArrayList<>();
        private LocalDateTime timestamp;

        public void addIssue(String issue) {
            issues.add(issue);
        }

        // Getters and setters
        public boolean isHealthy() { return healthy; }
        public void setHealthy(boolean healthy) { this.healthy = healthy; }
        public java.util.List<String> getIssues() { return issues; }
        public void setIssues(java.util.List<String> issues) { this.issues = issues; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    }
}
