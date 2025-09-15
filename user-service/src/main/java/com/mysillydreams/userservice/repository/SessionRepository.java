package com.mysillydreams.userservice.repository;

import com.mysillydreams.userservice.domain.SessionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for SessionEntity operations.
 * Provides methods for session metadata management (read-only).
 */
@Repository
public interface SessionRepository extends JpaRepository<SessionEntity, UUID> {

    /**
     * Find session by token hash
     */
    Optional<SessionEntity> findBySessionTokenHash(String sessionTokenHash);

    /**
     * Find all sessions for a specific user
     */
    List<SessionEntity> findByUserIdOrderByCreatedAtDesc(UUID userId);

    /**
     * Find all active sessions for a user
     */
    List<SessionEntity> findByUserIdAndActiveTrueOrderByLastAccessedAtDesc(UUID userId);

    /**
     * Find sessions for a user with pagination
     */
    Page<SessionEntity> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    /**
     * Find active sessions for a user with pagination
     */
    Page<SessionEntity> findByUserIdAndActiveTrueOrderByLastAccessedAtDesc(UUID userId, Pageable pageable);

    /**
     * Find all active sessions
     */
    List<SessionEntity> findByActiveTrueOrderByLastAccessedAtDesc();

    /**
     * Find expired sessions that are still marked as active
     */
    @Query("SELECT s FROM SessionEntity s WHERE s.active = true AND s.expiresAt < CURRENT_TIMESTAMP")
    List<SessionEntity> findExpiredActiveSessions();

    /**
     * Find sessions that haven't been accessed recently
     */
    @Query("SELECT s FROM SessionEntity s WHERE s.active = true AND s.lastAccessedAt < :inactiveThreshold")
    List<SessionEntity> findInactiveSessions(@Param("inactiveThreshold") LocalDateTime inactiveThreshold);

    /**
     * Find sessions from a specific IP address
     */
    List<SessionEntity> findByIpAddressOrderByCreatedAtDesc(java.net.InetAddress ipAddress);

    /**
     * Find sessions created within a date range
     */
    @Query("SELECT s FROM SessionEntity s WHERE s.createdAt >= :startDate AND s.createdAt <= :endDate ORDER BY s.createdAt DESC")
    List<SessionEntity> findSessionsCreatedBetween(@Param("startDate") LocalDateTime startDate,
                                                   @Param("endDate") LocalDateTime endDate);

    /**
     * Count active sessions for a user
     */
    long countByUserIdAndActiveTrue(UUID userId);

    /**
     * Count total active sessions
     */
    long countByActiveTrue();

    /**
     * Check if user has any active sessions
     */
    boolean existsByUserIdAndActiveTrue(UUID userId);

    /**
     * Terminate all active sessions for a user
     */
    @Modifying
    @Query("UPDATE SessionEntity s SET s.active = false, s.terminatedAt = CURRENT_TIMESTAMP, s.terminationReason = :reason WHERE s.user.id = :userId AND s.active = true")
    void terminateAllUserSessions(@Param("userId") UUID userId, @Param("reason") SessionEntity.TerminationReason reason);

    /**
     * Terminate expired sessions
     */
    @Modifying
    @Query("UPDATE SessionEntity s SET s.active = false, s.terminatedAt = CURRENT_TIMESTAMP, s.terminationReason = 'EXPIRED' WHERE s.active = true AND s.expiresAt < CURRENT_TIMESTAMP")
    int terminateExpiredSessions();

    /**
     * Update last accessed time for a session
     */
    @Modifying
    @Query("UPDATE SessionEntity s SET s.lastAccessedAt = CURRENT_TIMESTAMP WHERE s.sessionTokenHash = :tokenHash AND s.active = true")
    void updateLastAccessedTime(@Param("tokenHash") String tokenHash);

    /**
     * Find concurrent sessions for a user (more than allowed limit)
     */
    @Query("SELECT s FROM SessionEntity s WHERE s.user.id = :userId AND s.active = true ORDER BY s.lastAccessedAt DESC")
    List<SessionEntity> findConcurrentSessions(@Param("userId") UUID userId);

    /**
     * Delete old terminated sessions
     */
    @Modifying
    @Query("DELETE FROM SessionEntity s WHERE s.active = false AND s.terminatedAt < :deleteThreshold")
    int deleteOldSessions(@Param("deleteThreshold") LocalDateTime deleteThreshold);

    /**
     * Get session statistics
     */
    @Query("SELECT " +
           "COUNT(CASE WHEN s.active = true THEN 1 END) as activeSessions, " +
           "COUNT(CASE WHEN s.active = false THEN 1 END) as terminatedSessions, " +
           "COUNT(CASE WHEN s.active = true AND s.expiresAt < CURRENT_TIMESTAMP THEN 1 END) as expiredSessions " +
           "FROM SessionEntity s")
    Object[] getSessionStatistics();
}
