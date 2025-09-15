package com.mysillydreams.userservice.config;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.noop.NoopTimer;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.exporter.logging.LoggingSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.support.VaultHealth;
import org.springframework.web.filter.CommonsRequestLoggingFilter;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Configuration for observability features including metrics, tracing, and logging.
 * Provides comprehensive monitoring capabilities for the User Service.
 */
@Configuration
@Slf4j
public class ObservabilityConfig {

    @Value("${app.observability.request-logging.enabled:true}")
    private boolean requestLoggingEnabled;

    @Value("${app.observability.metrics.enabled:true}")
    private boolean metricsEnabled;

    /**
     * Request logging filter for detailed HTTP request/response logging.
     */
    @Bean
    public CommonsRequestLoggingFilter requestLoggingFilter() {
        if (!requestLoggingEnabled) {
            return null; // Or a disabled dummy filter
        }

        CommonsRequestLoggingFilter filter = new CommonsRequestLoggingFilter();
        filter.setIncludeQueryString(true);
        filter.setIncludePayload(true);
        filter.setMaxPayloadLength(10000);
        filter.setIncludeHeaders(true);
        filter.setIncludeClientInfo(true);
        filter.setBeforeMessagePrefix("REQUEST: ");
        filter.setAfterMessagePrefix("RESPONSE: ");
        return filter;
    }

    /**
     * Custom metrics for business logic monitoring.
     */
    @Bean
    public UserServiceMetrics userServiceMetrics(MeterRegistry meterRegistry) {
        if (!metricsEnabled) {
            return new UserServiceMetrics(null); // Return a disabled metrics object
        }
        return new UserServiceMetrics(meterRegistry);
    }

    /**
     * Custom metrics collector for User Service specific metrics.
     */
    public static class UserServiceMetrics {
        private final MeterRegistry meterRegistry;
        private final boolean enabled;

        // Counters
        private final AtomicLong userCreatedCount = new AtomicLong(0);
        private final AtomicLong userUpdatedCount = new AtomicLong(0);
        private final AtomicLong userDeletedCount = new AtomicLong(0);
        private final AtomicLong authenticationAttempts = new AtomicLong(0);
        private final AtomicLong authenticationFailures = new AtomicLong(0);
        private final AtomicLong gdprRequestsCount = new AtomicLong(0);
        private final AtomicLong cacheHitsCount = new AtomicLong(0);
        private final AtomicLong cacheMissesCount = new AtomicLong(0);

        // Timers
        private final Timer userCreationTimer;
        private final Timer userUpdateTimer;
        private final Timer userLookupTimer;
        private final Timer databaseQueryTimer;
        private final Timer cacheOperationTimer;
        private final Timer encryptionTimer;
        private final Timer auditLogTimer;

