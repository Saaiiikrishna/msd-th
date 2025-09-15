package com.mysillydreams.userservice.service;

import com.mysillydreams.userservice.domain.AddressEntity;
import com.mysillydreams.userservice.domain.UserAuditEntity;
import com.mysillydreams.userservice.domain.UserEntity;
import com.mysillydreams.userservice.dto.AddressDto;
import com.mysillydreams.userservice.mapper.AddressMapper;
import com.mysillydreams.userservice.repository.AddressRepository;
import com.mysillydreams.userservice.repository.UserRepository;
import com.mysillydreams.userservice.security.RoleHierarchyConfig;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing user addresses with PII encryption.
 * Handles address CRUD operations and primary address management.
 */
@Service
@Slf4j
@Transactional
public class AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final AddressMapper addressMapper;
    private final AuditService auditService;
    private final EventPublishingService eventPublishingService;

    public AddressService(AddressRepository addressRepository,
                         UserRepository userRepository,
                         AddressMapper addressMapper,
                         AuditService auditService,
                         EventPublishingService eventPublishingService) {
        this.addressRepository = addressRepository;
        this.userRepository = userRepository;
        this.addressMapper = addressMapper;
        this.auditService = auditService;
        this.eventPublishingService = eventPublishingService;
    }

    /**
     * Adds a new address for a user
     */
    @PreAuthorize("hasRole('ADMIN') or (authentication.principal instanceof T(com.mysillydreams.userservice.security.SessionAuthenticationFilter.UserPrincipal) and authentication.principal.userReferenceId == #userReferenceId)")
    @CacheEvict(value = "user-addresses", key = "#userReferenceId")
    public AddressDto addAddress(String userReferenceId, AddressDto addressDto) {
        log.info("Adding address for user: {}", userReferenceId);

        try {
            // Find user
            UserEntity user = findActiveUser(userReferenceId);

            // Validate address
            validateAddressForCreation(addressDto);

            // Check address limits
            validateAddressLimits(user);

            // Create address entity
            AddressEntity addressEntity = addressMapper.toEntity(addressDto);
            addressEntity.setUser(user);

            // Handle primary address logic
            if (addressDto.getIsPrimary() != null && addressDto.getIsPrimary()) {
                handlePrimaryAddressAssignment(user);
            } else if (!hasAnyAddress(user)) {
                // Make first address primary by default
                addressEntity.markAsPrimary();
            }

            // Save address
            AddressEntity savedAddress = addressRepository.save(addressEntity);
            log.info("Added address {} for user: {}", savedAddress.getId(), userReferenceId);

            // Create audit record
            Map<String, Object> details = Map.of(
                "addressId", savedAddress.getId().toString(),
                "addressType", savedAddress.getType().name(),
                "isPrimary", savedAddress.getIsPrimary()
            );
            auditService.createUserAudit(user, UserAuditEntity.AuditEventType.ADDRESS_ADDED,
                "Address added", getCurrentUserId(), details);

            // Publish event
            eventPublishingService.publishUserAddressAddedEvent(user, savedAddress.getId(), 
                savedAddress.getType().name());

            return addressMapper.toDto(savedAddress);

        } catch (Exception e) {
            log.error("Failed to add address for user {}: {}", userReferenceId, e.getMessage());
            throw new AddressServiceException("Address creation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Updates an existing address
     */
    @PreAuthorize("hasRole('ADMIN') or (authentication.principal instanceof T(com.mysillydreams.userservice.security.SessionAuthenticationFilter.UserPrincipal) and authentication.principal.userReferenceId == #userReferenceId)")
    @CacheEvict(value = "user-addresses", key = "#userReferenceId")
    public AddressDto updateAddress(String userReferenceId, UUID addressId, AddressDto addressDto) {
        log.info("Updating address {} for user: {}", addressId, userReferenceId);

        try {
            // Find user and address
            UserEntity user = findActiveUser(userReferenceId);
            AddressEntity existingAddress = findUserAddress(user, addressId);

            // Validate update
            validateAddressForUpdate(addressDto);

            // Update address
            addressMapper.updateEntityFromDto(addressDto, existingAddress);

            // Handle primary address changes
            if (addressDto.getIsPrimary() != null && addressDto.getIsPrimary() && !existingAddress.getIsPrimary()) {
                handlePrimaryAddressAssignment(user);
                existingAddress.markAsPrimary();
            }

            // Save changes
            AddressEntity updatedAddress = addressRepository.save(existingAddress);
            log.info("Updated address {} for user: {}", addressId, userReferenceId);

            // Create audit record
            Map<String, Object> details = Map.of(
                "addressId", addressId.toString(),
                "addressType", updatedAddress.getType().name()
            );
            auditService.createUserAudit(user, UserAuditEntity.AuditEventType.ADDRESS_UPDATED,
                "Address updated", getCurrentUserId(), details);

            // Publish event
            eventPublishingService.publishUserAddressUpdatedEvent(user, addressId, 
                updatedAddress.getType().name());

            return addressMapper.toDto(updatedAddress);

        } catch (Exception e) {
            log.error("Failed to update address {} for user {}: {}", addressId, userReferenceId, e.getMessage());
            throw new AddressServiceException("Address update failed: " + e.getMessage(), e);
        }
    }

    /**
     * Deletes an address
     */
    @PreAuthorize("hasRole('ADMIN') or (authentication.principal instanceof T(com.mysillydreams.userservice.security.SessionAuthenticationFilter.UserPrincipal) and authentication.principal.userReferenceId == #userReferenceId)")
    @CacheEvict(value = "user-addresses", key = "#userReferenceId")
    public void deleteAddress(String userReferenceId, UUID addressId) {
        log.info("Deleting address {} for user: {}", addressId, userReferenceId);

        try {
            // Find user and address
            UserEntity user = findActiveUser(userReferenceId);
            AddressEntity address = findUserAddress(user, addressId);

            // Validate deletion
            validateAddressDeletion(user, address);

            String addressType = address.getType().name();
            boolean wasPrimary = address.getIsPrimary();

            // Delete address
            addressRepository.delete(address);
            log.info("Deleted address {} for user: {}", addressId, userReferenceId);

            // If deleted address was primary, assign new primary
            if (wasPrimary) {
                assignNewPrimaryAddress(user);
            }

            // Create audit record
            Map<String, Object> details = Map.of(
                "addressId", addressId.toString(),
                "addressType", addressType,
                "wasPrimary", wasPrimary
            );
            auditService.createUserAudit(user, UserAuditEntity.AuditEventType.ADDRESS_DELETED,
                "Address deleted", getCurrentUserId(), details);

            // Publish event
            eventPublishingService.publishUserAddressDeletedEvent(user, addressId, addressType);

        } catch (Exception e) {
            log.error("Failed to delete address {} for user {}: {}", addressId, userReferenceId, e.getMessage());
            throw new AddressServiceException("Address deletion failed: " + e.getMessage(), e);
        }
    }

    /**
     * Gets all addresses for a user
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT_USER') or (authentication.principal instanceof T(com.mysillydreams.userservice.security.SessionAuthenticationFilter.UserPrincipal) and authentication.principal.userReferenceId == #userReferenceId)")
    @Cacheable(value = "user-addresses", key = "#userReferenceId")
    @Transactional(readOnly = true)
    public List<AddressDto> getUserAddresses(String userReferenceId) {
        log.debug("Getting addresses for user: {}", userReferenceId);

        UserEntity user = findActiveUser(userReferenceId);
        List<AddressEntity> addresses = addressRepository.findByUserIdOrderByCreatedAtDesc(user.getId());

        // Return appropriate DTO based on user role
        if (RoleHierarchyConfig.SecurityUtils.isAdmin()) {
            return addresses.stream()
                .map(addressMapper::toDtoFull)
                .collect(Collectors.toList());
        } else {
            return addresses.stream()
                .map(addressMapper::toDto)
                .collect(Collectors.toList());
        }
    }

    /**
     * Gets primary address for a user
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT_USER') or (authentication.principal instanceof T(com.mysillydreams.userservice.security.SessionAuthenticationFilter.UserPrincipal) and authentication.principal.userReferenceId == #userReferenceId)")
    @Transactional(readOnly = true)
    public AddressDto getPrimaryAddress(String userReferenceId) {
        log.debug("Getting primary address for user: {}", userReferenceId);

        UserEntity user = findActiveUser(userReferenceId);
        return addressRepository.findByUserIdAndIsPrimaryTrue(user.getId())
            .map(addressMapper::toDto)
            .orElse(null);
    }

    /**
     * Sets an address as primary
     */
    @PreAuthorize("hasRole('ADMIN') or (authentication.principal instanceof T(com.mysillydreams.userservice.security.SessionAuthenticationFilter.UserPrincipal) and authentication.principal.userReferenceId == #userReferenceId)")
    @CacheEvict(value = "user-addresses", key = "#userReferenceId")
    public void setPrimaryAddress(String userReferenceId, UUID addressId) {
        log.info("Setting primary address {} for user: {}", addressId, userReferenceId);

        try {
            UserEntity user = findActiveUser(userReferenceId);
            AddressEntity address = findUserAddress(user, addressId);

            // Clear current primary and set new one
            addressRepository.setPrimaryAddress(user.getId(), addressId);
            log.info("Set primary address {} for user: {}", addressId, userReferenceId);

            // Create audit record
            Map<String, Object> details = Map.of(
                "addressId", addressId.toString(),
                "addressType", address.getType().name()
            );
            auditService.createUserAudit(user, UserAuditEntity.AuditEventType.ADDRESS_PRIMARY_CHANGED,
                "Primary address changed", getCurrentUserId(), details);

        } catch (Exception e) {
            log.error("Failed to set primary address {} for user {}: {}", addressId, userReferenceId, e.getMessage());
            throw new AddressServiceException("Primary address update failed: " + e.getMessage(), e);
        }
    }

    // Helper methods

    private UserEntity findActiveUser(String userReferenceId) {
        return userRepository.findByReferenceIdAndActiveTrue(userReferenceId)
            .orElseThrow(() -> new AddressServiceException("User not found: " + userReferenceId));
    }

    private AddressEntity findUserAddress(UserEntity user, UUID addressId) {
        return addressRepository.findById(addressId)
            .filter(address -> address.getUser().getId().equals(user.getId()))
            .orElseThrow(() -> new AddressServiceException("Address not found: " + addressId));
    }

    private void validateAddressForCreation(AddressDto addressDto) {
        if (addressDto.getType() == null) {
            throw new AddressValidationException("Address type is required");
        }
    }

    private void validateAddressForUpdate(AddressDto addressDto) {
        // Additional validation rules for updates
    }

    private void validateAddressLimits(UserEntity user) {
        long addressCount = addressRepository.countByUserId(user.getId());
        if (addressCount >= 10) { // Max 10 addresses per user
            throw new AddressValidationException("Maximum number of addresses reached");
        }
    }

    private void validateAddressDeletion(UserEntity user, AddressEntity address) {
        // Prevent deletion if it's the only address and user has orders
        long addressCount = addressRepository.countByUserId(user.getId());
        if (addressCount == 1) {
            log.warn("Attempting to delete the only address for user: {}", user.getReferenceId());
            // In a real system, you might check for active orders
        }
    }

    private boolean hasAnyAddress(UserEntity user) {
        return addressRepository.existsByUserId(user.getId());
    }

    private void handlePrimaryAddressAssignment(UserEntity user) {
        // Clear all primary addresses for the user
        addressRepository.clearPrimaryAddressForUser(user.getId());
    }

    private void assignNewPrimaryAddress(UserEntity user) {
        // Find the most recent address and make it primary
        List<AddressEntity> addresses = addressRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        if (!addresses.isEmpty()) {
            AddressEntity newPrimary = addresses.get(0);
            newPrimary.markAsPrimary();
            addressRepository.save(newPrimary);
        }
    }

    private UUID getCurrentUserId() {
        return RoleHierarchyConfig.SecurityUtils.getCurrentUserId();
    }

    // Exception classes
    public static class AddressServiceException extends RuntimeException {
        public AddressServiceException(String message) {
            super(message);
        }

        public AddressServiceException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class AddressValidationException extends AddressServiceException {
        public AddressValidationException(String message) {
            super(message);
        }
    }
}
