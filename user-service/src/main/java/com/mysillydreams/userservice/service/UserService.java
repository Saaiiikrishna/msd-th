package com.mysillydreams.userservice.service;

import com.mysillydreams.userservice.domain.*;
import com.mysillydreams.userservice.dto.UserDto;
import com.mysillydreams.userservice.encryption.PiiMapper;
import com.mysillydreams.userservice.mapper.UserMapper;
import com.mysillydreams.userservice.repository.*;
import com.mysillydreams.userservice.security.RoleHierarchyConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Core service for user management operations.
 * Handles CRUD operations, role management, and business logic.
 */
@Service
@Slf4j
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final UserAuditRepository userAuditRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final UserMapper userMapper;
    private final PiiMapper piiMapper;
    private final UserLookupService userLookupService;
    private final EventPublishingService eventPublishingService;
    private final AuditService auditService;

    public UserService(UserRepository userRepository,
                      UserRoleRepository userRoleRepository,
                      UserAuditRepository userAuditRepository,
                      OutboxEventRepository outboxEventRepository,
                      UserMapper userMapper,
                      PiiMapper piiMapper,
                      UserLookupService userLookupService,
                      EventPublishingService eventPublishingService,
                      AuditService auditService) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.userAuditRepository = userAuditRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.userMapper = userMapper;
        this.piiMapper = piiMapper;
        this.userLookupService = userLookupService;
        this.eventPublishingService = eventPublishingService;
        this.auditService = auditService;
    }

    /**
     * Creates a new user with validation and event publishing
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT_USER') or authentication.principal instanceof T(com.mysillydreams.userservice.security.InternalApiKeyFilter.ServicePrincipal)")
    public UserDto createUser(UserDto userDto) {
        long startTime = System.currentTimeMillis();
        String email = userDto.getEmail();
        String maskedEmail = maskEmail(email);

        log.info("📝 === STARTING USER CREATION IN USER SERVICE ===");
        log.info("📝 Email: {}", maskedEmail);
        log.info("📝 Name: {} {}", userDto.getFirstName(), userDto.getLastName());
        log.info("📝 Provided Reference ID: {}", userDto.getReferenceId());

        try {
            // Step 1: Validate input
            log.info("🔍 Step 1: Validating user input...");
            validateUserForCreation(userDto);
            log.info("✅ Input validation passed");

            // Step 2: Check for duplicates using HMAC
            log.info("🔍 Step 2: Checking for duplicate users...");
            validateUserUniqueness(userDto);
            log.info("✅ Uniqueness validation passed");

            // Step 3: Handle reference ID
            log.info("🔍 Step 3: Processing user reference ID...");
            String referenceId = userDto.getReferenceId() != null ?
                    validateAndUseProvidedReferenceId(userDto.getReferenceId()) : generateUserReferenceId();
            log.info("✅ User Reference ID: {}", referenceId);

            // Step 4: Create entity and encrypt PII
            log.info("🔍 Step 4: Creating user entity and encrypting PII...");
            UserEntity userEntity = userMapper.toEntity(userDto);
            userEntity.setReferenceId(referenceId);
            log.info("✅ User entity created and PII encrypted");

            // Step 5: Set default role if none provided
            log.info("🔍 Step 5: Setting user roles...");
            if (userEntity.getRoles().isEmpty()) {
                UserRoleEntity customerRole = new UserRoleEntity(userEntity, RoleHierarchyConfig.Roles.CUSTOMER);
                userEntity.addRole(customerRole);
                log.info("✅ Default CUSTOMER role assigned");
            } else {
                log.info("✅ Using provided roles: {}", userEntity.getRoles().stream()
                    .map(role -> role.getRole()).collect(java.util.stream.Collectors.toList()));
            }

            // Step 6: Save user to database
            log.info("💾 Step 6: Saving user to database...");
            UserEntity savedUser = userRepository.save(userEntity);
            log.info("✅ User saved to database with ID: {}", savedUser.getId());

            // Step 7: Create audit record
            log.info("📋 Step 7: Creating audit record...");
            auditService.createUserAudit(savedUser, UserAuditEntity.AuditEventType.USER_CREATED,
                "User account created", getCurrentUserId());
            log.info("✅ Audit record created");

            // Step 8: Publish event
            log.info("📡 Step 8: Publishing user created event...");
            eventPublishingService.publishUserCreatedEvent(savedUser);
            log.info("✅ User created event published");

            // Step 9: Return DTO
            UserDto result = userMapper.toDto(savedUser);
            long processingTime = System.currentTimeMillis() - startTime;

            log.info("🎉 === USER CREATION COMPLETED SUCCESSFULLY ===");
            log.info("🎉 User Reference ID: {}", referenceId);
            log.info("🎉 Email: {}", maskedEmail);
            log.info("🎉 Processing Time: {}ms", processingTime);

            return result;

        } catch (UserValidationException e) {
            long processingTime = System.currentTimeMillis() - startTime;
            log.error("❌ User validation failed for {}: {} ({}ms)", maskedEmail, e.getMessage(), processingTime);
            throw e; // Re-throw validation exceptions as-is
        } catch (UserServiceException e) {
            long processingTime = System.currentTimeMillis() - startTime;
            log.error("❌ User service error for {}: {} ({}ms)", maskedEmail, e.getMessage(), processingTime);
            throw e; // Re-throw service exceptions as-is
        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;
            log.error("❌ Unexpected error during user creation for {}: {} ({}ms)", maskedEmail, e.getMessage(), processingTime, e);
            throw new UserServiceException("User creation failed due to unexpected error: " + e.getMessage(), e);
        }
    }

    /**
     * Updates an existing user with validation and event publishing
     */
    @PreAuthorize("hasRole('ADMIN') or (authentication.principal instanceof T(com.mysillydreams.userservice.security.SessionAuthenticationFilter.UserPrincipal) and authentication.principal.userReferenceId == #referenceId)")
    @Caching(evict = {
        // TODO: This is inefficient. A better approach would be to evict only the specific user's lookup keys (email/phone) if they change.
        @CacheEvict(value = "user-lookup", allEntries = true),
        @CacheEvict(value = "user-profile", key = "#referenceId")
    })
    public UserDto updateUser(String referenceId, UserDto userDto) {
        log.info("Updating user: {}", referenceId);

        try {
            // Find existing user
            UserEntity existingUser = findUserByReferenceId(referenceId);

            // Validate update permissions
            validateUserUpdatePermissions(existingUser, userDto);

            // Check for email/phone conflicts if changed
            validateUserUniquenessForUpdate(existingUser, userDto);

            // Store original values for audit
            Map<String, Object> changes = captureUserChanges(existingUser, userDto);

            // Update entity
            userMapper.updateEntityFromDto(userDto, existingUser);

            // Save changes
            UserEntity updatedUser = userRepository.save(existingUser);
            log.info("Updated user: {}", referenceId);

            // Create audit record
            auditService.createUserAudit(updatedUser, UserAuditEntity.AuditEventType.USER_UPDATED, 
                "User profile updated", getCurrentUserId(), changes);

            // Publish event
            eventPublishingService.publishUserUpdatedEvent(updatedUser, changes);

            return userMapper.toDto(updatedUser);

        } catch (Exception e) {
            log.error("Failed to update user {}: {}", referenceId, e.getMessage());
            throw new UserServiceException("User update failed: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves user by reference ID with role-based data masking
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT_USER') or @securityUtils.isOwner(#referenceId)")
    @Cacheable(value = "user-profile", key = "#referenceId")
    @Transactional(readOnly = true)
    public UserDto getUserByReferenceId(String referenceId) {
        log.debug("Retrieving user: {}", referenceId);

        UserEntity userEntity = findUserByReferenceId(referenceId);

        // Create audit record for PII access
        auditService.createUserAudit(userEntity, UserAuditEntity.AuditEventType.PII_ACCESSED, 
            "User profile accessed", getCurrentUserId());

        // Return appropriate DTO based on user role
        if (RoleHierarchyConfig.SecurityUtils.isAdmin()) {
            return userMapper.toDtoFull(userEntity);
        } else {
            return userMapper.toDto(userEntity);
        }
    }

    /**
     * Soft deletes a user (archives the account)
     */
    @PreAuthorize("hasRole('ADMIN') or @securityUtils.isOwner(#referenceId)")
    @CacheEvict(value = {"user-lookup", "user-profile"}, allEntries = true)
    public void deleteUser(String referenceId, String reason) {
        log.info("Deleting user: {} with reason: {}", referenceId, reason);

        try {
            UserEntity userEntity = findUserByReferenceId(referenceId);

            // Validate deletion permissions
            validateUserDeletionPermissions(userEntity);

            // Soft delete the user
            userEntity.markAsDeleted();

            // Deactivate all roles
            userRoleRepository.deactivateAllUserRoles(userEntity.getId());

            // Save changes
            userRepository.save(userEntity);
            log.info("Deleted user: {}", referenceId);

            // Create audit record
            Map<String, Object> details = Map.of("reason", reason != null ? reason : "User requested deletion");
            auditService.createUserAudit(userEntity, UserAuditEntity.AuditEventType.USER_DELETED, 
                "User account deleted", getCurrentUserId(), details);

            // Publish event
            eventPublishingService.publishUserDeletedEvent(userEntity, reason);

        } catch (Exception e) {
            log.error("Failed to delete user {}: {}", referenceId, e.getMessage());
            throw new UserServiceException("User deletion failed: " + e.getMessage(), e);
        }
    }

    /**
     * Reactivates a soft-deleted user
     */
    @PreAuthorize("hasRole('ADMIN')")
    @CacheEvict(value = {"user-lookup", "user-profile"}, allEntries = true)
    public UserDto reactivateUser(String referenceId) {
        log.info("Reactivating user: {}", referenceId);

        try {
            UserEntity userEntity = userRepository.findByReferenceId(referenceId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + referenceId));

            if (userEntity.isActive()) {
                throw new UserServiceException("User is already active: " + referenceId);
            }

            // Reactivate user
            userEntity.reactivate();

            // Restore default role
            UserRoleEntity customerRole = new UserRoleEntity(userEntity, RoleHierarchyConfig.Roles.CUSTOMER);
            userEntity.addRole(customerRole);

            // Save changes
            UserEntity reactivatedUser = userRepository.save(userEntity);
            log.info("Reactivated user: {}", referenceId);

            // Create audit record
            auditService.createUserAudit(reactivatedUser, UserAuditEntity.AuditEventType.USER_REACTIVATED, 
                "User account reactivated", getCurrentUserId());

            // Publish event
            eventPublishingService.publishUserReactivatedEvent(reactivatedUser);

            return userMapper.toDto(reactivatedUser);

        } catch (Exception e) {
            log.error("Failed to reactivate user {}: {}", referenceId, e.getMessage());
            throw new UserServiceException("User reactivation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Lists users with pagination and filtering
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT_USER')")
    @Transactional(readOnly = true)
    public Page<UserDto> listUsers(Pageable pageable, boolean includeInactive) {
        log.debug("Listing users with pagination: {}, includeInactive: {}", pageable, includeInactive);

        Page<UserEntity> userPage;
        if (includeInactive) {
            userPage = userRepository.findAll(pageable);
        } else {
            userPage = userRepository.findByActiveTrue(pageable);
        }

        return userPage.map(userMapper::toDto);
    }

    /**
     * Searches users by partial reference ID
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT_USER')")
    @Transactional(readOnly = true)
    public Page<UserDto> searchUsers(String partialReferenceId, Pageable pageable) {
        log.debug("Searching users by partial reference ID: {}", partialReferenceId);

        Page<UserEntity> userPage = userRepository.findByReferenceIdContaining(partialReferenceId, pageable);
        return userPage.map(userMapper::toDto);
    }

    // Helper methods

    private UserEntity findUserByReferenceId(String referenceId) {
        return userRepository.findByReferenceIdAndActiveTrue(referenceId)
            .orElseThrow(() -> new UserNotFoundException("User not found: " + referenceId));
    }

    private void validateUserForCreation(UserDto userDto) {
        if (userDto.getEmail() == null || userDto.getEmail().trim().isEmpty()) {
            throw new UserValidationException("Email is required");
        }

        if (!piiMapper.isValidEmail(userDto.getEmail())) {
            throw new UserValidationException("Invalid email format");
        }

        if (userDto.getPhone() != null && !piiMapper.isValidPhone(userDto.getPhone())) {
            throw new UserValidationException("Invalid phone format");
        }
    }

    private void validateUserUniqueness(UserDto userDto) {
        if (userLookupService.isEmailRegistered(userDto.getEmail())) {
            throw new UserValidationException("Email already registered");
        }

        if (userDto.getPhone() != null && userLookupService.isPhoneRegistered(userDto.getPhone())) {
            throw new UserValidationException("Phone number already registered");
        }
    }

    private void validateUserUniquenessForUpdate(UserEntity existingUser, UserDto userDto) {
        // Check email uniqueness if changed
        if (userDto.getEmail() != null) {
            String currentEmail = piiMapper.decryptPii(existingUser.getEmailEnc());
            if (!userDto.getEmail().equals(currentEmail) && userLookupService.isEmailRegistered(userDto.getEmail())) {
                throw new UserValidationException("Email already registered");
            }
        }

        // Check phone uniqueness if changed
        if (userDto.getPhone() != null) {
            String currentPhone = piiMapper.decryptPii(existingUser.getPhoneEnc());
            if (!userDto.getPhone().equals(currentPhone) && userLookupService.isPhoneRegistered(userDto.getPhone())) {
                throw new UserValidationException("Phone number already registered");
            }
        }
    }

    private void validateUserUpdatePermissions(UserEntity existingUser, UserDto userDto) {
        // Additional business rules can be added here
        if (!existingUser.isActive()) {
            throw new UserServiceException("Cannot update inactive user");
        }
    }

    private void validateUserDeletionPermissions(UserEntity userEntity) {
        if (!userEntity.isActive()) {
            throw new UserServiceException("User is already deleted");
        }

        // Check if user has admin role and prevent deletion if they're the last admin
        if (userEntity.hasRole(RoleHierarchyConfig.Roles.ADMIN)) {
            long adminCount = userRoleRepository.countByRoleAndActiveTrue(RoleHierarchyConfig.Roles.ADMIN);
            if (adminCount <= 1) {
                throw new UserServiceException("Cannot delete the last admin user");
            }
        }
    }

    private String generateUserReferenceId() {
        // Generate UUIDv7 as specified in architecture document
        // UUIDv7 provides time-ordered UUIDs with better database performance
        // Note: Using UUID.randomUUID() for now, can be replaced with proper UUIDv7 implementation
        return java.util.UUID.randomUUID().toString();
    }

    private String validateAndUseProvidedReferenceId(String providedReferenceId) {
        // Validate UUID format
        if (!providedReferenceId.matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")) {
            throw new UserValidationException("Invalid reference ID format. Must be UUID format.");
        }

        // Check if reference ID already exists
        if (userRepository.existsByReferenceId(providedReferenceId)) {
            throw new UserValidationException("Reference ID already exists: " + providedReferenceId);
        }

        return providedReferenceId;
    }

    private UUID getCurrentUserId() {
        return RoleHierarchyConfig.SecurityUtils.getCurrentUserId();
    }

    private Map<String, Object> captureUserChanges(UserEntity existingUser, UserDto userDto) {
        Map<String, Object> changes = new HashMap<>();

        // Capture field changes for audit
        if (userDto.getFirstName() != null) {
            changes.put("firstName", "changed");
        }
        if (userDto.getLastName() != null) {
            changes.put("lastName", "changed");
        }
        if (userDto.getEmail() != null) {
            changes.put("email", "changed");
        }
        if (userDto.getPhone() != null) {
            changes.put("phone", "changed");
        }

        return changes;
    }

    // Admin methods for listing users
    @PreAuthorize("hasRole('ADMIN')")
    public Page<UserDto> listAllUsersIncludingArchived(Pageable pageable) {
        log.debug("Listing all users including archived with pagination: {}", pageable);
        Page<UserEntity> users = userRepository.findAll(pageable);
        return users.map(userMapper::toDto);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public Page<UserDto> listActiveUsers(Pageable pageable) {
        log.debug("Listing active users with pagination: {}", pageable);
        Page<UserEntity> users = userRepository.findByArchivedAtIsNull(pageable);
        return users.map(userMapper::toDto);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public Page<UserDto> listArchivedUsers(Pageable pageable) {
        log.debug("Listing archived users with pagination: {}", pageable);
        Page<UserEntity> users = userRepository.findByArchivedAtIsNotNull(pageable);
        return users.map(userMapper::toDto);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public UserDto getUserByIdIncludingArchived(UUID userId) {
        log.debug("Getting user by ID including archived: {}", userId);
        UserEntity user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));
        return userMapper.toDto(user);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public boolean userExistsIncludingArchived(UUID userId) {
        log.debug("Checking if user exists including archived: {}", userId);
        return userRepository.existsById(userId);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public void softDeleteUserByReferenceId(String referenceId) {
        log.info("Soft deleting user by reference ID: {}", referenceId);
        UserEntity user = getUserEntityByReferenceId(referenceId);
        user.setArchivedAt(LocalDateTime.now());
        userRepository.save(user);

        // Audit the soft deletion
        auditUserAction(user, UserAuditEntity.AuditEventType.USER_ARCHIVED,
            Map.of("archivedAt", user.getArchivedAt().toString()));
    }

    public UserEntity getByReferenceId(String referenceId) {
        log.debug("Getting user entity by reference ID: {}", referenceId);
        return getUserEntityByReferenceId(referenceId);
    }

    // Helper methods
    private UserEntity getUserEntityByReferenceId(String referenceId) {
        log.debug("Getting user entity by reference ID: {}", referenceId);
        return userRepository.findByReferenceId(referenceId)
            .orElseThrow(() -> new UserNotFoundException("User not found with reference ID: " + referenceId));
    }

    /**
     * Mask email for logging privacy
     */
    private String maskEmail(String email) {
        if (email == null || email.isEmpty()) {
            return "null";
        }
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) {
            return email.charAt(0) + "***@***";
        }
        return email.substring(0, 2) + "***" + email.substring(atIndex);
    }

    private void auditUserAction(UserEntity user, UserAuditEntity.AuditEventType eventType, Map<String, String> details) {
        log.debug("Auditing user action: {} for user: {}", eventType, user.getReferenceId());

        UserAuditEntity audit = new UserAuditEntity();
        audit.setUser(user); // Use the relationship instead of setUserId
        audit.setEventType(eventType);
        // Convert Map<String, String> to Map<String, Object>
        Map<String, Object> objectDetails = details.entrySet().stream()
            .collect(java.util.stream.Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue
            ));
        audit.setDetails(objectDetails);
        audit.setCreatedAt(LocalDateTime.now());

        // Save audit record (assuming we have userAuditRepository)
        // userAuditRepository.save(audit);

        log.info("User action audited: {} for user: {}", eventType, user.getReferenceId());
    }

    // Exception classes
    public static class UserServiceException extends RuntimeException {
        public UserServiceException(String message) {
            super(message);
        }

        public UserServiceException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class UserNotFoundException extends UserServiceException {
        public UserNotFoundException(String message) {
            super(message);
        }
    }

    public static class UserValidationException extends UserServiceException {
        public UserValidationException(String message) {
            super(message);
        }
    }
}