        public UserServiceMetrics(MeterRegistry meterRegistry) {
            this.meterRegistry = meterRegistry;
            this.enabled = meterRegistry != null;

            if (!enabled) {
                // If disabled, initialize timers to no-op timers to prevent NullPointerExceptions
                Timer noopTimer = new NoopTimer(new Meter.Id("noop", io.micrometer.core.instrument.Tags.empty(), null, null, Meter.Type.TIMER));
                userCreationTimer = noopTimer;
                userUpdateTimer = noopTimer;
                userLookupTimer = noopTimer;
                databaseQueryTimer = noopTimer;
                cacheOperationTimer = noopTimer;
                encryptionTimer = noopTimer;
                auditLogTimer = noopTimer;
                return;
            }

            // Register counters
            meterRegistry.gauge("user.service.users.created.total", userCreatedCount);
            meterRegistry.gauge("user.service.users.updated.total", userUpdatedCount);
            meterRegistry.gauge("user.service.users.deleted.total", userDeletedCount);
            meterRegistry.gauge("user.service.auth.attempts.total", authenticationAttempts);
            meterRegistry.gauge("user.service.auth.failures.total", authenticationFailures);
            meterRegistry.gauge("user.service.gdpr.requests.total", gdprRequestsCount);
            meterRegistry.gauge("user.service.cache.hits.total", cacheHitsCount);
            meterRegistry.gauge("user.service.cache.misses.total", cacheMissesCount);

            // Register timers
            userCreationTimer = Timer.builder("user.service.user.creation.duration")
                    .description("Time taken to create a user")
                    .register(meterRegistry);

            userUpdateTimer = Timer.builder("user.service.user.update.duration")
                    .description("Time taken to update a user")
                    .register(meterRegistry);

            userLookupTimer = Timer.builder("user.service.user.lookup.duration")
                    .description("Time taken to lookup a user")
                    .register(meterRegistry);

            databaseQueryTimer = Timer.builder("user.service.database.query.duration")
                    .description("Time taken for database queries")
                    .register(meterRegistry);

            cacheOperationTimer = Timer.builder("user.service.cache.operation.duration")
                    .description("Time taken for cache operations")
                    .register(meterRegistry);

            encryptionTimer = Timer.builder("user.service.encryption.duration")
                    .description("Time taken for encryption/decryption operations")
                    .register(meterRegistry);

            auditLogTimer = Timer.builder("user.service.audit.log.duration")
                    .description("Time taken to write audit logs")
                    .register(meterRegistry);
        }

        // Counter methods
        public void incrementUserCreated() { if (enabled) userCreatedCount.incrementAndGet(); }
        public void incrementUserUpdated() { if (enabled) userUpdatedCount.incrementAndGet(); }
        public void incrementUserDeleted() { if (enabled) userDeletedCount.incrementAndGet(); }
        public void incrementAuthenticationAttempt() { if (enabled) authenticationAttempts.incrementAndGet(); }
        public void incrementAuthenticationFailure() { if (enabled) authenticationFailures.incrementAndGet(); }
        public void incrementGdprRequest() { if (enabled) gdprRequestsCount.incrementAndGet(); }
        public void incrementCacheHit() { if (enabled) cacheHitsCount.incrementAndGet(); }
        public void incrementCacheMiss() { if (enabled) cacheMissesCount.incrementAndGet(); }

        // Timer methods
        public Timer.Sample startUserCreationTimer() { return Timer.start(meterRegistry); }
        public void recordUserCreation(Timer.Sample sample) { if(enabled) sample.stop(userCreationTimer); }

        public Timer.Sample startUserUpdateTimer() { return Timer.start(meterRegistry); }
        public void recordUserUpdate(Timer.Sample sample) { if(enabled) sample.stop(userUpdateTimer); }

        public Timer.Sample startUserLookupTimer() { return Timer.start(meterRegistry); }
        public void recordUserLookup(Timer.Sample sample) { if(enabled) sample.stop(userLookupTimer); }

        public Timer.Sample startDatabaseQueryTimer() { return Timer.start(meterRegistry); }
        public void recordDatabaseQuery(Timer.Sample sample) { if(enabled) sample.stop(databaseQueryTimer); }

        public Timer.Sample startCacheOperationTimer() { return Timer.start(meterRegistry); }
        public void recordCacheOperation(Timer.Sample sample) { if(enabled) sample.stop(cacheOperationTimer); }

        public Timer.Sample startEncryptionTimer() { return Timer.start(meterRegistry); }
        public void recordEncryption(Timer.Sample sample) { if(enabled) sample.stop(encryptionTimer); }

        public Timer.Sample startAuditLogTimer() { return Timer.start(meterRegistry); }
        public void recordAuditLog(Timer.Sample sample) { if(enabled) sample.stop(auditLogTimer); }

        // Utility methods
        public double getAuthenticationSuccessRate() {
            long attempts = authenticationAttempts.get();
            long failures = authenticationFailures.get();
            if (attempts == 0) return 100.0;
            return (double) (attempts - failures) / attempts * 100.0;
        }

