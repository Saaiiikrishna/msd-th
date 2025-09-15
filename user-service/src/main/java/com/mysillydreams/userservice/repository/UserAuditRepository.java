package com.mysillydreams.userservice.repository;

import com.mysillydreams.userservice.domain.UserAuditEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository interface for UserAuditEntity operations.
 * Provides methods for audit trail management and compliance queries.
 */
@Repository
public interface UserAuditRepository extends JpaRepository<UserAuditEntity, UUID> {

    /**
     * Find all audit records for a specific user
     */
    List<UserAuditEntity> findByUserId(UUID userId);

    /**
     * Find all audit records for a specific user ordered by creation date
     */
    List<UserAuditEntity> findByUserIdOrderByCreatedAtDesc(UUID userId);

    /**
     * Find audit records for a user with pagination
     */
    Page<UserAuditEntity> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    /**
     * Find audit records by event type
     */
    @Query(value = "SELECT * FROM user_audit WHERE event_type = :eventType ORDER BY created_at DESC", nativeQuery = true)
    List<UserAuditEntity> findByEventTypeOrderByCreatedAtDesc(@Param("eventType") String eventType);

    /**
     * Find audit records by event type with pagination
     */
    @Query(value = "SELECT * FROM user_audit WHERE event_type = :eventType ORDER BY created_at DESC", nativeQuery = true)
    Page<UserAuditEntity> findByEventTypeOrderByCreatedAtDesc(@Param("eventType") String eventType, Pageable pageable);

    /**
     * Find audit records for a user and event type
     */
    @Query(value = "SELECT * FROM user_audit WHERE user_id = :userId AND event_type = :eventType ORDER BY created_at DESC", nativeQuery = true)
    List<UserAuditEntity> findByUserIdAndEventTypeOrderByCreatedAtDesc(@Param("userId") UUID userId, @Param("eventType") String eventType);

    /**
     * Find audit records performed by a specific user
     */
    List<UserAuditEntity> findByPerformedByOrderByCreatedAtDesc(UUID performedBy);

    /**
     * Find audit records within a date range
     */
    @Query(value = "SELECT * FROM user_audit WHERE created_at >= :startDate AND created_at <= :endDate ORDER BY created_at DESC", nativeQuery = true)
    Page<UserAuditEntity> findAuditRecordsBetween(@Param("startDate") LocalDateTime startDate,
                                                  @Param("endDate") LocalDateTime endDate,
                                                  Pageable pageable);

    /**
     * Find audit records for a user within a date range
     */
    @Query(value = "SELECT * FROM user_audit WHERE user_id = :userId AND created_at >= :startDate AND created_at <= :endDate ORDER BY created_at DESC", nativeQuery = true)
    List<UserAuditEntity> findUserAuditRecordsBetween(@Param("userId") UUID userId,
                                                      @Param("startDate") LocalDateTime startDate,
                                                      @Param("endDate") LocalDateTime endDate);

    /**
     * Find security-related audit records
     */
    @Query(value = "SELECT * FROM user_audit WHERE event_type IN ('LOGIN_FAILED', 'SUSPICIOUS_ACTIVITY', 'ACCOUNT_LOCKED', 'SECURITY_BREACH') ORDER BY created_at DESC",
           nativeQuery = true)
    Page<UserAuditEntity> findSecurityAuditRecords(Pageable pageable);

    /**
     * Find privacy-related audit records
     */
    @Query(value = "SELECT * FROM user_audit WHERE event_type IN ('CONSENT_GRANTED', 'CONSENT_WITHDRAWN', 'DATA_EXPORT_REQUESTED', 'DATA_DELETION_REQUESTED', 'PRIVACY_SETTINGS_CHANGED', 'PII_ACCESSED', 'PII_MODIFIED') ORDER BY created_at DESC",
           nativeQuery = true)
    Page<UserAuditEntity> findPrivacyAuditRecords(Pageable pageable);

    /**
     * Find user lifecycle audit records
     */
    @Query(value = "SELECT * FROM user_audit WHERE event_type IN ('USER_CREATED', 'USER_UPDATED', 'USER_DELETED', 'USER_ARCHIVED', 'USER_REACTIVATED') ORDER BY created_at DESC",
           nativeQuery = true)
    Page<UserAuditEntity> findUserLifecycleAuditRecords(Pageable pageable);

    /**
     * Find audit records by correlation ID
     */
    List<UserAuditEntity> findByCorrelationIdOrderByCreatedAtDesc(String correlationId);

    /**
     * Find audit records by session ID
     */
    List<UserAuditEntity> findBySessionIdOrderByCreatedAtDesc(String sessionId);

    /**
     * Find audit records from a specific IP address
     */
    List<UserAuditEntity> findByIpAddressOrderByCreatedAtDesc(java.net.InetAddress ipAddress);

    /**
     * Count audit records by event type
     */
    @Query(value = "SELECT event_type, COUNT(*) FROM user_audit GROUP BY event_type",
           nativeQuery = true)
    List<Object[]> countAuditRecordsByEventType();

    /**
     * Count audit records by user
     */
    @Query(value = "SELECT user_id, COUNT(*) FROM user_audit GROUP BY user_id ORDER BY COUNT(*) DESC", nativeQuery = true)
    List<Object[]> countAuditRecordsByUser();

