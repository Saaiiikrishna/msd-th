package com.mysillydreams.userservice.repository;

import com.mysillydreams.userservice.domain.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    // Standard find methods will now respect the @Where clause implicitly

    /**
     * Finds a user by their unique business reference ID.
     *
     * @param referenceId The reference ID to search for.
     * @return An {@link Optional} containing the {@link UserEntity} if found, or empty otherwise.
     */
    Optional<UserEntity> findByReferenceId(String referenceId);

    /**
     * Find active user by business reference ID
     */
    Optional<UserEntity> findByReferenceIdAndActiveTrue(String referenceId);

    /**
     * Check if user exists by reference ID
     */
    boolean existsByReferenceId(String referenceId);

    /**
     * Check if active user exists by reference ID
     */
    boolean existsByReferenceIdAndActiveTrue(String referenceId);

    // HMAC-based search methods for encrypted PII fields

    /**
     * Find user by email HMAC (for encrypted email search)
     */
    Optional<UserEntity> findByEmailHmac(String emailHmac);

    /**
     * Find active user by email HMAC
     */
    Optional<UserEntity> findByEmailHmacAndActiveTrue(String emailHmac);

    /**
     * Find user by phone HMAC (for encrypted phone search)
     */
    Optional<UserEntity> findByPhoneHmac(String phoneHmac);

    /**
     * Find active user by phone HMAC
     */
    Optional<UserEntity> findByPhoneHmacAndActiveTrue(String phoneHmac);

    /**
     * Check if email HMAC exists (for uniqueness validation)
     */
    boolean existsByEmailHmac(String emailHmac);

    /**
     * Check if active user exists with email HMAC
     */
    boolean existsByEmailHmacAndActiveTrue(String emailHmac);

    /**
     * Check if phone HMAC exists (for uniqueness validation)
     */
    boolean existsByPhoneHmac(String phoneHmac);

    /**
     * Check if active user exists with phone HMAC
     */
    boolean existsByPhoneHmacAndActiveTrue(String phoneHmac);

    // Bulk lookup methods for internal services

    /**
     * Find users by multiple criteria (bulk lookup)
     */
    @Query(value = "SELECT * FROM users WHERE " +
           "(reference_id = ANY(:referenceIds) OR :referenceIds IS NULL) OR " +
           "(email_hmac = ANY(:emailHmacs) OR :emailHmacs IS NULL) OR " +
           "(phone_hmac = ANY(:phoneHmacs) OR :phoneHmacs IS NULL) OR " +
           "(id = ANY(:userIds) OR :userIds IS NULL)", nativeQuery = true)
    List<UserEntity> findByBulkCriteria(@Param("referenceIds") List<String> referenceIds,
                                       @Param("emailHmacs") List<String> emailHmacs,
                                       @Param("phoneHmacs") List<String> phoneHmacs,
                                       @Param("userIds") List<UUID> userIds);

    // Statistical and admin methods

    /**
     * Count total active users
     */
    long countByActiveTrue();

    /**
     * Count total archived users
     */
    long countByActiveFalse();

    /**
     * Find users created within a date range
     */
    @Query(value = "SELECT * FROM users WHERE created_at >= :startDate AND created_at <= :endDate ORDER BY created_at DESC", nativeQuery = true)
    Page<UserEntity> findUsersCreatedBetween(@Param("startDate") java.time.LocalDateTime startDate,
                                           @Param("endDate") java.time.LocalDateTime endDate,
                                           Pageable pageable);

    /**
     * Find users by partial reference ID match (for admin search)
     */
    @Query(value = "SELECT * FROM users WHERE reference_id LIKE CONCAT('%', :partialRef, '%') AND active = true ORDER BY created_at DESC", nativeQuery = true)
    Page<UserEntity> findByReferenceIdContaining(@Param("partialRef") String partialRef, Pageable pageable);

    // Additional methods for admin operations
    Page<UserEntity> findByActiveTrue(Pageable pageable);
    Page<UserEntity> findByActiveFalse(Pageable pageable);

    // GDPR/DPDP methods for data deletion and retention

    /**
     * Find users deleted before a specific date (for retention policy)
     */
    @Query(value = "SELECT * FROM users WHERE active = false AND deleted_at < :cutoffDate ORDER BY deleted_at ASC", nativeQuery = true)
    List<UserEntity> findDeletedUsersBefore(@Param("cutoffDate") java.time.LocalDateTime cutoffDate);

    /**
     * Find anonymized users
     */
    @Query(value = "SELECT * FROM users WHERE anonymized = true ORDER BY anonymized_at DESC", nativeQuery = true)
    List<UserEntity> findAnonymizedUsers();

    /**
     * Count users deleted before a specific date
     */
    @Query(value = "SELECT COUNT(*) FROM users WHERE active = false AND deleted_at < :cutoffDate", nativeQuery = true)
    long countDeletedUsersBefore(@Param("cutoffDate") java.time.LocalDateTime cutoffDate);

    /**
     * Find users by deletion reason
     */
    @Query(value = "SELECT * FROM users WHERE deletion_reason = :reason ORDER BY deleted_at DESC", nativeQuery = true)
    List<UserEntity> findByDeletionReason(@Param("reason") String reason);

    /**
     * Find active users (not archived)
     */
    Page<UserEntity> findByArchivedAtIsNull(Pageable pageable);

    /**
     * Find archived users
     */
    Page<UserEntity> findByArchivedAtIsNotNull(Pageable pageable);
}
