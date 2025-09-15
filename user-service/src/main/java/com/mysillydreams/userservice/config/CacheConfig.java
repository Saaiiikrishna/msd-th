package com.mysillydreams.userservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Unified Redis and Cache configuration for the User Service.
 *
 * This class provides a single, authoritative configuration for:
 * 1.  Connecting to the Redis server (via RedisConnectionFactory).
 * 2.  Providing a RedisTemplate for direct, low-level Redis operations.
 * 3.  Configuring a primary, production-grade CacheManager for Spring's caching abstraction (@Cacheable, etc.).
 * 4.  Defining a fallback in-memory cache for local development or if Redis is unavailable.
 * 5.  Custom cache error handling, key generation, and metrics.
 */
@Configuration
@EnableCaching
@Slf4j
public class CacheConfig {

    // --- Redis Connection Properties ---
    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Value("${spring.data.redis.database:1}")
    private int redisDatabase;

    // --- Cache TTL Properties (Tuned for E-commerce) ---
    @Value("${app.cache.default-ttl:600s}") // 10 minutes
    private Duration defaultTtl;

    @Value("${app.auth.cache-ttl:30s}") // 30 seconds (short for security)
    private Duration authCacheTtl;

    @Value("${app.cache.user-lookup-ttl:900s}") // 15 minutes
    private Duration userLookupTtl;

    @Value("${app.cache.user-profile.ttl:1800s}") // 30 minutes
    private Duration userProfileTtl;

    @Value("${app.cache.role-hierarchy.ttl:3600s}") // 1 hour
    private Duration roleHierarchyTtl;

    @Value("${app.cache.consent-status.ttl:1800s}") // 30 minutes
    private Duration consentStatusTtl;

    @Value("${app.cache.address-lookup.ttl:1800s}") // 30 minutes
    private Duration addressLookupTtl;

    // --- Cache Name Constants ---
    public static final String SESSION_INTROSPECTION_CACHE = "session-introspection";
    public static final String USER_LOOKUP_CACHE = "userLookup";
    public static final String USER_PROFILE_CACHE = "userProfile";
    public static final String ROLE_HIERARCHY_CACHE = "roleHierarchy";
    public static final String CONSENT_STATUS_CACHE = "consentStatus";
    public static final String ADDRESS_LOOKUP_CACHE = "addressLookup";
    public static final String HMAC_LOOKUP_CACHE = "hmacLookup";
    public static final String SESSION_CACHE = "sessionCache";

    // --- Core Redis Beans ---

    /**
     * Configures the connection factory for Lettuce, the Redis client.
     * This bean is conditional on `spring.data.redis.host` being set.
     */
    @Bean
    @ConditionalOnProperty(name = "spring.data.redis.host")
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(redisHost);
        config.setPort(redisPort);
        config.setDatabase(redisDatabase);

        if (redisPassword != null && !redisPassword.trim().isEmpty()) {
            config.setPassword(redisPassword);
        }

