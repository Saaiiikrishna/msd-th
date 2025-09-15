package com.mysillydreams.userservice.repository;

import com.mysillydreams.userservice.domain.AddressEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for AddressEntity operations.
 * Provides methods for address management and queries.
 */
@Repository
public interface AddressRepository extends JpaRepository<AddressEntity, UUID> {

    /**
     * Find all addresses for a specific user
     */
    List<AddressEntity> findByUserId(UUID userId);

    /**
     * Find all addresses for a user ordered by creation date
     */
    List<AddressEntity> findByUserIdOrderByCreatedAtDesc(UUID userId);

    /**
     * Find addresses by user and type
     */
    List<AddressEntity> findByUserIdAndType(UUID userId, AddressEntity.AddressType type);

    /**
     * Find primary address for a user
     */
    Optional<AddressEntity> findByUserIdAndIsPrimaryTrue(UUID userId);

    /**
     * Find all primary addresses (should be one per user)
     */
    List<AddressEntity> findByIsPrimaryTrue();

    /**
     * Check if user has any addresses
     */
    boolean existsByUserId(UUID userId);

    /**
     * Check if user has a primary address
     */
    boolean existsByUserIdAndIsPrimaryTrue(UUID userId);

    /**
     * Count addresses for a user
     */
    long countByUserId(UUID userId);

    /**
     * Count addresses by type for a user
     */
    long countByUserIdAndType(UUID userId, AddressEntity.AddressType type);

    /**
     * Find addresses by type across all users
     */
    List<AddressEntity> findByType(AddressEntity.AddressType type);

    /**
     * Delete all addresses for a user
     */
    void deleteByUserId(UUID userId);

    /**
     * Set all addresses as non-primary for a user (used before setting a new primary)
     */
    @Modifying
    @Query(value = "UPDATE addresses SET is_primary = false WHERE user_id = :userId", nativeQuery = true)
    void clearPrimaryAddressForUser(@Param("userId") UUID userId);

    /**
     * Set a specific address as primary and clear others
     */
    @Modifying
    @Query(value = "UPDATE addresses SET is_primary = CASE WHEN id = :addressId THEN true ELSE false END WHERE user_id = :userId", nativeQuery = true)
    void setPrimaryAddress(@Param("userId") UUID userId, @Param("addressId") UUID addressId);

    /**
     * Find users with multiple primary addresses (data integrity check)
     */
    @Query(value = "SELECT user_id FROM addresses WHERE is_primary = true GROUP BY user_id HAVING COUNT(id) > 1", nativeQuery = true)
    List<UUID> findUsersWithMultiplePrimaryAddresses();

    /**
     * Find users without any primary address
     */
    @Query(value = "SELECT DISTINCT u.id FROM users u LEFT JOIN addresses a ON u.id = a.user_id AND a.is_primary = true WHERE a.id IS NULL AND EXISTS (SELECT 1 FROM addresses a2 WHERE a2.user_id = u.id)", nativeQuery = true)
    List<UUID> findUsersWithoutPrimaryAddress();

    /**
     * Find addresses created within a date range
     */
    @Query(value = "SELECT * FROM addresses WHERE created_at >= :startDate AND created_at <= :endDate ORDER BY created_at DESC", nativeQuery = true)
    List<AddressEntity> findAddressesCreatedBetween(@Param("startDate") java.time.LocalDateTime startDate,
                                                   @Param("endDate") java.time.LocalDateTime endDate);

    /**
     * Count addresses by type
     */
    @Query(value = "SELECT type, COUNT(*) FROM addresses GROUP BY type", nativeQuery = true)
    List<Object[]> countAddressesByType();

    /**
     * Find addresses that need validation (missing required fields)
     */
    @Query(value = "SELECT * FROM addresses WHERE line1_enc IS NULL OR city_enc IS NULL OR country_enc IS NULL", nativeQuery = true)
    List<AddressEntity> findIncompleteAddresses();
}
