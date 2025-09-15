package com.mysillydreams.userservice.repository;

import com.mysillydreams.userservice.domain.UserRoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for UserRoleEntity operations.
 * Provides methods for role management and queries.
 */
@Repository
public interface UserRoleRepository extends JpaRepository<UserRoleEntity, UserRoleEntity.UserRoleId> {

    /**
     * Find all roles for a specific user
     */
    List<UserRoleEntity> findByUserId(UUID userId);

    /**
     * Find all active roles for a specific user
     */
    List<UserRoleEntity> findByUserIdAndActiveTrue(UUID userId);

    /**
     * Find all users with a specific role
     */
    List<UserRoleEntity> findByRole(String role);

    /**
     * Find all active users with a specific role
     */
    List<UserRoleEntity> findByRoleAndActiveTrue(String role);

    /**
     * Find specific user role assignment
     */
    Optional<UserRoleEntity> findByUserIdAndRole(UUID userId, String role);

    /**
     * Find active user role assignment
     */
    Optional<UserRoleEntity> findByUserIdAndRoleAndActiveTrue(UUID userId, String role);

    /**
     * Check if user has a specific role
     */
    boolean existsByUserIdAndRole(UUID userId, String role);

    /**
     * Check if user has an active specific role
     */
    boolean existsByUserIdAndRoleAndActiveTrue(UUID userId, String role);

    /**
     * Find roles assigned by a specific admin
     */
    List<UserRoleEntity> findByAssignedBy(UUID assignedBy);

    /**
     * Find roles that expire within a time period
     */
    @Query(value = "SELECT * FROM user_roles WHERE expires_at IS NOT NULL AND expires_at <= :expiryDate AND active = true", nativeQuery = true)
    List<UserRoleEntity> findExpiringRoles(@Param("expiryDate") LocalDateTime expiryDate);

    /**
     * Find expired roles that are still active
     */
    @Query(value = "SELECT * FROM user_roles WHERE expires_at IS NOT NULL AND expires_at < CURRENT_TIMESTAMP AND active = true", nativeQuery = true)
    List<UserRoleEntity> findExpiredActiveRoles();

    /**
     * Count users with a specific role
     */
    long countByRole(String role);

    /**
     * Count active users with a specific role
     */
    long countByRoleAndActiveTrue(String role);

    /**
     * Find roles assigned within a date range
     */
    @Query(value = "SELECT * FROM user_roles WHERE assigned_at >= :startDate AND assigned_at <= :endDate ORDER BY assigned_at DESC", nativeQuery = true)
    List<UserRoleEntity> findRolesAssignedBetween(@Param("startDate") LocalDateTime startDate,
                                                  @Param("endDate") LocalDateTime endDate);

    /**
     * Find all distinct roles in the system
     */
    @Query(value = "SELECT DISTINCT role FROM user_roles WHERE active = true", nativeQuery = true)
    List<String> findAllActiveRoles();

    /**
     * Delete all roles for a user (for user deletion)
     */
    void deleteByUserId(UUID userId);

    /**
     * Deactivate all roles for a user
     */
    @Query(value = "UPDATE user_roles SET active = false WHERE user_id = :userId", nativeQuery = true)
    void deactivateAllUserRoles(@Param("userId") UUID userId);

    /**
     * Find users with multiple roles (admin query)
     */
    @Query(value = "SELECT user_id, COUNT(role) as roleCount FROM user_roles WHERE active = true GROUP BY user_id HAVING COUNT(role) > 1", nativeQuery = true)
    List<Object[]> findUsersWithMultipleRoles();
}