        public double getCacheHitRate() {
            long hits = cacheHitsCount.get();
            long misses = cacheMissesCount.get();
            long total = hits + misses;
            if (total == 0) return 100.0;
            return (double) hits / total * 100.0;
        }

        public long getTotalUsers() {
            return userCreatedCount.get() - userDeletedCount.get();
        }
    }

    /**
     * Health indicators for custom health checks.
     * Note: Spring Boot Actuator provides default health indicators. This is a custom aggregator.
     */
    @Bean
    public CustomHealthIndicators customHealthIndicators(DataSource dataSource, RedisConnectionFactory redisConnectionFactory, KafkaAdmin kafkaAdmin, @Autowired(required = false) VaultTemplate vaultTemplate) {
        return new CustomHealthIndicators(dataSource, redisConnectionFactory, kafkaAdmin, vaultTemplate);
    }

    public class CustomHealthIndicators {
        private final DataSource dataSource;
        private final RedisConnectionFactory redisConnectionFactory;
        private final KafkaAdmin kafkaAdmin;
        private final VaultTemplate vaultTemplate;

        public CustomHealthIndicators(DataSource dataSource, RedisConnectionFactory redisConnectionFactory, KafkaAdmin kafkaAdmin, VaultTemplate vaultTemplate) {
            this.dataSource = dataSource;
            this.redisConnectionFactory = redisConnectionFactory;
            this.kafkaAdmin = kafkaAdmin;
            this.vaultTemplate = vaultTemplate;
        }

        public boolean isDatabaseHealthy() {
            try (Connection connection = dataSource.getConnection()) {
                return connection.isValid(1); // Check connection validity with a 1-second timeout
            } catch (Exception e) {
                log.error("Database health check failed", e);
                return false;
            }
        }

        public boolean isCacheHealthy() {
            try {
                this.redisConnectionFactory.getConnection().ping();
                return true;
            } catch (Exception e) {
                log.error("Cache (Redis) health check failed", e);
                return false;
            }
        }

        public boolean isKafkaHealthy() {
            try (AdminClient adminClient = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
                adminClient.describeCluster().clusterId().get(2, TimeUnit.SECONDS);
                return true;
            } catch (Exception e) {
                log.error("Kafka health check failed", e);
                return false;
            }
        }

        public boolean isVaultHealthy() {
            if (vaultTemplate == null) {
                log.info("Vault is not configured, skipping health check.");
                return true; // Return true if not configured to avoid false negatives
            }
            try {
                VaultHealth health = vaultTemplate.opsForSys().health();
                return health.isInitialized() && !health.isSealed();
            } catch (Exception e) {
                log.error("Vault health check failed", e);
                return false;
            }
        }

        public Map<String, Object> getDetailedHealthStatus() {
            Map<String, Object> health = new HashMap<>();
            health.put("database", isDatabaseHealthy());
            health.put("cache", isCacheHealthy());
            health.put("kafka", isKafkaHealthy());
            health.put("vault", isVaultHealthy());
            health.put("timestamp", java.time.LocalDateTime.now());
            return health;
        }
    }

    /**
     * Distributed tracing configuration using OpenTelemetry.
     * This basic setup exports traces to the console. For production, replace ConsoleSpanExporter
     * with an exporter for your tracing backend (e.g., Jaeger, Zipkin, OTLP).
     */
    @Bean
    public OpenTelemetry openTelemetry() {
        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(SimpleSpanProcessor.create(LoggingSpanExporter.create()))
                .build();

        OpenTelemetrySdk openTelemetrySdk = OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .buildAndRegisterGlobal();

        log.info("Configured OpenTelemetry with LoggingSpanExporter for distributed tracing.");
        return openTelemetrySdk;
    }

    @Bean
    public Tracer tracer(OpenTelemetry openTelemetry) {
        return openTelemetry.getTracer("user-service");
    }
}
