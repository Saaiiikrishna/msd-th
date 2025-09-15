package com.mysillydreams.userservice.repository;

import com.mysillydreams.userservice.domain.UserExternalRefEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for UserExternalRefEntity operations.
 * Provides methods for external system reference management.
 */
@Repository
public interface UserExternalRefRepository extends JpaRepository<UserExternalRefEntity, UserExternalRefEntity.UserExternalRefId> {

    /**
     * Find all external references for a specific user
     */
    List<UserExternalRefEntity> findByUserId(UUID userId);

    /**
     * Find all active external references for a user
     */
    List<UserExternalRefEntity> findByUserIdAndActiveTrue(UUID userId);

    /**
     * Find external reference by user and system
     */
    Optional<UserExternalRefEntity> findByUserIdAndSystem(UUID userId, String system);

    /**
     * Find active external reference by user and system
     */
    Optional<UserExternalRefEntity> findByUserIdAndSystemAndActiveTrue(UUID userId, String system);

    /**
     * Find user by external system and external ID
     */
    Optional<UserExternalRefEntity> findBySystemAndExternalId(String system, String externalId);

    /**
     * Find active user by external system and external ID
     */
    Optional<UserExternalRefEntity> findBySystemAndExternalIdAndActiveTrue(String system, String externalId);

    /**
     * Find all references for a specific system
     */
    List<UserExternalRefEntity> findBySystem(String system);

    /**
     * Find all active references for a specific system
     */
    List<UserExternalRefEntity> findBySystemAndActiveTrue(String system);

    /**
     * Check if external reference exists
     */
    boolean existsBySystemAndExternalId(String system, String externalId);

    /**
     * Check if active external reference exists
     */
    boolean existsBySystemAndExternalIdAndActiveTrue(String system, String externalId);

    /**
     * Check if user has reference in a specific system
     */
    boolean existsByUserIdAndSystem(UUID userId, String system);

    /**
     * Check if user has active reference in a specific system
     */
    boolean existsByUserIdAndSystemAndActiveTrue(UUID userId, String system);

    /**
     * Count references for a user
     */
    long countByUserId(UUID userId);

    /**
     * Count active references for a user
     */
    long countByUserIdAndActiveTrue(UUID userId);

    /**
     * Count references in a system
     */
    long countBySystem(String system);

    /**
     * Count active references in a system
     */
    long countBySystemAndActiveTrue(String system);

    /**
     * Find all distinct systems
     */
    @Query(value = "SELECT DISTINCT system FROM user_external_refs WHERE active = true", nativeQuery = true)
    List<String> findAllActiveSystems();

    /**
     * Find users with references in multiple systems
     */
    @Query(value = "SELECT user_id, COUNT(DISTINCT system) as systemCount FROM user_external_refs WHERE active = true GROUP BY user_id HAVING COUNT(DISTINCT system) > 1", nativeQuery = true)
    List<Object[]> findUsersWithMultipleSystemReferences();

    /**
     * Find users without reference in a specific system
     */
    @Query(value = "SELECT u.id FROM users u WHERE u.active = true AND NOT EXISTS (SELECT 1 FROM user_external_refs e WHERE e.user_id = u.id AND e.system = :system AND e.active = true)", nativeQuery = true)
    List<UUID> findUsersWithoutSystemReference(@Param("system") String system);

    /**
     * Find references by external ID pattern (for migration/cleanup)
     */
    @Query(value = "SELECT * FROM user_external_refs WHERE external_id LIKE CONCAT('%', :pattern, '%')", nativeQuery = true)
    List<UserExternalRefEntity> findByExternalIdPattern(@Param("pattern") String pattern);

    /**
     * Find references with metadata containing specific value
     */
    @Query("SELECT e FROM UserExternalRefEntity e WHERE e.metadata LIKE %:value%")
    List<UserExternalRefEntity> findByMetadataContaining(@Param("value") String value);

    /**
     * Delete all references for a user (for user deletion)
     */
    void deleteByUserId(UUID userId);

    /**
     * Deactivate all references for a user
     */
    @Query("UPDATE UserExternalRefEntity e SET e.active = false WHERE e.user.id = :userId")
    void deactivateAllUserReferences(@Param("userId") UUID userId);

    /**
     * Count references by system
     */
    @Query("SELECT e.system, COUNT(e) FROM UserExternalRefEntity e WHERE e.active = true GROUP BY e.system")
    List<Object[]> countReferencesBySystem();

    /**
     * Find orphaned references (users that no longer exist)
     */
    @Query("SELECT e FROM UserExternalRefEntity e WHERE NOT EXISTS (SELECT u FROM UserEntity u WHERE u.id = e.user.id)")
    List<UserExternalRefEntity> findOrphanedReferences();

    /**
     * Find duplicate external IDs within a system
     */
    @Query("SELECT e.system, e.externalId, COUNT(e) FROM UserExternalRefEntity e WHERE e.active = true GROUP BY e.system, e.externalId HAVING COUNT(e) > 1")
    List<Object[]> findDuplicateExternalIds();

    /**
     * Find references created within a date range
     */
    @Query("SELECT e FROM UserExternalRefEntity e WHERE e.createdAt >= :startDate AND e.createdAt <= :endDate ORDER BY e.createdAt DESC")
    List<UserExternalRefEntity> findReferencesCreatedBetween(@Param("startDate") java.time.LocalDateTime startDate,
                                                            @Param("endDate") java.time.LocalDateTime endDate);

    // Helper methods for specific systems

    /**
     * Find user by payment system ID
     */
    default Optional<UserExternalRefEntity> findByPaymentId(String paymentId) {
        return findBySystemAndExternalIdAndActiveTrue("PAYMENTS", paymentId);
    }

    /**
     * Find user by order system ID
     */
    default Optional<UserExternalRefEntity> findByOrderId(String orderId) {
        return findBySystemAndExternalIdAndActiveTrue("ORDERS", orderId);
    }

    /**
     * Find user by notification system ID
     */
    default Optional<UserExternalRefEntity> findByNotificationId(String notificationId) {
        return findBySystemAndExternalIdAndActiveTrue("NOTIFICATIONS", notificationId);
    }

    /**
     * Find user by analytics system ID
     */
    default Optional<UserExternalRefEntity> findByAnalyticsId(String analyticsId) {
        return findBySystemAndExternalIdAndActiveTrue("ANALYTICS", analyticsId);
    }

    /**
     * Find user by support system ID
     */
    default Optional<UserExternalRefEntity> findBySupportId(String supportId) {
        return findBySystemAndExternalIdAndActiveTrue("SUPPORT", supportId);
    }
}
