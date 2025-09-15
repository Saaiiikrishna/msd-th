package com.mysillydreams.treasure.messaging.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mysillydreams.treasure.domain.model.UserStatistics;
import com.mysillydreams.treasure.domain.repository.UserStatisticsRepository;
import com.mysillydreams.treasure.domain.service.LeaderboardService;
import com.mysillydreams.treasure.messaging.TopicNames;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Consumer for user lifecycle events from User Service
 * Handles user creation, updates, and deletion events
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserLifecycleConsumer {

    private final UserStatisticsRepository userStatsRepo;
    private final LeaderboardService leaderboardService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Handle user creation events
     * Initialize user statistics when a new user is created
     */
    @KafkaListener(topics = "user-events", groupId = "treasure-service")
    @Transactional
    public void handleUserEvent(String message) {
        try {
            JsonNode root = objectMapper.readTree(message);
            String eventType = root.path("eventType").asText();
            String userRef = root.path("userReferenceId").asText();
            
            if (userRef == null || userRef.isEmpty()) {
                log.warn("Received user event without userReferenceId: {}", message);
                return;
            }
            
            UUID userId = UUID.fromString(userRef);
            
            switch (eventType) {
                case "USER_CREATED":
                    handleUserCreated(userId, root);
                    break;
                case "USER_UPDATED":
                    handleUserUpdated(userId, root);
                    break;
                case "USER_DELETED":
                    handleUserDeleted(userId, root);
                    break;
                case "USER_SUSPENDED":
                    handleUserSuspended(userId, root);
                    break;
                case "USER_REACTIVATED":
                    handleUserReactivated(userId, root);
                    break;
                default:
                    log.debug("Unhandled user event type: {}", eventType);
            }
            
        } catch (Exception e) {
            log.error("Failed to process user lifecycle event: {}", message, e);
        }
    }

    /**
     * Handle user creation - initialize user statistics
     */
    private void handleUserCreated(UUID userId, JsonNode eventData) {
        log.info("Initializing user statistics for new user: {}", userId);
        
        try {
            // Check if user statistics already exist
            if (!userStatsRepo.findByUserIdOrderByDifficulty(userId).isEmpty()) {
                log.debug("User statistics already exist for user: {}", userId);
                return;
            }
            
            // Create initial user statistics record
            UserStatistics userStats = UserStatistics.builder()
                    .userId(userId)
                    .build();
            
            userStatsRepo.save(userStats);
            log.info("Created initial user statistics for user: {}", userId);
            
        } catch (Exception e) {
            log.error("Failed to initialize user statistics for user: {}", userId, e);
        }
    }

    /**
     * Handle user updates - may need to update cached user data
     */
    private void handleUserUpdated(UUID userId, JsonNode eventData) {
        log.debug("Processing user update for user: {}", userId);
        
        // Extract updated fields if needed
        String email = eventData.path("email").asText(null);
        String firstName = eventData.path("firstName").asText(null);
        String lastName = eventData.path("lastName").asText(null);
        
        // Update any cached user information if necessary
        // For now, just log the update
        log.debug("User {} updated - email: {}, name: {} {}", userId, email, firstName, lastName);
    }

    /**
     * Handle user deletion - clean up user data
     */
    private void handleUserDeleted(UUID userId, JsonNode eventData) {
        log.info("Processing user deletion for user: {}", userId);
        
        try {
            // Note: We typically don't delete user statistics for audit purposes
            // Instead, we might mark them as deleted or anonymize them
            
            // For GDPR compliance, we might need to anonymize or delete user data
            // This depends on business requirements and legal obligations
            
            log.info("User deletion processed for user: {}", userId);
            
        } catch (Exception e) {
            log.error("Failed to process user deletion for user: {}", userId, e);
        }
    }

    /**
     * Handle user suspension - may need to pause active enrollments
     */
    private void handleUserSuspended(UUID userId, JsonNode eventData) {
        log.info("Processing user suspension for user: {}", userId);
        
        try {
            // Handle user suspension logic
            // e.g., pause active enrollments, hide from leaderboards, etc.
            
            log.info("User suspension processed for user: {}", userId);
            
        } catch (Exception e) {
            log.error("Failed to process user suspension for user: {}", userId, e);
        }
    }

    /**
     * Handle user reactivation - restore user access
     */
    private void handleUserReactivated(UUID userId, JsonNode eventData) {
        log.info("Processing user reactivation for user: {}", userId);
        
        try {
            // Handle user reactivation logic
            // e.g., restore enrollments, add back to leaderboards, etc.
            
            log.info("User reactivation processed for user: {}", userId);
            
        } catch (Exception e) {
            log.error("Failed to process user reactivation for user: {}", userId, e);
        }
    }
}
