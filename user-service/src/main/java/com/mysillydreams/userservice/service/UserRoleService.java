package com.mysillydreams.userservice.service;

import com.mysillydreams.userservice.domain.UserAuditEntity;
import com.mysillydreams.userservice.domain.UserEntity;
import com.mysillydreams.userservice.domain.UserRoleEntity;
import com.mysillydreams.userservice.repository.UserRepository;
import com.mysillydreams.userservice.repository.UserRoleRepository;
import com.mysillydreams.userservice.security.RoleHierarchyConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing user roles and permissions.
 * Handles role assignment, removal, and validation.
 */
@Service
@Slf4j
@Transactional
public class UserRoleService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final AuditService auditService;
    private final EventPublishingService eventPublishingService;

    public UserRoleService(UserRepository userRepository,
                          UserRoleRepository userRoleRepository,
                          AuditService auditService,
                          EventPublishingService eventPublishingService) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.auditService = auditService;
        this.eventPublishingService = eventPublishingService;
    }

    /**
     * Assigns a role to a user
     */
    @PreAuthorize("hasRole('ADMIN')")
    @CacheEvict(value = {"user-lookup", "user-profile", "role-hierarchy"}, allEntries = true)
    public void assignRole(String userReferenceId, String role, LocalDateTime expiresAt) {
        log.info("Assigning role {} to user {}", role, userReferenceId);

        try {
            // Validate role
            validateRole(role);

            // Find user
            UserEntity user = findActiveUser(userReferenceId);

            // Check if role already assigned
            if (userRoleRepository.existsByUserIdAndRoleAndActiveTrue(user.getId(), role)) {
                throw new RoleServiceException("Role already assigned to user: " + role);
            }

            // Validate role assignment permissions
            validateRoleAssignmentPermissions(role);

            // Create role assignment
            UserRoleEntity userRole = new UserRoleEntity(user, role, getCurrentUserId());
            if (expiresAt != null) {
                userRole.setExpiration(expiresAt);
            }

            // Save role
            userRoleRepository.save(userRole);
            log.info("Assigned role {} to user {}", role, userReferenceId);

            // Create audit record
            Map<String, Object> details = Map.of(
                "role", role,
                "expiresAt", expiresAt != null ? expiresAt.toString() : "never"
            );
            auditService.createUserAudit(user, UserAuditEntity.AuditEventType.ROLE_ASSIGNED,
                "Role assigned to user", getCurrentUserId(), details);

            // Publish event
            eventPublishingService.publishUserRoleAssignedEvent(user, role, expiresAt);

        } catch (Exception e) {
            log.error("Failed to assign role {} to user {}: {}", role, userReferenceId, e.getMessage());
            throw new RoleServiceException("Role assignment failed: " + e.getMessage(), e);
        }
    }

    /**
     * Removes a role from a user
     */
    @PreAuthorize("hasRole('ADMIN')")
    @CacheEvict(value = {"user-lookup", "user-profile", "role-hierarchy"}, allEntries = true)
    public void removeRole(String userReferenceId, String role) {
        log.info("Removing role {} from user {}", role, userReferenceId);

        try {
            // Find user
            UserEntity user = findActiveUser(userReferenceId);

            // Find role assignment
            UserRoleEntity userRole = userRoleRepository.findByUserIdAndRoleAndActiveTrue(user.getId(), role)
                .orElseThrow(() -> new RoleServiceException("Role not assigned to user: " + role));

            // Validate role removal permissions
            validateRoleRemovalPermissions(user, role);

            // Deactivate role
            userRole.deactivate();
            userRoleRepository.save(userRole);
            log.info("Removed role {} from user {}", role, userReferenceId);

            // Create audit record
            Map<String, Object> details = Map.of("role", role);
            auditService.createUserAudit(user, UserAuditEntity.AuditEventType.ROLE_REMOVED,
                "Role removed from user", getCurrentUserId(), details);

            // Publish event
            eventPublishingService.publishUserRoleRemovedEvent(user, role);

        } catch (Exception e) {
            log.error("Failed to remove role {} from user {}: {}", role, userReferenceId, e.getMessage());
            throw new RoleServiceException("Role removal failed: " + e.getMessage(), e);
        }
    }

    /**
     * Gets all roles for a user
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT_USER') or @securityUtils.isOwner(#userReferenceId)")
    @Cacheable(value = "user-roles", key = "#userReferenceId")
    @Transactional(readOnly = true)
    public Set<String> getUserRoles(String userReferenceId) {
        log.debug("Getting roles for user: {}", userReferenceId);

        UserEntity user = findActiveUser(userReferenceId);
        List<UserRoleEntity> userRoles = userRoleRepository.findByUserIdAndActiveTrue(user.getId());

        return userRoles.stream()
            .filter(UserRoleEntity::isActive)
            .map(UserRoleEntity::getRole)
            .collect(Collectors.toSet());
    }

    /**
     * Checks if user has a specific role
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT_USER', 'INTERNAL_CONSUMER')")
    @Cacheable(value = "user-role-check", key = "#userReferenceId + ':' + #role")
    @Transactional(readOnly = true)
    public boolean hasRole(String userReferenceId, String role) {
        log.debug("Checking if user {} has role {}", userReferenceId, role);

        try {
            UserEntity user = findActiveUser(userReferenceId);
            return userRoleRepository.existsByUserIdAndRoleAndActiveTrue(user.getId(), role);
        } catch (Exception e) {
            log.debug("Role check failed for user {} and role {}: {}", userReferenceId, role, e.getMessage());
            return false;
        }
    }

    /**
     * Gets all users with a specific role
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT_USER')")
    @Transactional(readOnly = true)
    public List<String> getUsersWithRole(String role) {
        log.debug("Getting users with role: {}", role);

        List<UserRoleEntity> userRoles = userRoleRepository.findByRoleAndActiveTrue(role);
        return userRoles.stream()
            .filter(UserRoleEntity::isActive)
            .map(ur -> ur.getUser().getReferenceId())
            .collect(Collectors.toList());
    }

    /**
     * Gets role statistics
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT_USER')")
    @Transactional(readOnly = true)
    public Map<String, Long> getRoleStatistics() {
        log.debug("Getting role statistics");

        Map<String, Long> statistics = new HashMap<>();
        
        // Get all active roles
        List<String> allRoles = userRoleRepository.findAllActiveRoles();
        
        for (String role : allRoles) {
            long count = userRoleRepository.countByRoleAndActiveTrue(role);
            statistics.put(role, count);
        }
        
        return statistics;
    }

    /**
     * Finds and deactivates expired roles
     */
    @PreAuthorize("hasRole('ADMIN')")
    public int deactivateExpiredRoles() {
        log.info("Deactivating expired roles");

        List<UserRoleEntity> expiredRoles = userRoleRepository.findExpiredActiveRoles();
        int deactivatedCount = 0;

        for (UserRoleEntity expiredRole : expiredRoles) {
            try {
                expiredRole.deactivate();
                userRoleRepository.save(expiredRole);

                // Create audit record
                Map<String, Object> details = Map.of(
                    "role", expiredRole.getRole(),
                    "reason", "Role expired"
                );
                auditService.createUserAudit(expiredRole.getUser(), UserAuditEntity.AuditEventType.ROLE_REMOVED,
                    "Role expired and deactivated", null, details);

                deactivatedCount++;
            } catch (Exception e) {
                log.error("Failed to deactivate expired role {} for user {}: {}", 
                    expiredRole.getRole(), expiredRole.getUser().getReferenceId(), e.getMessage());
            }
        }

        log.info("Deactivated {} expired roles", deactivatedCount);
        return deactivatedCount;
    }

    /**
     * Bulk assigns roles to multiple users
     */
    @PreAuthorize("hasRole('ADMIN')")
    @CacheEvict(value = {"user-lookup", "user-profile", "role-hierarchy"}, allEntries = true)
    public void bulkAssignRole(List<String> userReferenceIds, String role, LocalDateTime expiresAt) {
        log.info("Bulk assigning role {} to {} users", role, userReferenceIds.size());

        validateRole(role);
        validateRoleAssignmentPermissions(role);

        int successCount = 0;
        int failureCount = 0;

        for (String userReferenceId : userReferenceIds) {
            try {
                assignRole(userReferenceId, role, expiresAt);
                successCount++;
            } catch (Exception e) {
                log.warn("Failed to assign role {} to user {}: {}", role, userReferenceId, e.getMessage());
                failureCount++;
            }
        }

        log.info("Bulk role assignment completed: {} successful, {} failed", successCount, failureCount);
    }

    // Helper methods

    private UserEntity findActiveUser(String userReferenceId) {
        return userRepository.findByReferenceIdAndActiveTrue(userReferenceId)
            .orElseThrow(() -> new RoleServiceException("User not found: " + userReferenceId));
    }

    private void validateRole(String role) {
        if (role == null || role.trim().isEmpty()) {
            throw new RoleServiceException("Role cannot be null or empty");
        }

        if (!role.startsWith("ROLE_")) {
            throw new RoleServiceException("Role must start with 'ROLE_' prefix");
        }

        // Validate against known roles
        try {
            // Check if it's a valid role from our hierarchy
            boolean isValidRole = Arrays.stream(RoleHierarchyConfig.Roles.class.getDeclaredFields())
                .anyMatch(field -> {
                    try {
                        return role.equals(field.get(null));
                    } catch (IllegalAccessException e) {
                        return false;
                    }
                });

            if (!isValidRole) {
                log.warn("Unknown role being assigned: {}", role);
            }
        } catch (Exception e) {
            log.debug("Role validation warning: {}", e.getMessage());
        }
    }

    private void validateRoleAssignmentPermissions(String role) {
        // Only admins can assign admin roles
        if (RoleHierarchyConfig.Roles.ADMIN.equals(role)) {
            if (!RoleHierarchyConfig.SecurityUtils.isAdmin()) {
                throw new RoleServiceException("Only admins can assign admin roles");
            }
        }
    }

    private void validateRoleRemovalPermissions(UserEntity user, String role) {
        // Prevent removing admin role if user is the last admin
        if (RoleHierarchyConfig.Roles.ADMIN.equals(role)) {
            long adminCount = userRoleRepository.countByRoleAndActiveTrue(RoleHierarchyConfig.Roles.ADMIN);
            if (adminCount <= 1) {
                throw new RoleServiceException("Cannot remove admin role from the last admin user");
            }
        }

        // Prevent users from removing their own admin role
        UUID currentUserId = getCurrentUserId();
        if (currentUserId != null && currentUserId.equals(user.getId()) && 
            RoleHierarchyConfig.Roles.ADMIN.equals(role)) {
            throw new RoleServiceException("Cannot remove your own admin role");
        }
    }

    private UUID getCurrentUserId() {
        return RoleHierarchyConfig.SecurityUtils.getCurrentUserId();
    }

    // Exception class
    public static class RoleServiceException extends RuntimeException {
        public RoleServiceException(String message) {
            super(message);
        }

        public RoleServiceException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
