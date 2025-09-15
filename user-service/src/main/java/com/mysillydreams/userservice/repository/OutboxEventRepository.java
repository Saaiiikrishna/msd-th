package com.mysillydreams.userservice.repository;

import com.mysillydreams.userservice.domain.OutboxEventEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository interface for OutboxEventEntity operations.
 * Provides methods for transactional outbox pattern implementation.
 */
@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, UUID> {

    /**
     * Find pending events for processing
     */
    @Query("SELECT e FROM OutboxEventEntity e WHERE e.processed = false AND (e.nextRetryAt IS NULL OR e.nextRetryAt <= CURRENT_TIMESTAMP) ORDER BY e.createdAt ASC")
    List<OutboxEventEntity> findPendingEventsForProcessing();

    /**
     * Find unprocessed events ready for publishing
     */
    @Query("SELECT e FROM OutboxEventEntity e WHERE e.processed = false AND (e.nextRetryAt IS NULL OR e.nextRetryAt <= CURRENT_TIMESTAMP) ORDER BY e.createdAt ASC")
    List<OutboxEventEntity> findUnprocessedEvents();

    /**
     * Find unprocessed events with pagination
     */
    @Query(value = "SELECT * FROM outbox_events WHERE processed = false AND (next_retry_at IS NULL OR next_retry_at <= CURRENT_TIMESTAMP) ORDER BY created_at ASC", nativeQuery = true)
    Page<OutboxEventEntity> findUnprocessedEvents(Pageable pageable);

    /**
     * Find events by aggregate ID
     */
    List<OutboxEventEntity> findByAggregateIdOrderByCreatedAtDesc(UUID aggregateId);

    /**
     * Find events by aggregate type
     */
    @Query(value = "SELECT * FROM outbox_events WHERE aggregate_type = :aggregateType ORDER BY created_at DESC", nativeQuery = true)
    List<OutboxEventEntity> findByAggregateTypeOrderByCreatedAtDesc(@Param("aggregateType") String aggregateType);

    /**
     * Find events by event type
     */
    List<OutboxEventEntity> findByEventTypeOrderByCreatedAtDesc(String eventType);

    /**
     * Find processed events before a certain date
     */
    @Query("SELECT e FROM OutboxEventEntity e WHERE e.processed = true AND e.processedAt < :cutoffDate ORDER BY e.processedAt DESC")
    List<OutboxEventEntity> findProcessedEventsBefore(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Delete processed events before a certain date
     */
    @Modifying
    @Query("DELETE FROM OutboxEventEntity e WHERE e.processed = true AND e.processedAt < :cutoffDate")
    int deleteProcessedEventsBefore(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Count events by status
     */
    long countByStatus(OutboxEventEntity.EventStatus status);

    /**
     * Find processed events
     */
    List<OutboxEventEntity> findByProcessedTrueOrderByProcessedAtDesc();

    /**
     * Find failed events (max retries exceeded)
     */
    @Query("SELECT e FROM OutboxEventEntity e WHERE e.processed = false AND e.retryCount >= 10 ORDER BY e.createdAt DESC")
    List<OutboxEventEntity> findFailedEvents();

    /**
     * Find events pending retry
     */
    @Query("SELECT e FROM OutboxEventEntity e WHERE e.processed = false AND e.nextRetryAt IS NOT NULL AND e.nextRetryAt > CURRENT_TIMESTAMP ORDER BY e.nextRetryAt ASC")
    List<OutboxEventEntity> findEventsPendingRetry();

    /**
     * Find events by correlation ID
     */
    List<OutboxEventEntity> findByCorrelationIdOrderByCreatedAtDesc(String correlationId);

    /**
     * Find events by causation ID
     */
    List<OutboxEventEntity> findByCausationIdOrderByCreatedAtDesc(String causationId);

    /**
     * Find events created within a date range
     */
    @Query("SELECT e FROM OutboxEventEntity e WHERE e.createdAt >= :startDate AND e.createdAt <= :endDate ORDER BY e.createdAt DESC")
    List<OutboxEventEntity> findEventsCreatedBetween(@Param("startDate") LocalDateTime startDate,
                                                     @Param("endDate") LocalDateTime endDate);

    /**
     * Find events processed within a date range
     */
    @Query("SELECT e FROM OutboxEventEntity e WHERE e.processedAt >= :startDate AND e.processedAt <= :endDate ORDER BY e.processedAt DESC")
    List<OutboxEventEntity> findEventsProcessedBetween(@Param("startDate") LocalDateTime startDate,
                                                       @Param("endDate") LocalDateTime endDate);

    /**
     * Count unprocessed events
     */
    long countByProcessedFalse();

    /**
     * Count processed events
     */
    long countByProcessedTrue();

    /**
     * Count failed events
     */
    @Query("SELECT COUNT(e) FROM OutboxEventEntity e WHERE e.processed = false AND e.retryCount >= 10")
    long countFailedEvents();

    /**
     * Count events by aggregate type
     */
    @Query("SELECT e.aggregateType, COUNT(e) FROM OutboxEventEntity e GROUP BY e.aggregateType")
    List<Object[]> countEventsByAggregateType();

    /**
     * Count events by event type
     */
    @Query("SELECT e.eventType, COUNT(e) FROM OutboxEventEntity e GROUP BY e.eventType ORDER BY COUNT(e) DESC")
    List<Object[]> countEventsByEventType();

    /**
     * Mark event as processed
     */
    @Modifying
    @Query("UPDATE OutboxEventEntity e SET e.processed = true, e.processedAt = CURRENT_TIMESTAMP, e.lastError = null, e.nextRetryAt = null WHERE e.id = :eventId")
    void markAsProcessed(@Param("eventId") UUID eventId);

    /**
     * Mark event as failed with error
     */
    @Modifying
    @Query("UPDATE OutboxEventEntity e SET e.retryCount = e.retryCount + 1, e.lastError = :error, e.nextRetryAt = :nextRetryAt WHERE e.id = :eventId")
    void markAsFailed(@Param("eventId") UUID eventId, @Param("error") String error, @Param("nextRetryAt") LocalDateTime nextRetryAt);

    /**
     * Reset retry count for an event
     */
    @Modifying
    @Query("UPDATE OutboxEventEntity e SET e.retryCount = 0, e.lastError = null, e.nextRetryAt = null WHERE e.id = :eventId")
    void resetRetries(@Param("eventId") UUID eventId);

    /**
     * Delete old processed events (for cleanup)
     */
    @Modifying
    @Query("DELETE FROM OutboxEventEntity e WHERE e.processed = true AND e.processedAt < :deleteThreshold")
    int deleteOldProcessedEvents(@Param("deleteThreshold") LocalDateTime deleteThreshold);

    /**
     * Delete old failed events (after manual review)
     */
    @Modifying
    @Query("DELETE FROM OutboxEventEntity e WHERE e.processed = false AND e.retryCount >= 10 AND e.createdAt < :deleteThreshold")
    int deleteOldFailedEvents(@Param("deleteThreshold") LocalDateTime deleteThreshold);

    /**
     * Find events that need attention (failed or stuck)
     */
    @Query("SELECT e FROM OutboxEventEntity e WHERE " +
           "(e.processed = false AND e.retryCount >= 5) OR " +
           "(e.processed = false AND e.createdAt < :stuckThreshold) " +
           "ORDER BY e.createdAt ASC")
    List<OutboxEventEntity> findEventsNeedingAttention(@Param("stuckThreshold") LocalDateTime stuckThreshold);

    /**
     * Get outbox statistics
     */
    @Query("SELECT " +
           "COUNT(CASE WHEN e.processed = false THEN 1 END) as unprocessed, " +
           "COUNT(CASE WHEN e.processed = true THEN 1 END) as processed, " +
           "COUNT(CASE WHEN e.processed = false AND e.retryCount >= 10 THEN 1 END) as failed, " +
           "COUNT(CASE WHEN e.processed = false AND e.nextRetryAt IS NOT NULL AND e.nextRetryAt > CURRENT_TIMESTAMP THEN 1 END) as pendingRetry " +
           "FROM OutboxEventEntity e")
    Object[] getOutboxStatistics();

    /**
     * Find events by message ID (for deduplication)
     */
    List<OutboxEventEntity> findByMessageId(String messageId);

    /**
     * Check if event with message ID exists
     */
    boolean existsByMessageId(String messageId);

    /**
     * Find recent events for monitoring
     */
    @Query(value = "SELECT * FROM outbox_events WHERE created_at >= :since ORDER BY created_at DESC", nativeQuery = true)
    List<OutboxEventEntity> findRecentEvents(@Param("since") LocalDateTime since, Pageable pageable);

    /**
     * Find events with high retry count (for investigation)
     */
    @Query(value = "SELECT * FROM outbox_events WHERE processed = false AND retry_count >= :minRetryCount ORDER BY retry_count DESC, created_at ASC", nativeQuery = true)
    List<OutboxEventEntity> findEventsWithHighRetryCount(@Param("minRetryCount") int minRetryCount);

    /**
     * Get processing performance metrics
     */
    @Query(value = "SELECT " +
           "AVG(EXTRACT(EPOCH FROM (processed_at - created_at))) as avgProcessingTimeSeconds, " +
           "MIN(EXTRACT(EPOCH FROM (processed_at - created_at))) as minProcessingTimeSeconds, " +
           "MAX(EXTRACT(EPOCH FROM (processed_at - created_at))) as maxProcessingTimeSeconds " +
           "FROM outbox_events WHERE processed = true AND processed_at >= :since", nativeQuery = true)
    Object[] getProcessingMetrics(@Param("since") LocalDateTime since);
}
