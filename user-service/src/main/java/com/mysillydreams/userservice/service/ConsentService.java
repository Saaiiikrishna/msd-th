package com.mysillydreams.userservice.service;

import com.mysillydreams.userservice.domain.ConsentEntity;
import com.mysillydreams.userservice.domain.UserAuditEntity;
import com.mysillydreams.userservice.domain.UserEntity;
import com.mysillydreams.userservice.dto.ConsentDto;
import com.mysillydreams.userservice.repository.ConsentRepository;
import com.mysillydreams.userservice.repository.UserRepository;
import com.mysillydreams.userservice.security.RoleHierarchyConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing user consents for GDPR/DPDP compliance.
 * Handles consent grants, withdrawals, and audit trail.
 */
@Service
@Slf4j
@Transactional
public class ConsentService {

    private final ConsentRepository consentRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final EventPublishingService eventPublishingService;

    public ConsentService(ConsentRepository consentRepository,
                         UserRepository userRepository,
                         AuditService auditService,
                         EventPublishingService eventPublishingService) {
        this.consentRepository = consentRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.eventPublishingService = eventPublishingService;
    }

    /**
     * Grants consent for a user
     */
    @PreAuthorize("hasRole('ADMIN') or @securityUtils.isOwner(#userReferenceId) or authentication.principal instanceof T(com.mysillydreams.userservice.security.InternalApiKeyFilter.ServicePrincipal)")
    @CacheEvict(value = "user-consents", key = "#userReferenceId")
    public ConsentDto grantConsent(String userReferenceId, String consentKey, String consentVersion,
                                  InetAddress ipAddress, String userAgent) {
        log.info("Granting consent {} for user: {}", consentKey, userReferenceId);

        try {
            // Find user
            UserEntity user = findActiveUser(userReferenceId);

            // Validate consent key
            validateConsentKey(consentKey);

            // Find or create consent record
            ConsentEntity consent = consentRepository.findByUserIdAndConsentKey(user.getId(), consentKey)
                .orElse(new ConsentEntity(user, consentKey, true, consentVersion, ipAddress, userAgent));

            // Grant consent
            consent.grantConsent(consentVersion, ipAddress, userAgent);

            // Save consent
            ConsentEntity savedConsent = consentRepository.save(consent);
            log.info("Granted consent {} for user: {}", consentKey, userReferenceId);

            // Create audit record
            Map<String, Object> details = Map.of(
                "consentKey", consentKey,
                "consentVersion", consentVersion != null ? consentVersion : "unknown",
                "ipAddress", ipAddress != null ? ipAddress.getHostAddress() : "unknown"
            );
            auditService.createUserAudit(user, UserAuditEntity.AuditEventType.CONSENT_GRANTED,
                "Consent granted", getCurrentUserId(), details);

            // Publish event
            eventPublishingService.publishUserConsentGrantedEvent(user, consentKey, consentVersion);

            return toConsentDto(savedConsent);

        } catch (Exception e) {
            log.error("Failed to grant consent {} for user {}: {}", consentKey, userReferenceId, e.getMessage());
            throw new ConsentServiceException("Consent grant failed: " + e.getMessage(), e);
        }
    }

    /**
     * Withdraws consent for a user
     */
    @PreAuthorize("hasRole('ADMIN') or @securityUtils.isOwner(#userReferenceId)")
    @CacheEvict(value = "user-consents", key = "#userReferenceId")
    public ConsentDto withdrawConsent(String userReferenceId, String consentKey,
                                     InetAddress ipAddress, String userAgent) {
        log.info("Withdrawing consent {} for user: {}", consentKey, userReferenceId);

        try {
            // Find user
            UserEntity user = findActiveUser(userReferenceId);

            // Find consent record
            ConsentEntity consent = consentRepository.findByUserIdAndConsentKey(user.getId(), consentKey)
                .orElseThrow(() -> new ConsentServiceException("Consent not found: " + consentKey));

            // Withdraw consent
            consent.withdrawConsent(ipAddress, userAgent);

            // Save consent
            ConsentEntity savedConsent = consentRepository.save(consent);
            log.info("Withdrew consent {} for user: {}", consentKey, userReferenceId);

            // Create audit record
            Map<String, Object> details = Map.of(
                "consentKey", consentKey,
                "ipAddress", ipAddress != null ? ipAddress.getHostAddress() : "unknown"
            );
            auditService.createUserAudit(user, UserAuditEntity.AuditEventType.CONSENT_WITHDRAWN,
                "Consent withdrawn", getCurrentUserId(), details);

            // Publish event
            eventPublishingService.publishUserConsentWithdrawnEvent(user, consentKey);

            return toConsentDto(savedConsent);

        } catch (Exception e) {
            log.error("Failed to withdraw consent {} for user {}: {}", consentKey, userReferenceId, e.getMessage());
            throw new ConsentServiceException("Consent withdrawal failed: " + e.getMessage(), e);
        }
    }

