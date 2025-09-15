package com.mysillydreams.userservice.service;

import com.mysillydreams.userservice.domain.UserAuditEntity;
import com.mysillydreams.userservice.domain.UserEntity;
import com.mysillydreams.userservice.repository.UserAuditRepository;
import com.mysillydreams.userservice.repository.UserRepository;
import com.mysillydreams.userservice.security.RoleHierarchyConfig;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for managing audit records and compliance tracking.
 * Provides comprehensive audit trail functionality.
 */
@Service
@Slf4j
@Transactional
public class AuditService {

    private final UserAuditRepository userAuditRepository;
    private final UserRepository userRepository;

    public AuditService(UserAuditRepository userAuditRepository, UserRepository userRepository) {
        this.userAuditRepository = userAuditRepository;
        this.userRepository = userRepository;
        log.info("AuditService initialized with UserAuditRepository and UserRepository");
    }

    /**
     * Creates a user audit record with basic information
     */
    public void createUserAudit(UserEntity user, UserAuditEntity.AuditEventType eventType, 
                               String description, UUID performedBy) {
        createUserAudit(user, eventType, description, performedBy, null);
    }

    /**
     * Creates a user audit record with additional details
     */
    public void createUserAudit(UserEntity user, UserAuditEntity.AuditEventType eventType, 
                               String description, UUID performedBy, Map<String, Object> additionalDetails) {
        try {
            // Create audit record
            UserAuditEntity audit = new UserAuditEntity(user, eventType);
            audit.setPerformedBy(performedBy);

            // Add description and details
            audit.addDetail("description", description);
            if (additionalDetails != null) {
                additionalDetails.forEach(audit::addDetail);
            }

            // Add context from MDC if available
            addContextFromMDC(audit);

            // Save audit record
            userAuditRepository.save(audit);
            log.debug("Created audit record: {} for user: {}", eventType, user.getReferenceId());

        } catch (Exception e) {
            log.error("Failed to create audit record for user {}: {}", user.getReferenceId(), e.getMessage());
            // Don't throw exception to avoid breaking the main operation
        }
    }

    /**
     * Creates a user audit record with full context
     */
    public void createUserAuditWithContext(UserEntity user, UserAuditEntity.AuditEventType eventType,
                                          String description, UUID performedBy, Map<String, Object> details,
                                          InetAddress ipAddress, String userAgent, String sessionId) {
        try {
            UserAuditEntity audit = UserAuditEntity.createAuditRecord(
                user, eventType, details, performedBy, ipAddress, userAgent, sessionId, getCorrelationId()
            );

            if (description != null) {
                audit.addDetail("description", description);
            }

            userAuditRepository.save(audit);
            log.debug("Created detailed audit record: {} for user: {}", eventType, user.getReferenceId());

        } catch (Exception e) {
            log.error("Failed to create detailed audit record for user {}: {}", user.getReferenceId(), e.getMessage());
        }
    }

    /**
     * Gets audit records for a specific user
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT_USER') or (authentication.principal instanceof T(com.mysillydreams.userservice.security.SessionAuthenticationFilter.UserPrincipal) and authentication.principal.userReferenceId == #userReferenceId)")
    @Transactional(readOnly = true)
    public Page<UserAuditEntity> getUserAuditRecords(String userReferenceId, Pageable pageable) {
        log.debug("Getting audit records for user: {}", userReferenceId);

        UUID userId = getUserIdByReferenceId(userReferenceId);
        return userAuditRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    /**
     * Gets audit records by event type
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT_USER')")
    @Transactional(readOnly = true)
    public Page<UserAuditEntity> getAuditRecordsByEventType(UserAuditEntity.AuditEventType eventType,
                                                           Pageable pageable) {
        log.debug("Getting audit records by event type: {}", eventType);
        return userAuditRepository.findByEventTypeOrderByCreatedAtDesc(eventType.name(), pageable);
    }

    /**
     * Gets security-related audit records
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT_USER')")
    @Transactional(readOnly = true)
    public Page<UserAuditEntity> getSecurityAuditRecords(Pageable pageable) {
        log.debug("Getting security audit records");
        return userAuditRepository.findSecurityAuditRecords(pageable);
    }

    /**
     * Gets privacy-related audit records
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT_USER')")
    @Transactional(readOnly = true)
    public Page<UserAuditEntity> getPrivacyAuditRecords(Pageable pageable) {
        log.debug("Getting privacy audit records");
        return userAuditRepository.findPrivacyAuditRecords(pageable);
    }

    /**
     * Gets audit records within a date range
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT_USER')")
    @Transactional(readOnly = true)
    public Page<UserAuditEntity> getAuditRecordsBetween(LocalDateTime startDate, LocalDateTime endDate,
                                                       Pageable pageable) {
        log.debug("Getting audit records between {} and {}", startDate, endDate);
        return userAuditRepository.findAuditRecordsBetween(startDate, endDate, pageable);
    }

    /**
     * Gets audit records by correlation ID
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT_USER')")
    @Transactional(readOnly = true)
    public List<UserAuditEntity> getAuditRecordsByCorrelationId(String correlationId) {
        log.debug("Getting audit records by correlation ID: {}", correlationId);
        return userAuditRepository.findByCorrelationIdOrderByCreatedAtDesc(correlationId);
    }

    /**
     * Gets audit statistics for compliance reporting
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT_USER')")
    @Transactional(readOnly = true)
    public AuditStatistics getAuditStatistics(LocalDateTime since) {
        log.debug("Getting audit statistics since: {}", since);

        Object[] stats = userAuditRepository.getAuditStatistics(since);

        return new AuditStatistics(
            ((Number) stats[0]).longValue(), // userEvents
            ((Number) stats[1]).longValue(), // consentEvents
            ((Number) stats[2]).longValue(), // authEvents
            ((Number) stats[3]).longValue()  // securityEvents
        );
    }

    /**
     * Gets failed login attempts for a user
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT_USER')")
    @Transactional(readOnly = true)
    public long getFailedLoginAttempts(String userReferenceId, LocalDateTime since) {
        log.debug("Getting failed login attempts for user: {} since: {}", userReferenceId, since);
        
        UUID userId = getUserIdByReferenceId(userReferenceId);
        return userAuditRepository.countFailedLoginAttempts(userId, since);
    }

    /**
     * Gets users with failed login attempts
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT_USER')")
    @Transactional(readOnly = true)
    public List<UUID> getUsersWithFailedLogins(LocalDateTime since) {
        log.debug("Getting users with failed logins since: {}", since);
        return userAuditRepository.findUsersWithFailedLogins(since);
    }

    /**
     * Gets suspicious activity records
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT_USER')")
    @Transactional(readOnly = true)
    public List<UserAuditEntity> getSuspiciousActivity(LocalDateTime since) {
        log.debug("Getting suspicious activity since: {}", since);
        return userAuditRepository.findSuspiciousActivity(since);
    }

    /**
     * Gets data access records for a user (GDPR compliance)
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT_USER') or (authentication.principal instanceof T(com.mysillydreams.userservice.security.SessionAuthenticationFilter.UserPrincipal) and authentication.principal.userReferenceId == #userReferenceId)")
    @Transactional(readOnly = true)
    public List<UserAuditEntity> getDataAccessRecords(String userReferenceId) {
        log.debug("Getting data access records for user: {}", userReferenceId);
        
        UUID userId = getUserIdByReferenceId(userReferenceId);
        return userAuditRepository.findDataAccessRecords(userId);
    }

    /**
     * Archives old audit records for data retention compliance
     */
    @PreAuthorize("hasRole('ADMIN')")
    public int archiveOldAuditRecords(LocalDateTime archiveDate) {
        log.info("Archiving audit records older than: {}", archiveDate);

        List<UserAuditEntity> recordsToArchive = userAuditRepository.findRecordsForArchival(archiveDate);

        // In a real implementation, you would move these to an archive storage
        // For now, we'll just log the count
        log.info("Found {} audit records for archival", recordsToArchive.size());

        return recordsToArchive.size();
    }

