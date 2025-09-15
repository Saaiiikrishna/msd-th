package com.mysillydreams.userservice.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Entity for implementing the transactional outbox pattern.
 * Ensures reliable event publishing with database transactions.
 */
@Entity
@Table(name = "outbox_events", indexes = {
    @Index(name = "idx_outbox_events_processed", columnList = "processed, created_at"),
    @Index(name = "idx_outbox_events_aggregate", columnList = "aggregate_type, aggregate_id"),
    @Index(name = "idx_outbox_events_retry", columnList = "retry_count, created_at")
})
@Data
@EqualsAndHashCode(of = "id")
public class OutboxEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Enumerated(EnumType.STRING)
    @Column(name = "aggregate_type", nullable = false, length = 50)
    private AggregateType aggregateType;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "event_data", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> eventData;

    // Processing status
    @Column(name = "processed", nullable = false)
    private Boolean processed = false;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;

    @Column(name = "last_error", length = 2000)
    private String lastError;

    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

    @Column(name = "processing_started_at")
    private LocalDateTime processingStartedAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "kafka_offset")
    private Long kafkaOffset;

    @Column(name = "kafka_partition")
    private Integer kafkaPartition;

    @Column(name = "last_retry_at")
    private LocalDateTime lastRetryAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private EventStatus status = EventStatus.PENDING;

    // Metadata
    @Column(name = "correlation_id", length = 100)
    private String correlationId;

    @Column(name = "causation_id", length = 100)
    private String causationId;

    @Column(name = "message_id", length = 100)
    private String messageId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Constructors
    public OutboxEventEntity() {}

    public OutboxEventEntity(UUID aggregateId, AggregateType aggregateType, 
                           String eventType, Map<String, Object> eventData) {
        this.aggregateId = aggregateId;
        this.aggregateType = aggregateType;
        this.eventType = eventType;
        this.eventData = eventData;
        this.processed = false;
        this.retryCount = 0;
    }

    public OutboxEventEntity(UUID aggregateId, AggregateType aggregateType, 
                           String eventType, Map<String, Object> eventData,
                           String correlationId, String causationId) {
        this(aggregateId, aggregateType, eventType, eventData);
        this.correlationId = correlationId;
        this.causationId = causationId;
    }

    // Business methods
    public boolean isProcessed() {
        return processed != null && processed;
    }

    public boolean canRetry() {
        return !isProcessed() && retryCount < 10 && 
               (nextRetryAt == null || nextRetryAt.isBefore(LocalDateTime.now()));
    }

    public void markAsProcessed() {
        this.processed = true;
        this.processedAt = LocalDateTime.now();
        this.lastError = null;
        this.nextRetryAt = null;
    }

    public void markAsFailed(String error) {
        this.retryCount++;
        this.lastError = error;
        
        // Exponential backoff: 2^retryCount minutes
        int delayMinutes = (int) Math.pow(2, Math.min(retryCount, 8));
        this.nextRetryAt = LocalDateTime.now().plusMinutes(delayMinutes);
    }

    public void resetRetries() {
        this.retryCount = 0;
        this.lastError = null;
        this.nextRetryAt = null;
    }

    public boolean hasMaxRetriesExceeded() {
        return retryCount >= 10;
    }

    // Aggregate types
    public enum AggregateType {
        USER("User aggregate"),
        ADDRESS("Address aggregate"),
        CONSENT("Consent aggregate"),
        ROLE("Role aggregate"),
        SESSION("Session aggregate");

        private final String description;

        AggregateType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    // Event status types
    public enum EventStatus {
        PENDING("Event is pending processing"),
        PROCESSING("Event is currently being processed"),
        PUBLISHED("Event has been successfully published"),
        FAILED("Event processing failed");

        private final String description;

        EventStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    // Common event types (constants)
    public static final class EventTypes {
        // User events
        public static final String USER_CREATED = "user.created.v1";
        public static final String USER_UPDATED = "user.updated.v1";
        public static final String USER_DELETED = "user.deleted.v1";
        public static final String USER_DATA_DELETED = "user.data.deleted.v1";
        public static final String USER_ARCHIVED = "user.archived.v1";
        public static final String USER_REACTIVATED = "user.reactivated.v1";

        // Role events
        public static final String USER_ROLE_ASSIGNED = "user.role.assigned.v1";
        public static final String USER_ROLE_REMOVED = "user.role.removed.v1";

        // Consent events
        public static final String USER_CONSENT_GRANTED = "user.consent.granted.v1";
        public static final String USER_CONSENT_WITHDRAWN = "user.consent.withdrawn.v1";

        // Address events
        public static final String USER_ADDRESS_ADDED = "user.address.added.v1";
        public static final String USER_ADDRESS_UPDATED = "user.address.updated.v1";
        public static final String USER_ADDRESS_DELETED = "user.address.deleted.v1";

        // Session events
        public static final String USER_SESSION_CREATED = "user.session.created.v1";
        public static final String USER_SESSION_TERMINATED = "user.session.terminated.v1";

        private EventTypes() {
            // Utility class
        }
    }

    // Validation
    @PrePersist
    @PreUpdate
    private void validateEntity() {
        if (aggregateId == null) {
            throw new IllegalStateException("Aggregate ID cannot be null");
        }
        
        if (aggregateType == null) {
            throw new IllegalStateException("Aggregate type cannot be null");
        }
        
        if (eventType == null || eventType.trim().isEmpty()) {
            throw new IllegalStateException("Event type cannot be null or empty");
        }
        
        if (eventData == null || eventData.isEmpty()) {
            throw new IllegalStateException("Event data cannot be null or empty");
        }
        
        if (processed == null) {
            processed = false;
        }
        
        if (retryCount == null) {
            retryCount = 0;
        }
        
        if (retryCount < 0) {
            throw new IllegalStateException("Retry count cannot be negative");
        }
        
        if (retryCount > 10) {
            throw new IllegalStateException("Retry count cannot exceed 10");
        }
    }

    // Helper methods for creating events
    public static OutboxEventEntity createUserEvent(UUID userId, String eventType, Map<String, Object> eventData) {
        return new OutboxEventEntity(userId, AggregateType.USER, eventType, eventData);
    }

    public static OutboxEventEntity createAddressEvent(UUID addressId, String eventType, Map<String, Object> eventData) {
        return new OutboxEventEntity(addressId, AggregateType.ADDRESS, eventType, eventData);
    }

    public static OutboxEventEntity createConsentEvent(UUID userId, String eventType, Map<String, Object> eventData) {
        return new OutboxEventEntity(userId, AggregateType.CONSENT, eventType, eventData);
    }

    public static OutboxEventEntity createRoleEvent(UUID userId, String eventType, Map<String, Object> eventData) {
        return new OutboxEventEntity(userId, AggregateType.ROLE, eventType, eventData);
    }

    public static OutboxEventEntity createSessionEvent(UUID sessionId, String eventType, Map<String, Object> eventData) {
        return new OutboxEventEntity(sessionId, AggregateType.SESSION, eventType, eventData);
    }

    public static OutboxEventEntity createGdprEvent(UUID userId, String eventType, Map<String, Object> eventData) {
        return new OutboxEventEntity(userId, AggregateType.USER, eventType, eventData);
    }
}