    /**
     * Find most active users (by audit record count)
     */
    @Query(value = "SELECT user_id, COUNT(*) as auditCount FROM user_audit WHERE created_at >= :since GROUP BY user_id ORDER BY COUNT(*) DESC", nativeQuery = true)
    List<Object[]> findMostActiveUsers(@Param("since") LocalDateTime since, Pageable pageable);

    /**
     * Find users with failed login attempts
     */
    @Query(value = "SELECT DISTINCT user_id FROM user_audit WHERE event_type = 'LOGIN_FAILED' AND created_at >= :since",
           nativeQuery = true)
    List<UUID> findUsersWithFailedLogins(@Param("since") LocalDateTime since);

    /**
     * Count failed login attempts for a user
     */
    @Query(value = "SELECT COUNT(*) FROM user_audit WHERE user_id = :userId AND event_type = 'LOGIN_FAILED' AND created_at >= :since",
           nativeQuery = true)
    long countFailedLoginAttempts(@Param("userId") UUID userId, @Param("since") LocalDateTime since);

    /**
     * Find suspicious activity records
     */
    @Query(value = "SELECT * FROM user_audit WHERE event_type = 'SUSPICIOUS_ACTIVITY' AND created_at >= :since ORDER BY created_at DESC",
           nativeQuery = true)
    List<UserAuditEntity> findSuspiciousActivity(@Param("since") LocalDateTime since);

    /**
     * Find data access audit records for a user
     */
    @Query(value = "SELECT * FROM user_audit WHERE user_id = :userId AND event_type IN ('PII_ACCESSED', 'DATA_EXPORT_REQUESTED') ORDER BY created_at DESC",
           nativeQuery = true)
    List<UserAuditEntity> findDataAccessRecords(@Param("userId") UUID userId);

    /**
     * Find audit records that need to be archived (old records)
     */
    @Query(value = "SELECT * FROM user_audit WHERE created_at < :archiveDate", nativeQuery = true)
    List<UserAuditEntity> findRecordsForArchival(@Param("archiveDate") LocalDateTime archiveDate);

    /**
     * Delete old audit records (for data retention compliance)
     */
    @Query(value = "DELETE FROM user_audit WHERE created_at < :deleteDate AND event_type NOT IN ('USER_DELETED', 'CONSENT_WITHDRAWN')",
           nativeQuery = true)
    void deleteOldAuditRecords(@Param("deleteDate") LocalDateTime deleteDate);

    /**
     * Find recent audit activity summary
     */
    @Query(value = "SELECT DATE(created_at) as auditDate, event_type, COUNT(*) as eventCount " +
           "FROM user_audit WHERE created_at >= :since " +
           "GROUP BY DATE(created_at), event_type " +
           "ORDER BY auditDate DESC, eventCount DESC",
           nativeQuery = true)
    List<Object[]> getAuditActivitySummary(@Param("since") LocalDateTime since);

    /**
     * Find audit records with specific details
     */
    @Query(value = "SELECT * FROM user_audit WHERE details->>'key' = :value ORDER BY created_at DESC",
           nativeQuery = true)
    List<UserAuditEntity> findByDetailsContaining(@Param("value") String value);

    /**
     * Count GDPR-related audit events after a specific date
     */
    @Query("SELECT COUNT(a) FROM UserAuditEntity a WHERE a.eventType IN ('GDPR_DATA_EXPORT', 'GDPR_DELETION_REQUEST', 'GDPR_DELETION_COMPLETED') AND a.createdAt > :since")
    long countGdprEventsAfter(@Param("since") LocalDateTime since);



    /**
     * Get audit statistics for compliance reporting
     */
    @Query(value = "SELECT " +
           "COUNT(CASE WHEN event_type IN ('USER_CREATED', 'USER_UPDATED', 'USER_DELETED') THEN 1 END) as userEvents, " +
           "COUNT(CASE WHEN event_type IN ('CONSENT_GRANTED', 'CONSENT_WITHDRAWN') THEN 1 END) as consentEvents, " +
           "COUNT(CASE WHEN event_type IN ('LOGIN_SUCCESS', 'LOGIN_FAILED') THEN 1 END) as authEvents, " +
           "COUNT(CASE WHEN event_type = 'SUSPICIOUS_ACTIVITY' THEN 1 END) as securityEvents " +
           "FROM user_audit WHERE created_at >= :since",
           nativeQuery = true)
    Object[] getAuditStatistics(@Param("since") LocalDateTime since);

    // GDPR/DPDP specific methods

    /**
     * Find user-initiated audit records (for data export)
     */
    @Query(value = "SELECT * FROM user_audit WHERE user_id = :userId AND " +
           "event_type NOT IN ('PROFILE_VIEWED', 'LOGIN_ATTEMPT', 'LOGIN_SUCCESS', 'LOGIN_FAILURE') " +
           "ORDER BY created_at DESC",
           nativeQuery = true)
    List<UserAuditEntity> findUserInitiatedAuditRecords(@Param("userId") UUID userId);

    /**
     * Find audit records by user and anonymization status
     */
    @Query(value = "SELECT * FROM user_audit WHERE user_id = :userId AND anonymized = :anonymized", nativeQuery = true)
    List<UserAuditEntity> findByUserIdAndAnonymized(@Param("userId") UUID userId, @Param("anonymized") boolean anonymized);



    /**
     * Find all audit records for a user (including anonymized)
     */
    @Query(value = "SELECT * FROM user_audit WHERE user_id = :userId ORDER BY created_at DESC", nativeQuery = true)
    List<UserAuditEntity> findAllByUserId(@Param("userId") UUID userId);
}
