package com.mysillydreams.userservice.service;

import com.mysillydreams.userservice.domain.UserEntity;
import com.mysillydreams.userservice.dto.UserDto;
import com.mysillydreams.userservice.encryption.PiiMapper;
import com.mysillydreams.userservice.mapper.UserMapper;
import com.mysillydreams.userservice.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for HMAC-based user lookups and searches.
 * Provides secure search functionality for encrypted PII fields.
 */
@Service
@Slf4j
@Transactional(readOnly = true)
public class UserLookupService {

    private final UserRepository userRepository;
    private final PiiMapper piiMapper;
    private final UserMapper userMapper;

    public UserLookupService(UserRepository userRepository, 
                           PiiMapper piiMapper, 
                           UserMapper userMapper) {
        this.userRepository = userRepository;
        this.piiMapper = piiMapper;
        this.userMapper = userMapper;
    }

    /**
     * Finds a user by email using HMAC-based search
     */
    @PreAuthorize("hasAnyRole('SERVICE_USER_LOOKUP', 'INTERNAL_CONSUMER', 'ADMIN')")
    @Cacheable(value = "user-lookup", key = "'email:' + #email")
    public Optional<UserDto> findByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return Optional.empty();
        }

        try {
            log.debug("Looking up user by email");
            
            String emailHmac = piiMapper.generateEmailLookupHmac(email);
            if (emailHmac == null) {
                log.warn("Failed to generate email HMAC for lookup");
                return Optional.empty();
            }

            Optional<UserEntity> userEntity = userRepository.findByEmailHmacAndActiveTrue(emailHmac);
            
            if (userEntity.isPresent()) {
                log.debug("Found user by email HMAC");
                UserDto userDto = userMapper.toDto(userEntity.get());
                return Optional.of(userDto);
            } else {
                log.debug("No user found with provided email");
                return Optional.empty();
            }

        } catch (Exception e) {
            log.error("Error during email-based user lookup: {}", e.getMessage());
            throw new UserLookupException("Email lookup failed", e);
        }
    }

    /**
     * Finds a user by phone number using HMAC-based search
     */
    @PreAuthorize("hasAnyRole('SERVICE_USER_LOOKUP', 'INTERNAL_CONSUMER', 'ADMIN')")
    @Cacheable(value = "user-lookup", key = "'phone:' + #phone")
    public Optional<UserDto> findByPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return Optional.empty();
        }

        try {
            log.debug("Looking up user by phone");
            
            String phoneHmac = piiMapper.generatePhoneLookupHmac(phone);
            if (phoneHmac == null) {
                log.warn("Failed to generate phone HMAC for lookup");
                return Optional.empty();
            }

            Optional<UserEntity> userEntity = userRepository.findByPhoneHmacAndActiveTrue(phoneHmac);
            
            if (userEntity.isPresent()) {
                log.debug("Found user by phone HMAC");
                UserDto userDto = userMapper.toDto(userEntity.get());
                return Optional.of(userDto);
            } else {
                log.debug("No user found with provided phone");
                return Optional.empty();
            }

        } catch (Exception e) {
            log.error("Error during phone-based user lookup: {}", e.getMessage());
            throw new UserLookupException("Phone lookup failed", e);
        }
    }

    /**
     * Finds a user by email or phone (tries both)
     */
    @PreAuthorize("hasAnyRole('SERVICE_USER_LOOKUP', 'INTERNAL_CONSUMER', 'ADMIN')")
    public Optional<UserDto> findByEmailOrPhone(String emailOrPhone) {
        if (emailOrPhone == null || emailOrPhone.trim().isEmpty()) {
            return Optional.empty();
        }

        try {
            // Try email first if it looks like an email
            if (piiMapper.isValidEmail(emailOrPhone)) {
                log.debug("Attempting email lookup for: {}", emailOrPhone.substring(0, Math.min(3, emailOrPhone.length())) + "***");
                Optional<UserDto> result = findByEmail(emailOrPhone);
                if (result.isPresent()) {
                    return result;
                }
            }

            // Try phone if it looks like a phone number
            if (piiMapper.isValidPhone(emailOrPhone)) {
                log.debug("Attempting phone lookup");
                return findByPhone(emailOrPhone);
            }

            log.debug("Input doesn't match email or phone format");
            return Optional.empty();

        } catch (Exception e) {
            log.error("Error during email/phone lookup: {}", e.getMessage());
            throw new UserLookupException("Email/phone lookup failed", e);
        }
    }

    /**
     * Bulk lookup users by multiple criteria
     */
    @PreAuthorize("hasAnyRole('INTERNAL_CONSUMER', 'ADMIN')")
    public List<UserDto> bulkLookup(BulkLookupRequest request) {
        if (request == null) {
            return List.of();
        }

        try {
            log.debug("Performing bulk user lookup with {} criteria", request.getCriteriaCount());
            
            List<UserEntity> users = userRepository.findByBulkCriteria(
                request.getUserReferenceIds(),
                request.getEmailHmacs(),
                request.getPhoneHmacs(),
                request.getUserIds()
            );

            log.debug("Found {} users from bulk lookup", users.size());
            
            return users.stream()
                    .map(userMapper::toDtoMinimal) // Use minimal DTO for bulk operations
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error during bulk user lookup: {}", e.getMessage());
            throw new UserLookupException("Bulk lookup failed", e);
        }
    }

    /**
     * Checks if email is already registered (for uniqueness validation)
     */
    @PreAuthorize("hasAnyRole('SERVICE_USER_LOOKUP', 'INTERNAL_CONSUMER', 'ADMIN')")
    public boolean isEmailRegistered(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }

        try {
            String emailHmac = piiMapper.generateEmailLookupHmac(email);
            if (emailHmac == null) {
                return false;
            }

            boolean exists = userRepository.existsByEmailHmacAndActiveTrue(emailHmac);
            log.debug("Email registration check result: {}", exists);
            return exists;

        } catch (Exception e) {
            log.error("Error checking email registration: {}", e.getMessage());
            return false; // Assume not registered on error
        }
    }

    /**
     * Checks if phone is already registered (for uniqueness validation)
     */
    @PreAuthorize("hasAnyRole('SERVICE_USER_LOOKUP', 'INTERNAL_CONSUMER', 'ADMIN')")
    public boolean isPhoneRegistered(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return false;
        }

        try {
            String phoneHmac = piiMapper.generatePhoneLookupHmac(phone);
            if (phoneHmac == null) {
                return false;
            }

            boolean exists = userRepository.existsByPhoneHmacAndActiveTrue(phoneHmac);
            log.debug("Phone registration check result: {}", exists);
            return exists;

        } catch (Exception e) {
            log.error("Error checking phone registration: {}", e.getMessage());
            return false; // Assume not registered on error
        }
    }

    /**
     * Gets minimal user information for internal services
     */
    @PreAuthorize("hasRole('INTERNAL_CONSUMER')")
    @Cacheable(value = "user-lookup", key = "'minimal:' + #userReferenceId")
    public Optional<UserDto> getMinimalUserInfo(String userReferenceId) {
        if (userReferenceId == null || userReferenceId.trim().isEmpty()) {
            return Optional.empty();
        }

        try {
            log.debug("Getting minimal user info for reference: {}", userReferenceId);
            
            Optional<UserEntity> userEntity = userRepository.findByReferenceIdAndActiveTrue(userReferenceId);
            
            if (userEntity.isPresent()) {
                UserDto minimalDto = userMapper.toDtoMinimal(userEntity.get());
                return Optional.of(minimalDto);
            } else {
                log.debug("No user found with reference ID: {}", userReferenceId);
                return Optional.empty();
            }

        } catch (Exception e) {
            log.error("Error getting minimal user info: {}", e.getMessage());
            throw new UserLookupException("Minimal user info lookup failed", e);
        }
    }

    /**
     * Request DTO for bulk lookups
     */
    public static class BulkLookupRequest {
        private List<String> userReferenceIds;
        private List<UUID> userIds;
        private List<String> emails;
        private List<String> phones;

        // Computed fields for HMAC lookups
        private List<String> emailHmacs;
        private List<String> phoneHmacs;

        public BulkLookupRequest() {}

        public BulkLookupRequest(List<String> userReferenceIds, List<UUID> userIds, 
                               List<String> emails, List<String> phones) {
            this.userReferenceIds = userReferenceIds;
            this.userIds = userIds;
            this.emails = emails;
            this.phones = phones;
        }

        // Getters and setters
        public List<String> getUserReferenceIds() { return userReferenceIds; }
        public void setUserReferenceIds(List<String> userReferenceIds) { this.userReferenceIds = userReferenceIds; }
        
        public List<UUID> getUserIds() { return userIds; }
        public void setUserIds(List<UUID> userIds) { this.userIds = userIds; }
        
        public List<String> getEmails() { return emails; }
        public void setEmails(List<String> emails) { this.emails = emails; }
        
        public List<String> getPhones() { return phones; }
        public void setPhones(List<String> phones) { this.phones = phones; }
        
        public List<String> getEmailHmacs() { return emailHmacs; }
        public void setEmailHmacs(List<String> emailHmacs) { this.emailHmacs = emailHmacs; }
        
        public List<String> getPhoneHmacs() { return phoneHmacs; }
        public void setPhoneHmacs(List<String> phoneHmacs) { this.phoneHmacs = phoneHmacs; }

        public int getCriteriaCount() {
            int count = 0;
            if (userReferenceIds != null) count += userReferenceIds.size();
            if (userIds != null) count += userIds.size();
            if (emails != null) count += emails.size();
            if (phones != null) count += phones.size();
            return count;
        }
    }

    /**
     * Exception thrown when user lookup operations fail
     */
    public static class UserLookupException extends RuntimeException {
        public UserLookupException(String message) {
            super(message);
        }

        public UserLookupException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
