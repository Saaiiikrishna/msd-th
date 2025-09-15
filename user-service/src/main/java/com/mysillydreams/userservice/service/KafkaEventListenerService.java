package com.mysillydreams.userservice.service;

import com.mysillydreams.userservice.event.UserEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

/**
 * Service for listening to Kafka events from other services.
 * Handles cross-service communication and event-driven workflows.
 */
@Service
@Slf4j
@ConditionalOnProperty(name = "app.events.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class KafkaEventListenerService {

    private final AuditService auditService;

    public KafkaEventListenerService(AuditService auditService) {
        this.auditService = auditService;
    }

    /**
     * Listens to user events from other services
     */
    @KafkaListener(
        topics = "${app.events.kafka.topics.user-events:user-events}",
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleUserEvent(
            @Payload UserEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {
        
        try {
            log.debug("Received user event: {} for user: {} from topic: {}", 
                event.getEventType(), event.getUserReferenceId(), topic);

            // Process the event based on type
            switch (event.getEventType()) {
                case "USER_CREATED":
                    handleUserCreatedEvent(event);
                    break;
                case "USER_UPDATED":
                    handleUserUpdatedEvent(event);
                    break;
                case "USER_DELETED":
                    handleUserDeletedEvent(event);
                    break;
                case "USER_REACTIVATED":
                    handleUserReactivatedEvent(event);
                    break;
                case "USER_ROLE_CHANGED":
                    handleUserRoleChangedEvent(event);
                    break;
                case "USER_DATA_DELETION":
                    handleUserDataDeletionEvent(event);
                    break;
                default:
                    log.debug("Unhandled user event type: {}", event.getEventType());
            }

            // Acknowledge successful processing
            acknowledgment.acknowledge();
            
            log.debug("Successfully processed user event: {} for user: {}", 
                event.getEventType(), event.getUserReferenceId());

        } catch (Exception e) {
            log.error("Error processing user event: {} for user: {} - {}", 
                event.getEventType(), event.getUserReferenceId(), e.getMessage(), e);
            
            // Don't acknowledge - message will be retried
            // In production, you might want to send to dead letter queue after max retries
        }
    }

    /**
     * Listens to consent events
     */
    @KafkaListener(
        topics = "${app.events.kafka.topics.consent-events:consent-events}",
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleConsentEvent(
            @Payload UserEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment acknowledgment) {
        
        try {
            log.debug("Received consent event: {} for user: {}", 
                event.getEventType(), event.getUserReferenceId());

            // Process consent events for compliance tracking
            processConsentEvent(event);

            acknowledgment.acknowledge();
            
        } catch (Exception e) {
            log.error("Error processing consent event: {} for user: {} - {}", 
                event.getEventType(), event.getUserReferenceId(), e.getMessage(), e);
        }
    }

    /**
     * Listens to GDPR events
     */
    @KafkaListener(
        topics = "${app.events.kafka.topics.gdpr-events:gdpr-events}",
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleGdprEvent(
            @Payload UserEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment acknowledgment) {
        
        try {
            log.info("Received GDPR event: {} for user: {}", 
                event.getEventType(), event.getUserReferenceId());

            // Process GDPR events for compliance
            processGdprEvent(event);

            acknowledgment.acknowledge();
            
        } catch (Exception e) {
            log.error("Error processing GDPR event: {} for user: {} - {}", 
                event.getEventType(), event.getUserReferenceId(), e.getMessage(), e);
        }
    }

    /**
     * Listens to audit events
     */
    @KafkaListener(
        topics = "${app.events.kafka.topics.audit-events:audit-events}",
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleAuditEvent(
            @Payload UserEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment acknowledgment) {
        
        try {
            log.debug("Received audit event: {} for user: {}", 
                event.getEventType(), event.getUserReferenceId());

            // Process audit events for compliance tracking
            processAuditEvent(event);

            acknowledgment.acknowledge();
            
        } catch (Exception e) {
            log.error("Error processing audit event: {} for user: {} - {}", 
                event.getEventType(), event.getUserReferenceId(), e.getMessage(), e);
        }
    }

    /**
     * Listens to events from other services (e.g., payment, treasure)
     */
    @KafkaListener(
        topics = {"payment-events", "treasure-events", "notification-events"},
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleExternalServiceEvent(
            @Payload Object event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            Acknowledgment acknowledgment) {
        
        try {
            log.debug("Received external service event from topic: {} with key: {}", topic, key);

            // Process events from other services
            processExternalServiceEvent(topic, key, event);

            acknowledgment.acknowledge();
            
        } catch (Exception e) {
            log.error("Error processing external service event from topic: {} - {}", topic, e.getMessage(), e);
        }
    }

    // Event processing methods

    private void handleUserCreatedEvent(UserEvent event) {
        log.debug("Processing user created event for user: {}", event.getUserReferenceId());
        // Add any business logic for when users are created
        // e.g., send welcome email, initialize user preferences, etc.
    }

    private void handleUserUpdatedEvent(UserEvent event) {
        log.debug("Processing user updated event for user: {}", event.getUserReferenceId());
        // Add any business logic for when users are updated
        // e.g., sync with external systems, update caches, etc.
    }

    private void handleUserDeletedEvent(UserEvent event) {
        log.debug("Processing user deleted event for user: {}", event.getUserReferenceId());
        // Add any business logic for when users are deleted
        // e.g., cleanup related data, notify other services, etc.
    }

    private void handleUserReactivatedEvent(UserEvent event) {
        log.debug("Processing user reactivated event for user: {}", event.getUserReferenceId());
        // Add any business logic for when users are reactivated
    }

    private void handleUserRoleChangedEvent(UserEvent event) {
        log.debug("Processing user role changed event for user: {}", event.getUserReferenceId());
        // Add any business logic for when user roles change
        // e.g., update permissions, notify administrators, etc.
    }

    private void handleUserDataDeletionEvent(UserEvent event) {
        log.info("Processing user data deletion event for user: {}", event.getUserReferenceId());
        // Add any business logic for GDPR data deletion
        // e.g., notify other services to delete related data
    }

    private void processConsentEvent(UserEvent event) {
        log.debug("Processing consent event for user: {}", event.getUserReferenceId());
        // Add consent processing logic
        // e.g., update marketing lists, compliance tracking, etc.
    }

    private void processGdprEvent(UserEvent event) {
        log.info("Processing GDPR event for user: {}", event.getUserReferenceId());
        // Add GDPR processing logic
        // e.g., compliance reporting, data deletion coordination, etc.
    }

    private void processAuditEvent(UserEvent event) {
        log.debug("Processing audit event for user: {}", event.getUserReferenceId());
        // Add audit processing logic
        // e.g., security monitoring, compliance reporting, etc.
    }

    private void processExternalServiceEvent(String topic, String key, Object event) {
        log.debug("Processing external service event from topic: {} with key: {}", topic, key);
        
        switch (topic) {
            case "payment-events":
                processPaymentEvent(key, event);
                break;
            case "treasure-events":
                processTreasureEvent(key, event);
                break;
            case "notification-events":
                processNotificationEvent(key, event);
                break;
            default:
                log.debug("Unhandled external service topic: {}", topic);
        }
    }

    private void processPaymentEvent(String key, Object event) {
        log.debug("Processing payment event with key: {}", key);
        // Add payment event processing logic
        // e.g., update user payment status, handle payment failures, etc.
    }

    private void processTreasureEvent(String key, Object event) {
        log.debug("Processing treasure event with key: {}", key);
        // Add treasure event processing logic
        // e.g., update user treasure balance, handle treasure transactions, etc.
    }

    private void processNotificationEvent(String key, Object event) {
        log.debug("Processing notification event with key: {}", key);
        // Add notification event processing logic
        // e.g., track notification delivery, handle bounces, etc.
    }
}