    /**
     * Deletes old audit records (after archival)
     */
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteOldAuditRecords(LocalDateTime deleteDate) {
        log.info("Deleting audit records older than: {}", deleteDate);
        
        try {
            userAuditRepository.deleteOldAuditRecords(deleteDate);
            log.info("Successfully deleted old audit records");
        } catch (Exception e) {
            log.error("Failed to delete old audit records: {}", e.getMessage());
            throw new AuditServiceException("Failed to delete old audit records", e);
        }
    }

    // Helper methods

    private void addContextFromMDC(UserAuditEntity audit) {
        try {
            String correlationId = MDC.get("correlationId");
            String sessionId = MDC.get("sessionId");
            String ipAddress = MDC.get("ipAddress");
            String userAgent = MDC.get("userAgent");

            if (correlationId != null) {
                audit.setCorrelationId(correlationId);
            }
            if (sessionId != null) {
                audit.setSessionId(sessionId);
            }
            if (ipAddress != null) {
                try {
                    audit.setIpAddress(InetAddress.getByName(ipAddress));
                } catch (Exception e) {
                    log.debug("Failed to parse IP address: {}", ipAddress);
                }
            }
            if (userAgent != null) {
                audit.setUserAgent(userAgent);
            }
        } catch (Exception e) {
            log.debug("Failed to add context from MDC: {}", e.getMessage());
        }
    }

    private String getCorrelationId() {
        return MDC.get("correlationId");
    }

    private UUID getUserIdByReferenceId(String userReferenceId) {
        log.debug("Looking up user ID for reference ID: {}", userReferenceId);
        try {
            UserEntity user = userRepository.findByReferenceId(userReferenceId)
                .orElseThrow(() -> new RuntimeException("User not found with reference ID: " + userReferenceId));
            log.debug("Found user ID: {} for reference ID: {}", user.getId(), userReferenceId);
            return user.getId();
        } catch (Exception e) {
            log.error("Error looking up user by reference ID {}: {}", userReferenceId, e.getMessage());
            throw e;
        }
    }

    // DTOs and Exception classes

    public static class AuditStatistics {
        private final long userEvents;
        private final long consentEvents;
        private final long authEvents;
        private final long securityEvents;

        public AuditStatistics(long userEvents, long consentEvents, long authEvents, long securityEvents) {
            this.userEvents = userEvents;
            this.consentEvents = consentEvents;
            this.authEvents = authEvents;
            this.securityEvents = securityEvents;
        }

        // Getters
        public long getUserEvents() { return userEvents; }
        public long getConsentEvents() { return consentEvents; }
        public long getAuthEvents() { return authEvents; }
        public long getSecurityEvents() { return securityEvents; }
        public long getTotalEvents() { return userEvents + consentEvents + authEvents + securityEvents; }
    }

    public static class AuditServiceException extends RuntimeException {
        public AuditServiceException(String message) {
            super(message);
        }

        public AuditServiceException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
