package com.mysillydreams.userservice.service;

import com.mysillydreams.userservice.domain.OutboxEventEntity;
import com.mysillydreams.userservice.domain.UserEntity;
import com.mysillydreams.userservice.event.*;
import com.mysillydreams.userservice.repository.OutboxEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Service for publishing domain events using both transactional outbox pattern and direct Kafka publishing.
 * Ensures reliable event publishing with database transactions and real-time event streaming.
 */
@Service
@Slf4j
@Transactional
public class EventPublishingService {

    private final OutboxEventRepository outboxEventRepository;

    @Autowired(required = false)
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.events.kafka.topics.user-events:user-events}")
    private String userEventsTopic;

    @Value("${app.events.kafka.topics.consent-events:consent-events}")
    private String consentEventsTopic;

    @Value("${app.events.kafka.topics.gdpr-events:gdpr-events}")
    private String gdprEventsTopic;

    @Value("${app.events.kafka.topics.audit-events:audit-events}")
    private String auditEventsTopic;

    @Value("${app.events.kafka.enabled:true}")
    private boolean kafkaEnabled;

    public EventPublishingService(OutboxEventRepository outboxEventRepository) {
        this.outboxEventRepository = outboxEventRepository;
    }

    /**
     * Publishes user created event to both outbox and Kafka
     */
    public void publishUserCreatedEvent(UserEntity user) {
        log.debug("Publishing user created event for user: {}", user.getReferenceId());

        // Create outbox event for transactional guarantee
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("userReferenceId", user.getReferenceId());
        eventData.put("userId", user.getId().toString());
        eventData.put("active", user.getActive());
        eventData.put("createdAt", user.getCreatedAt() != null ? user.getCreatedAt().toString() : LocalDateTime.now().toString());
        eventData.put("roles", user.getRoleNames());

        OutboxEventEntity event = OutboxEventEntity.createUserEvent(
            user.getId(),
            OutboxEventEntity.EventTypes.USER_CREATED,
            eventData
        );

        addEventContext(event);
        outboxEventRepository.save(event);

        // Publish to Kafka for real-time processing
        if (kafkaEnabled && kafkaTemplate != null) {
            publishUserCreatedEventToKafka(user);
        }

        log.debug("Published user created event: {}", event.getId());
    }

    /**
     * Publishes user created event directly to Kafka
     */
    @Async
    public void publishUserCreatedEventToKafka(UserEntity user) {
        try {
            UserCreatedEvent kafkaEvent = new UserCreatedEvent(
                user.getReferenceId(),
                user.getId(),
                "***@***.com", // Masked for privacy in events
                "***", // Masked for privacy in events
                "***", // Masked for privacy in events
                "***", // Masked for privacy in events
                user.getGender() != null ? user.getGender().name() : null,
                user.getRoleNames()
            );

            kafkaEvent.withCorrelationId(getCorrelationId());

            CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(
                userEventsTopic,
                user.getReferenceId(),
                kafkaEvent
            );

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.debug("User created event sent to Kafka successfully for user: {}", user.getReferenceId());
                } else {
                    log.error("Failed to send user created event to Kafka for user: {}", user.getReferenceId(), ex);
                }
            });

        } catch (Exception e) {
            log.error("Error publishing user created event to Kafka for user: {}", user.getReferenceId(), e);
        }
    }

    /**
     * Publishes user updated event
     */
    public void publishUserUpdatedEvent(UserEntity user, Map<String, Object> changes) {
        log.debug("Publishing user updated event for user: {}", user.getReferenceId());

        Map<String, Object> eventData = new HashMap<>();
        eventData.put("userReferenceId", user.getReferenceId());
        eventData.put("userId", user.getId().toString());
        eventData.put("updatedAt", user.getUpdatedAt() != null ? user.getUpdatedAt().toString() : LocalDateTime.now().toString());
        eventData.put("changes", changes);

        OutboxEventEntity event = OutboxEventEntity.createUserEvent(
            user.getId(),
            OutboxEventEntity.EventTypes.USER_UPDATED,
            eventData
        );

        addEventContext(event);
        outboxEventRepository.save(event);
        log.debug("Published user updated event: {}", event.getId());
    }

    /**
     * Publishes user deleted event
     */
    public void publishUserDeletedEvent(UserEntity user, String reason) {
        log.debug("Publishing user deleted event for user: {}", user.getReferenceId());

        Map<String, Object> eventData = new HashMap<>();
        eventData.put("userReferenceId", user.getReferenceId());
        eventData.put("userId", user.getId().toString());
        eventData.put("deletedAt", user.getDeletedAt() != null ? user.getDeletedAt().toString() : null);
        eventData.put("reason", reason);

        OutboxEventEntity event = OutboxEventEntity.createUserEvent(
            user.getId(),
            OutboxEventEntity.EventTypes.USER_DELETED,
            eventData
        );

        addEventContext(event);
        outboxEventRepository.save(event);
        log.debug("Published user deleted event: {}", event.getId());
    }

    /**
     * Publishes user reactivated event
     */
    public void publishUserReactivatedEvent(UserEntity user) {
        log.debug("Publishing user reactivated event for user: {}", user.getReferenceId());

        Map<String, Object> eventData = new HashMap<>();
        eventData.put("userReferenceId", user.getReferenceId());
        eventData.put("userId", user.getId().toString());
        eventData.put("reactivatedAt", user.getUpdatedAt().toString());
        eventData.put("active", user.getActive());

        OutboxEventEntity event = OutboxEventEntity.createUserEvent(
            user.getId(),
            OutboxEventEntity.EventTypes.USER_REACTIVATED,
            eventData
        );

        addEventContext(event);
        outboxEventRepository.save(event);
        log.debug("Published user reactivated event: {}", event.getId());
    }

    /**
     * Publishes user role assigned event
     */
    public void publishUserRoleAssignedEvent(UserEntity user, String role, LocalDateTime expiresAt) {
        log.debug("Publishing user role assigned event for user: {} role: {}", user.getReferenceId(), role);

        Map<String, Object> eventData = new HashMap<>();
        eventData.put("userReferenceId", user.getReferenceId());
        eventData.put("userId", user.getId().toString());
        eventData.put("role", role);
        eventData.put("expiresAt", expiresAt != null ? expiresAt.toString() : null);
        eventData.put("assignedAt", LocalDateTime.now().toString());

        OutboxEventEntity event = OutboxEventEntity.createRoleEvent(
            user.getId(),
            OutboxEventEntity.EventTypes.USER_ROLE_ASSIGNED,
            eventData
        );

        addEventContext(event);
        outboxEventRepository.save(event);
        log.debug("Published user role assigned event: {}", event.getId());
    }

    /**
     * Publishes user role removed event
     */
    public void publishUserRoleRemovedEvent(UserEntity user, String role) {
        log.debug("Publishing user role removed event for user: {} role: {}", user.getReferenceId(), role);

        Map<String, Object> eventData = new HashMap<>();
        eventData.put("userReferenceId", user.getReferenceId());
        eventData.put("userId", user.getId().toString());
        eventData.put("role", role);
        eventData.put("removedAt", LocalDateTime.now().toString());

        OutboxEventEntity event = OutboxEventEntity.createRoleEvent(
            user.getId(),
            OutboxEventEntity.EventTypes.USER_ROLE_REMOVED,
            eventData
        );

        addEventContext(event);
        outboxEventRepository.save(event);
        log.debug("Published user role removed event: {}", event.getId());
    }

    /**
     * Publishes user consent granted event
     */
    public void publishUserConsentGrantedEvent(UserEntity user, String consentKey, String consentVersion) {
        log.debug("Publishing user consent granted event for user: {} consent: {}", user.getReferenceId(), consentKey);

        Map<String, Object> eventData = new HashMap<>();
        eventData.put("userReferenceId", user.getReferenceId());
        eventData.put("userId", user.getId().toString());
        eventData.put("consentKey", consentKey);
        eventData.put("consentVersion", consentVersion);
        eventData.put("grantedAt", LocalDateTime.now().toString());

        OutboxEventEntity event = OutboxEventEntity.createConsentEvent(
            user.getId(),
            OutboxEventEntity.EventTypes.USER_CONSENT_GRANTED,
            eventData
        );

        addEventContext(event);
        outboxEventRepository.save(event);
        log.debug("Published user consent granted event: {}", event.getId());
    }

    /**
     * Publishes user consent withdrawn event
     */
    public void publishUserConsentWithdrawnEvent(UserEntity user, String consentKey) {
        log.debug("Publishing user consent withdrawn event for user: {} consent: {}", user.getReferenceId(), consentKey);

        Map<String, Object> eventData = new HashMap<>();
        eventData.put("userReferenceId", user.getReferenceId());
        eventData.put("userId", user.getId().toString());
        eventData.put("consentKey", consentKey);
        eventData.put("withdrawnAt", LocalDateTime.now().toString());

        OutboxEventEntity event = OutboxEventEntity.createConsentEvent(
            user.getId(),
            OutboxEventEntity.EventTypes.USER_CONSENT_WITHDRAWN,
            eventData
        );

        addEventContext(event);
        outboxEventRepository.save(event);
        log.debug("Published user consent withdrawn event: {}", event.getId());
    }

    /**
     * Publishes user address added event
     */
    public void publishUserAddressAddedEvent(UserEntity user, UUID addressId, String addressType) {
        log.debug("Publishing user address added event for user: {}", user.getReferenceId());

        Map<String, Object> eventData = new HashMap<>();
        eventData.put("userReferenceId", user.getReferenceId());
        eventData.put("userId", user.getId().toString());
        eventData.put("addressId", addressId.toString());
        eventData.put("addressType", addressType);
        eventData.put("addedAt", LocalDateTime.now().toString());

        OutboxEventEntity event = OutboxEventEntity.createAddressEvent(
            addressId,
            OutboxEventEntity.EventTypes.USER_ADDRESS_ADDED,
            eventData
        );

        addEventContext(event);
        outboxEventRepository.save(event);
        log.debug("Published user address added event: {}", event.getId());
    }

    /**
     * Publishes user address updated event
     */
    public void publishUserAddressUpdatedEvent(UserEntity user, UUID addressId, String addressType) {
        log.debug("Publishing user address updated event for user: {}", user.getReferenceId());

        Map<String, Object> eventData = new HashMap<>();
        eventData.put("userReferenceId", user.getReferenceId());
        eventData.put("userId", user.getId().toString());
        eventData.put("addressId", addressId.toString());
        eventData.put("addressType", addressType);
        eventData.put("updatedAt", LocalDateTime.now().toString());

        OutboxEventEntity event = OutboxEventEntity.createAddressEvent(
            addressId,
            OutboxEventEntity.EventTypes.USER_ADDRESS_UPDATED,
            eventData
        );

        addEventContext(event);
        outboxEventRepository.save(event);
        log.debug("Published user address updated event: {}", event.getId());
    }

    /**
     * Publishes user address deleted event
     */
    public void publishUserAddressDeletedEvent(UserEntity user, UUID addressId, String addressType) {
        log.debug("Publishing user address deleted event for user: {}", user.getReferenceId());

        Map<String, Object> eventData = new HashMap<>();
        eventData.put("userReferenceId", user.getReferenceId());
        eventData.put("userId", user.getId().toString());
        eventData.put("addressId", addressId.toString());
        eventData.put("addressType", addressType);
        eventData.put("deletedAt", LocalDateTime.now().toString());

        OutboxEventEntity event = OutboxEventEntity.createAddressEvent(
            addressId,
            OutboxEventEntity.EventTypes.USER_ADDRESS_DELETED,
            eventData
        );

        addEventContext(event);
        outboxEventRepository.save(event);
        log.debug("Published user address deleted event: {}", event.getId());
    }

    /**
     * Publishes user data deletion event for GDPR compliance
     */
    public void publishUserDataDeletionEvent(UserEntity user, UUID deletionRequestId, String reason) {
        log.debug("Publishing user data deletion event for user: {}", user.getReferenceId());

        // Outbox event
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("userReferenceId", user.getReferenceId());
        eventData.put("userId", user.getId().toString());
        eventData.put("deletionRequestId", deletionRequestId.toString());
        eventData.put("reason", reason);
        eventData.put("deletedAt", LocalDateTime.now().toString());

        OutboxEventEntity event = OutboxEventEntity.createGdprEvent(
            user.getId(),
            OutboxEventEntity.EventTypes.USER_DATA_DELETED,
            eventData
        );

        addEventContext(event);
        outboxEventRepository.save(event);

        // Kafka event
        if (kafkaEnabled && kafkaTemplate != null) {
            publishUserDataDeletionEventToKafka(user, deletionRequestId, reason);
        }

        log.debug("Published user data deletion event: {}", event.getId());
    }

    /**
     * Publishes user data deletion event to Kafka
     */
    @Async
    public void publishUserDataDeletionEventToKafka(UserEntity user, UUID deletionRequestId, String reason) {
        try {
            UserDataDeletionEvent kafkaEvent = new UserDataDeletionEvent(
                user.getReferenceId(),
                user.getId(),
                deletionRequestId,
                reason,
                0, // Will be updated by deletion service
                new HashMap<>(),
                true,
                getCurrentUserId()
            );

            kafkaEvent.withCorrelationId(getCorrelationId());

            CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(
                gdprEventsTopic,
                user.getReferenceId(),
                kafkaEvent
            );

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.debug("User data deletion event sent to Kafka successfully for user: {}", user.getReferenceId());
                } else {
                    log.error("Failed to send user data deletion event to Kafka for user: {}", user.getReferenceId(), ex);
                }
            });

        } catch (Exception e) {
            log.error("Error publishing user data deletion event to Kafka for user: {}", user.getReferenceId(), e);
        }
    }

    // Helper methods

    private String getCorrelationId() {
        String correlationId = MDC.get("correlationId");
        return correlationId != null ? correlationId : UUID.randomUUID().toString();
    }

    private UUID getCurrentUserId() {
        try {
            return com.mysillydreams.userservice.security.RoleHierarchyConfig.SecurityUtils.getCurrentUserId();
        } catch (Exception e) {
            return null;
        }
    }

    private void addEventContext(OutboxEventEntity event) {
        try {
            String correlationId = MDC.get("correlationId");
            String causationId = MDC.get("causationId");
            
            if (correlationId != null) {
                event.setCorrelationId(correlationId);
            }
            
            if (causationId != null) {
                event.setCausationId(causationId);
            }
            
            // Generate message ID for deduplication
            event.setMessageId(generateMessageId(event));
            
        } catch (Exception e) {
            log.debug("Failed to add event context: {}", e.getMessage());
        }
    }

    private String generateMessageId(OutboxEventEntity event) {
        return String.format("%s-%s-%d", 
            event.getAggregateType(), 
            event.getEventType(), 
            System.currentTimeMillis());
    }
}