    /**
     * Gets all consents for a user
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT_USER') or @securityUtils.isOwner(#userReferenceId)")
    @Cacheable(value = "user-consents", key = "#userReferenceId")
    @Transactional(readOnly = true)
    public List<ConsentDto> getUserConsents(String userReferenceId) {
        log.debug("Getting consents for user: {}", userReferenceId);

        UserEntity user = findActiveUser(userReferenceId);
        List<ConsentEntity> consents = consentRepository.findByUserId(user.getId());

        return consents.stream()
            .map(this::toConsentDto)
            .collect(Collectors.toList());
    }

    /**
     * Gets active consents for a user
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT_USER', 'INTERNAL_CONSUMER') or @securityUtils.isOwner(#userReferenceId)")
    @Transactional(readOnly = true)
    public List<ConsentDto> getUserActiveConsents(String userReferenceId) {
        log.debug("Getting active consents for user: {}", userReferenceId);

        UserEntity user = findActiveUser(userReferenceId);
        List<ConsentEntity> consents = consentRepository.findByUserIdAndGrantedTrue(user.getId());

        return consents.stream()
            .filter(ConsentEntity::isActive)
            .map(this::toConsentDto)
            .collect(Collectors.toList());
    }

    /**
     * Checks if user has granted a specific consent
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT_USER', 'INTERNAL_CONSUMER')")
    @Transactional(readOnly = true)
    public boolean hasConsent(String userReferenceId, String consentKey) {
        log.debug("Checking consent {} for user: {}", consentKey, userReferenceId);

        try {
            UserEntity user = findActiveUser(userReferenceId);
            return consentRepository.existsByUserIdAndConsentKeyAndGrantedTrue(user.getId(), consentKey);
        } catch (Exception e) {
            log.debug("Consent check failed for user {} and consent {}: {}", userReferenceId, consentKey, e.getMessage());
            return false;
        }
    }

    /**
     * Gets consent statistics for admin dashboard
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT_USER')")
    @Transactional(readOnly = true)
    public Map<String, ConsentStatistics> getConsentStatistics() {
        log.debug("Getting consent statistics");

        List<Object[]> stats = consentRepository.getConsentStatistics();
        
        return stats.stream()
            .collect(Collectors.toMap(
                stat -> (String) stat[0], // consentKey
                stat -> new ConsentStatistics(
                    ((Number) stat[1]).longValue(), // granted
                    ((Number) stat[2]).longValue()  // withdrawn
                )
            ));
    }

    /**
     * Gets users with marketing consents
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT_USER', 'INTERNAL_CONSUMER')")
    @Transactional(readOnly = true)
    public List<UUID> getUsersWithMarketingConsents() {
        log.debug("Getting users with marketing consents");
        return consentRepository.findUsersWithAllMarketingConsents();
    }

    /**
     * Gets users who withdrew marketing consents
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT_USER', 'INTERNAL_CONSUMER')")
    @Transactional(readOnly = true)
    public List<UUID> getUsersWithWithdrawnMarketingConsents() {
        log.debug("Getting users with withdrawn marketing consents");
        return consentRepository.findUsersWithWithdrawnMarketingConsents();
    }

    /**
     * Bulk grants consent for multiple users (admin operation)
     */
    @PreAuthorize("hasRole('ADMIN')")
    @CacheEvict(value = "user-consents", allEntries = true)
    public void bulkGrantConsent(List<String> userReferenceIds, String consentKey, String consentVersion) {
        log.info("Bulk granting consent {} for {} users", consentKey, userReferenceIds.size());

        validateConsentKey(consentKey);

        int successCount = 0;
        int failureCount = 0;

        for (String userReferenceId : userReferenceIds) {
            try {
                grantConsent(userReferenceId, consentKey, consentVersion, null, "ADMIN_BULK_OPERATION");
                successCount++;
            } catch (Exception e) {
                log.warn("Failed to grant consent {} for user {}: {}", consentKey, userReferenceId, e.getMessage());
                failureCount++;
            }
        }

        log.info("Bulk consent grant completed: {} successful, {} failed", successCount, failureCount);
    }

