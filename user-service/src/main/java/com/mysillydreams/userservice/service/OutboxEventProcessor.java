package com.mysillydreams.userservice.service;

import com.mysillydreams.userservice.domain.OutboxEventEntity;
import com.mysillydreams.userservice.repository.OutboxEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Service for processing outbox events and publishing them to Kafka.
 * Implements the transactional outbox pattern for reliable event publishing.
 */
@Service
@Slf4j
@ConditionalOnProperty(name = "app.events.outbox.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxEventProcessor {

    private final OutboxEventRepository outboxEventRepository;
    
    @Autowired(required = false)
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.events.outbox.batch-size:100}")
    private int batchSize;

    @Value("${app.events.outbox.max-retry-attempts:3}")
    private int maxRetryAttempts;

    @Value("${app.events.kafka.enabled:true}")
    private boolean kafkaEnabled;

    @Value("${app.events.kafka.topics.user-events:user-events}")
    private String userEventsTopic;

    @Value("${app.events.kafka.topics.consent-events:consent-events}")
    private String consentEventsTopic;

    @Value("${app.events.kafka.topics.gdpr-events:gdpr-events}")
    private String gdprEventsTopic;

    @Value("${app.events.kafka.topics.audit-events:audit-events}")
    private String auditEventsTopic;

    public OutboxEventProcessor(OutboxEventRepository outboxEventRepository) {
        this.outboxEventRepository = outboxEventRepository;
    }

    /**
     * Scheduled task to process pending outbox events
     * Runs every 30 seconds to ensure timely event publishing
     */
    @Scheduled(fixedDelay = 30000) // 30 seconds
    @Transactional
    public void processOutboxEvents() {
        if (!kafkaEnabled || kafkaTemplate == null) {
            log.debug("Kafka is disabled, skipping outbox event processing");
            return;
        }

        try {
            List<OutboxEventEntity> pendingEvents = outboxEventRepository
                .findPendingEventsForProcessing();

            if (pendingEvents.isEmpty()) {
                log.debug("No pending outbox events to process");
                return;
            }

            log.info("Processing {} pending outbox events", pendingEvents.size());

            for (OutboxEventEntity event : pendingEvents) {
                processOutboxEvent(event);
            }

            log.info("Completed processing {} outbox events", pendingEvents.size());

        } catch (Exception e) {
            log.error("Error processing outbox events: {}", e.getMessage(), e);
        }
    }

    /**
     * Processes a single outbox event
     */
    @Async
    public void processOutboxEvent(OutboxEventEntity event) {
        try {
            log.debug("Processing outbox event: {} of type: {}", event.getId(), event.getEventType());

            // Mark as processing
            event.setProcessingStartedAt(LocalDateTime.now());
            event.setStatus(OutboxEventEntity.EventStatus.PROCESSING);
            outboxEventRepository.save(event);

            // Determine target topic based on event type
            String topic = determineTargetTopic(event);
            String key = determineMessageKey(event);

            // Publish to Kafka
            CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(
                topic, 
                key, 
                createKafkaMessage(event)
            );

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    handleSuccessfulPublishing(event, result);
                } else {
                    handleFailedPublishing(event, ex);
                }
            });

        } catch (Exception e) {
            log.error("Error processing outbox event {}: {}", event.getId(), e.getMessage(), e);
            handleFailedPublishing(event, e);
        }
    }

    /**
     * Handles successful event publishing
     */
    @Transactional
    public void handleSuccessfulPublishing(OutboxEventEntity event, SendResult<String, Object> result) {
        try {
            event.setStatus(OutboxEventEntity.EventStatus.PUBLISHED);
            event.setPublishedAt(LocalDateTime.now());
            event.setKafkaOffset(result.getRecordMetadata().offset());
            event.setKafkaPartition(result.getRecordMetadata().partition());
            
            outboxEventRepository.save(event);
            
            log.debug("Successfully published outbox event: {} to topic: {} partition: {} offset: {}", 
                event.getId(), 
                result.getRecordMetadata().topic(),
                result.getRecordMetadata().partition(),
                result.getRecordMetadata().offset());

        } catch (Exception e) {
            log.error("Error updating outbox event {} after successful publishing: {}", 
                event.getId(), e.getMessage(), e);
        }
    }

    /**
     * Handles failed event publishing
     */
    @Transactional
    public void handleFailedPublishing(OutboxEventEntity event, Throwable error) {
        try {
            event.setRetryCount(event.getRetryCount() + 1);
            event.setLastError(error.getMessage());
            event.setLastRetryAt(LocalDateTime.now());

            if (event.getRetryCount() >= maxRetryAttempts) {
                event.setStatus(OutboxEventEntity.EventStatus.FAILED);
                log.error("Outbox event {} failed after {} attempts: {}", 
                    event.getId(), maxRetryAttempts, error.getMessage());
            } else {
                event.setStatus(OutboxEventEntity.EventStatus.PENDING);
                // Calculate next retry time with exponential backoff
                LocalDateTime nextRetry = LocalDateTime.now()
                    .plusMinutes((long) Math.pow(2, event.getRetryCount()));
                event.setNextRetryAt(nextRetry);
                
                log.warn("Outbox event {} failed (attempt {}), will retry at {}: {}", 
                    event.getId(), event.getRetryCount(), nextRetry, error.getMessage());
            }

            outboxEventRepository.save(event);

        } catch (Exception e) {
            log.error("Error updating outbox event {} after failed publishing: {}", 
                event.getId(), e.getMessage(), e);
        }
    }

    /**
     * Determines the target Kafka topic based on event type
     */
    private String determineTargetTopic(OutboxEventEntity event) {
        String eventType = event.getEventType();
        
        if (eventType.startsWith("USER_")) {
            return userEventsTopic;
        } else if (eventType.startsWith("CONSENT_")) {
            return consentEventsTopic;
        } else if (eventType.startsWith("GDPR_") || eventType.startsWith("DATA_")) {
            return gdprEventsTopic;
        } else if (eventType.startsWith("AUDIT_")) {
            return auditEventsTopic;
        } else {
            return userEventsTopic; // Default topic
        }
    }

    /**
     * Determines the message key for Kafka partitioning
     */
    private String determineMessageKey(OutboxEventEntity event) {
        // Use aggregate ID as key for proper partitioning
        if (event.getAggregateId() != null) {
            return event.getAggregateId().toString();
        }
        
        // Fallback to event ID
        return event.getId().toString();
    }

    /**
     * Creates the Kafka message from outbox event
     */
    private Object createKafkaMessage(OutboxEventEntity event) {
        // Create a simplified message structure
        return new OutboxKafkaMessage(
            event.getId().toString(),
            event.getEventType(),
            event.getAggregateType().name(),
            event.getAggregateId() != null ? event.getAggregateId().toString() : null,
            event.getEventData(),
            event.getCreatedAt(),
            event.getCorrelationId(),
            event.getCausationId()
        );
    }

    /**
     * Cleanup processed events older than specified days
     */
    @Scheduled(cron = "0 0 2 * * ?") // Daily at 2 AM
    @Transactional
    public void cleanupProcessedEvents() {
        try {
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(7); // Keep for 7 days
            int deletedCount = outboxEventRepository.deleteProcessedEventsBefore(cutoffDate);
            
            if (deletedCount > 0) {
                log.info("Cleaned up {} processed outbox events older than {}", deletedCount, cutoffDate);
            }

        } catch (Exception e) {
            log.error("Error cleaning up processed outbox events: {}", e.getMessage(), e);
        }
    }

    /**
     * Gets outbox event processing statistics
     */
    public OutboxProcessingStats getProcessingStats() {
        try {
            long pendingCount = outboxEventRepository.countByStatus(OutboxEventEntity.EventStatus.PENDING);
            long processingCount = outboxEventRepository.countByStatus(OutboxEventEntity.EventStatus.PROCESSING);
            long publishedCount = outboxEventRepository.countByStatus(OutboxEventEntity.EventStatus.PUBLISHED);
            long failedCount = outboxEventRepository.countByStatus(OutboxEventEntity.EventStatus.FAILED);

            return new OutboxProcessingStats(pendingCount, processingCount, publishedCount, failedCount);

        } catch (Exception e) {
            log.error("Error getting outbox processing stats: {}", e.getMessage(), e);
            return new OutboxProcessingStats(0, 0, 0, 0);
        }
    }

    // Supporting classes

    /**
     * Kafka message structure for outbox events
     */
    public static class OutboxKafkaMessage {
        private String eventId;
        private String eventType;
        private String aggregateType;
        private String aggregateId;
        private Object eventData;
        private LocalDateTime timestamp;
        private String correlationId;
        private String causationId;

        public OutboxKafkaMessage(String eventId, String eventType, String aggregateType, 
                                 String aggregateId, Object eventData, LocalDateTime timestamp,
                                 String correlationId, String causationId) {
            this.eventId = eventId;
            this.eventType = eventType;
            this.aggregateType = aggregateType;
            this.aggregateId = aggregateId;
            this.eventData = eventData;
            this.timestamp = timestamp;
            this.correlationId = correlationId;
            this.causationId = causationId;
        }

        // Getters
        public String getEventId() { return eventId; }
        public String getEventType() { return eventType; }
        public String getAggregateType() { return aggregateType; }
        public String getAggregateId() { return aggregateId; }
        public Object getEventData() { return eventData; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public String getCorrelationId() { return correlationId; }
        public String getCausationId() { return causationId; }
    }

    /**
     * Outbox processing statistics
     */
    public static class OutboxProcessingStats {
        private long pendingEvents;
        private long processingEvents;
        private long publishedEvents;
        private long failedEvents;

        public OutboxProcessingStats(long pendingEvents, long processingEvents, 
                                   long publishedEvents, long failedEvents) {
            this.pendingEvents = pendingEvents;
            this.processingEvents = processingEvents;
            this.publishedEvents = publishedEvents;
            this.failedEvents = failedEvents;
        }

        // Getters
        public long getPendingEvents() { return pendingEvents; }
        public long getProcessingEvents() { return processingEvents; }
        public long getPublishedEvents() { return publishedEvents; }
        public long getFailedEvents() { return failedEvents; }
        public long getTotalEvents() { return pendingEvents + processingEvents + publishedEvents + failedEvents; }
    }
}