        return new LettuceConnectionFactory(config);
    }

    /**
     * Provides a RedisTemplate for direct interaction with Redis.
     * Configured with String keys and JSON values for storing complex objects.
     */
    @Bean
    @ConditionalOnProperty(name = "spring.data.redis.host")
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Use String serializer for keys
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // Use JSON serializer for values
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());

        template.afterPropertiesSet();
        return template;
    }


    // --- Cache Manager Beans ---

    /**
     * The primary, Redis-based cache manager with custom TTL configurations.
     * This is the default cache manager used by the application.
     */
    @Bean
    @Primary
    @ConditionalOnProperty(name = "spring.data.redis.host")
    public CacheManager redisCacheManager(RedisConnectionFactory redisConnectionFactory,
                                        ObjectMapper objectMapper) {

        log.info("Configuring Redis cache manager with default TTL: {}", defaultTtl);

        // Create custom ObjectMapper for cache serialization
        ObjectMapper cacheObjectMapper = objectMapper.copy();
        cacheObjectMapper.activateDefaultTyping(
            cacheObjectMapper.getPolymorphicTypeValidator(),
            ObjectMapper.DefaultTyping.NON_FINAL
        );

        // Default cache configuration
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(defaultTtl)
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer(cacheObjectMapper)))
                .disableCachingNullValues();

        // Cache-specific configurations
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // Session introspection cache - short TTL for security
        cacheConfigurations.put(SESSION_INTROSPECTION_CACHE,
                defaultConfig.entryTtl(authCacheTtl));

        // User lookup cache - medium TTL for performance
        cacheConfigurations.put(USER_LOOKUP_CACHE,
                defaultConfig.entryTtl(userLookupTtl));

        // User profile cache - longer TTL as profiles change less frequently
        cacheConfigurations.put(USER_PROFILE_CACHE,
                defaultConfig.entryTtl(userProfileTtl));

        // Role hierarchy cache - very long TTL as roles rarely change
        cacheConfigurations.put(ROLE_HIERARCHY_CACHE,
                defaultConfig.entryTtl(roleHierarchyTtl));

        // Consent cache - medium TTL for compliance queries
        cacheConfigurations.put(CONSENT_STATUS_CACHE,
                defaultConfig.entryTtl(consentStatusTtl));

        // Address lookup cache - medium TTL for address queries
        cacheConfigurations.put(ADDRESS_LOOKUP_CACHE,
                defaultConfig.entryTtl(addressLookupTtl));

        // HMAC lookup cache - short TTL for security
        cacheConfigurations.put(HMAC_LOOKUP_CACHE,
                defaultConfig.entryTtl(userLookupTtl));

        // Session cache - short TTL for active sessions
        cacheConfigurations.put(SESSION_CACHE,
                defaultConfig.entryTtl(authCacheTtl));

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()
                .build();
    }

    /**
     * Fallback in-memory cache manager used when Redis is not available.
     */
    @Bean
    @ConditionalOnProperty(name = "spring.data.redis.host", havingValue = "false", matchIfMissing = true)
    public CacheManager inMemoryCacheManager() {
        log.warn("Redis not configured, using in-memory cache manager");

        return new org.springframework.cache.concurrent.ConcurrentMapCacheManager(
                SESSION_INTROSPECTION_CACHE,
                USER_LOOKUP_CACHE,
                USER_PROFILE_CACHE,
                ROLE_HIERARCHY_CACHE,
                CONSENT_STATUS_CACHE,
                ADDRESS_LOOKUP_CACHE,
                HMAC_LOOKUP_CACHE,
                SESSION_CACHE
        );
    }

    // --- Helper Beans ---

    /**
     * Cache error handler to prevent cache failures from breaking the application.
     */
    @Bean
    public org.springframework.cache.interceptor.CacheErrorHandler cacheErrorHandler() {
        return new org.springframework.cache.interceptor.SimpleCacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception,
                                          org.springframework.cache.Cache cache,
                                          Object key) {
                log.warn("Cache GET error for cache '{}' and key '{}': {}",
                        cache.getName(), key, exception.getMessage());
                // Don't throw exception, just log and continue without cache
            }

            @Override
            public void handleCachePutError(RuntimeException exception,
                                          org.springframework.cache.Cache cache,
                                          Object key, Object value) {
                log.warn("Cache PUT error for cache '{}' and key '{}': {}",
                        cache.getName(), key, exception.getMessage());
                // Don't throw exception, just log and continue without cache
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception,
                                            org.springframework.cache.Cache cache,
                                            Object key) {
                log.warn("Cache EVICT error for cache '{}' and key '{}': {}",
                        cache.getName(), key, exception.getMessage());
                // Don't throw exception, just log and continue
            }

            @Override
            public void handleCacheClearError(RuntimeException exception,
                                            org.springframework.cache.Cache cache) {
                log.warn("Cache CLEAR error for cache '{}': {}",
                        cache.getName(), exception.getMessage());
                // Don't throw exception, just log and continue
            }
        };
    }

    /**
     * Cache statistics and monitoring.
     */
    @Bean
    public CacheMetrics cacheMetrics(CacheManager cacheManager) {
        return new CacheMetrics(cacheManager);
    }

    /**
     * Simple cache metrics collector.
     */
    public static class CacheMetrics {
        private final CacheManager cacheManager;

        public CacheMetrics(CacheManager cacheManager) {
            this.cacheManager = cacheManager;
        }

        public Map<String, Object> getCacheStatistics() {
            Map<String, Object> stats = new HashMap<>();

            cacheManager.getCacheNames().forEach(cacheName -> {
                var cache = cacheManager.getCache(cacheName);
                if (cache != null) {
                    // Basic cache information
                    stats.put(cacheName + ".name", cacheName);
                    stats.put(cacheName + ".nativeCache", cache.getNativeCache().getClass().getSimpleName());
                }
            });

            return stats;
        }

        public void clearAllCaches() {
            cacheManager.getCacheNames().forEach(cacheName -> {
                var cache = cacheManager.getCache(cacheName);
                if (cache != null) {
                    cache.clear();
                }
            });
        }


        public void clearCache(String cacheName) {
            var cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
        }

        public void evictUserFromAllCaches(String userReferenceId) {
            // Evict user from all relevant caches
            evictFromCache(USER_PROFILE_CACHE, userReferenceId);
            evictFromCache(ADDRESS_LOOKUP_CACHE, userReferenceId);
        }

        private void evictFromCache(String cacheName, String key) {
            var cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.evict(key);
            }
        }
    }

    /**
     * Cache key generator for consistent key naming across the application.
     */
    @Bean
    public CacheKeyGenerator cacheKeyGenerator() {
        return new CacheKeyGenerator();
    }

    public static class CacheKeyGenerator {

        public String generateUserLookupKey(String email) {
            return "user:lookup:email:" + email.toLowerCase();
        }

        public String generateUserLookupKeyByPhone(String phone) {
            return "user:lookup:phone:" + phone;
        }

        public String generateUserProfileKey(String userReferenceId) {
            return "user:profile:" + userReferenceId;
        }

        public String generateHmacLookupKey(String hmac) {
            return "hmac:lookup:" + hmac;
        }

        public String generateConsentStatusKey(String userReferenceId, String consentKey) {
            return "consent:status:" + userReferenceId + ":" + consentKey;
        }

        public String generateAddressLookupKey(String userReferenceId) {
            return "address:lookup:" + userReferenceId;
        }

        public String generateSessionKey(String sessionId) {
            return "session:" + sessionId;
        }

        public String generateRoleHierarchyKey(String role) {
            return "role:hierarchy:" + role;
        }
    }
}