    // Helper methods

    private UserEntity findActiveUser(String userReferenceId) {
        return userRepository.findByReferenceIdAndActiveTrue(userReferenceId)
            .orElseThrow(() -> new ConsentServiceException("User not found: " + userReferenceId));
    }

    private void validateConsentKey(String consentKey) {
        if (consentKey == null || consentKey.trim().isEmpty()) {
            throw new ConsentServiceException("Consent key cannot be null or empty");
        }

        // Validate against known consent keys
        if (!isValidConsentKey(consentKey)) {
            log.warn("Unknown consent key: {}", consentKey);
        }
    }

    private boolean isValidConsentKey(String consentKey) {
        // Check against predefined consent keys
        return consentKey.equals(ConsentEntity.ConsentKeys.MARKETING_EMAILS) ||
               consentKey.equals(ConsentEntity.ConsentKeys.MARKETING_SMS) ||
               consentKey.equals(ConsentEntity.ConsentKeys.ANALYTICS) ||
               consentKey.equals(ConsentEntity.ConsentKeys.PERSONALIZATION) ||
               consentKey.equals(ConsentEntity.ConsentKeys.THIRD_PARTY_SHARING) ||
               consentKey.equals(ConsentEntity.ConsentKeys.DATA_PROCESSING) ||
               consentKey.equals(ConsentEntity.ConsentKeys.COOKIES_FUNCTIONAL) ||
               consentKey.equals(ConsentEntity.ConsentKeys.COOKIES_ANALYTICS) ||
               consentKey.equals(ConsentEntity.ConsentKeys.COOKIES_MARKETING);
    }

    private UUID getCurrentUserId() {
        return RoleHierarchyConfig.SecurityUtils.getCurrentUserId();
    }

    private ConsentDto toConsentDto(ConsentEntity entity) {
        ConsentDto dto = new ConsentDto();
        dto.setConsentKey(entity.getConsentKey());
        dto.setGranted(entity.getGranted());
        dto.setGrantedAt(entity.getGrantedAt());
        dto.setWithdrawnAt(entity.getWithdrawnAt());
        dto.setConsentVersion(entity.getConsentVersion());
        dto.setSource(entity.getSource() != null ? entity.getSource().name() : null);
        dto.setLegalBasis(entity.getLegalBasis() != null ? entity.getLegalBasis().name() : null);
        dto.setIpAddress(entity.getIpAddress() != null ? entity.getIpAddress().getHostAddress() : null);
        dto.setUserAgent(entity.getUserAgent());
        return dto;
    }

    // DTOs and Exception classes

    public static class ConsentStatistics {
        private final long granted;
        private final long withdrawn;

        public ConsentStatistics(long granted, long withdrawn) {
            this.granted = granted;
            this.withdrawn = withdrawn;
        }

        public long getGranted() { return granted; }
        public long getWithdrawn() { return withdrawn; }
        public long getTotal() { return granted + withdrawn; }
        public double getGrantedPercentage() { 
            return getTotal() > 0 ? (double) granted / getTotal() * 100 : 0; 
        }
    }

    public static class ConsentServiceException extends RuntimeException {
        public ConsentServiceException(String message) {
            super(message);
        }

        public ConsentServiceException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
