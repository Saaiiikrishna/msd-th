package com.mysillydreams.userservice.service;

import com.mysillydreams.userservice.domain.*;
import com.mysillydreams.userservice.dto.DataDeletionResult;
import com.mysillydreams.userservice.dto.DataPurgeResult;
import com.mysillydreams.userservice.dto.UserDataExport;
import com.mysillydreams.userservice.repository.*;
import com.mysillydreams.userservice.security.RoleHierarchyConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Service for GDPR/DPDP compliant data deletion and purging.
 * Handles right to be forgotten, data retention, and secure data purging.
 */
@Service
@Slf4j
@Transactional
public class DataDeletionService {

    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final ConsentRepository consentRepository;
    private final UserAuditRepository userAuditRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final AuditService auditService;
    private final EventPublishingService eventPublishingService;

    public DataDeletionService(UserRepository userRepository,
                              AddressRepository addressRepository,
                              ConsentRepository consentRepository,
                              UserAuditRepository userAuditRepository,
                              OutboxEventRepository outboxEventRepository,
                              AuditService auditService,
                              EventPublishingService eventPublishingService) {
        this.userRepository = userRepository;
        this.addressRepository = addressRepository;
        this.consentRepository = consentRepository;
        this.userAuditRepository = userAuditRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.auditService = auditService;
        this.eventPublishingService = eventPublishingService;
    }

    /**
     * Implements GDPR Article 17 - Right to be Forgotten
     * Performs complete data erasure for a user
     */
    @PreAuthorize("hasRole('ADMIN') or hasRole('DPO')")
    public DataDeletionResult processRightToBeForgotten(String userReferenceId, String requestReason, 
                                                       boolean retainAuditTrail) {
        log.info("Processing right to be forgotten request for user: {} with reason: {}", 
                userReferenceId, requestReason);

        try {
            // Find user (including soft-deleted)
            UserEntity user = userRepository.findByReferenceId(userReferenceId)
                .orElseThrow(() -> new DataDeletionException("User not found: " + userReferenceId));

            // Create deletion audit record before starting
            UUID deletionRequestId = UUID.randomUUID();
            auditService.createUserAudit(user, UserAuditEntity.AuditEventType.GDPR_DELETION_REQUESTED,
                "Right to be forgotten request initiated", getCurrentUserId(),
                Map.of("deletionRequestId", deletionRequestId.toString(), "reason", requestReason));

            DataDeletionResult result = new DataDeletionResult(deletionRequestId, userReferenceId);

            // Step 1: Soft delete user if not already deleted
            if (user.getActive()) {
                user.markAsDeleted();
                user.setDeletionReason("GDPR_RIGHT_TO_BE_FORGOTTEN");
                userRepository.save(user);
                result.incrementDeletedRecords("users", 1);
            }

            // Step 2: Delete addresses
            List<AddressEntity> addresses = addressRepository.findByUserId(user.getId());
            for (AddressEntity address : addresses) {
                addressRepository.delete(address);
                result.incrementDeletedRecords("addresses", 1);
            }

            // Step 3: Delete consents (but keep audit trail of deletion)
            List<ConsentEntity> consents = consentRepository.findByUserId(user.getId());
            for (ConsentEntity consent : consents) {
                auditService.createUserAudit(user, UserAuditEntity.AuditEventType.CONSENT_DELETED,
                    "Consent deleted due to GDPR request", getCurrentUserId(),
                    Map.of("consentKey", consent.getConsentKey(), "deletionRequestId", deletionRequestId.toString()));
                
                consentRepository.delete(consent);
                result.incrementDeletedRecords("consents", 1);
            }

            // Step 4: Purge PII from user record
            purgePiiFromUser(user, result);

            // Step 5: Handle audit records based on retention policy
            if (!retainAuditTrail) {
                handleAuditRecordDeletion(user, deletionRequestId, result);
            } else {
                anonymizeAuditRecords(user, deletionRequestId, result);
            }

            // Step 6: Clean up outbox events
            cleanupOutboxEvents(user, result);

            // Step 7: Create final audit record
            auditService.createUserAudit(user, UserAuditEntity.AuditEventType.GDPR_DELETION_COMPLETED,
                "Right to be forgotten request completed", getCurrentUserId(),
                Map.of("deletionRequestId", deletionRequestId.toString(), 
                       "recordsDeleted", result.getTotalDeletedRecords(),
                       "retainAuditTrail", retainAuditTrail));

            // Step 8: Publish deletion event
            eventPublishingService.publishUserDataDeletionEvent(user, deletionRequestId, requestReason);

            result.setCompletedAt(LocalDateTime.now());
            result.setStatus(DataDeletionResult.Status.COMPLETED);

            log.info("Completed right to be forgotten request for user: {} - deleted {} records", 
                    userReferenceId, result.getTotalDeletedRecords());

            return result;

        } catch (Exception e) {
            log.error("Failed to process right to be forgotten for user {}: {}", userReferenceId, e.getMessage());
            throw new DataDeletionException("Data deletion failed: " + e.getMessage(), e);
        }
    }

    /**
     * Purges expired soft-deleted users based on retention policy
     */
    @PreAuthorize("hasRole('ADMIN') or hasRole('DPO')")
    @Async
    public CompletableFuture<DataPurgeResult> purgeExpiredUsers(int retentionDays) {
        log.info("Starting purge of users deleted more than {} days ago", retentionDays);

        try {
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(retentionDays);
            List<UserEntity> expiredUsers = userRepository.findDeletedUsersBefore(cutoffDate);

            DataPurgeResult result = new DataPurgeResult();
            result.setStartedAt(LocalDateTime.now());
            result.setCutoffDate(cutoffDate);

            for (UserEntity user : expiredUsers) {
                try {
                    DataDeletionResult deletionResult = processRightToBeForgotten(
                        user.getReferenceId(), 
                        "AUTOMATIC_RETENTION_POLICY_PURGE", 
                        true // Retain audit trail for compliance
                    );
                    result.addSuccessfulDeletion(user.getReferenceId(), deletionResult);
                } catch (Exception e) {
                    log.error("Failed to purge user {}: {}", user.getReferenceId(), e.getMessage());
                    result.addFailedDeletion(user.getReferenceId(), e.getMessage());
                }
            }

            result.setCompletedAt(LocalDateTime.now());
            log.info("Completed purge operation: {} successful, {} failed", 
                    result.getSuccessfulDeletions().size(), result.getFailedDeletions().size());

            return CompletableFuture.completedFuture(result);

        } catch (Exception e) {
            log.error("Purge operation failed: {}", e.getMessage());
            throw new DataDeletionException("Purge operation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Generates data export for GDPR Article 20 - Right to Data Portability
     */
    @PreAuthorize("hasRole('ADMIN') or hasRole('DPO') or (authentication.principal instanceof T(com.mysillydreams.userservice.security.SessionAuthenticationFilter.UserPrincipal) and authentication.principal.userReferenceId == #userReferenceId)")
    @Transactional(readOnly = true)
    public UserDataExport generateDataExport(String userReferenceId) {
        log.info("Generating data export for user: {}", userReferenceId);

        UserEntity user = userRepository.findByReferenceId(userReferenceId)
            .orElseThrow(() -> new DataDeletionException("User not found: " + userReferenceId));

        UserDataExport export = new UserDataExport();
        export.setUserReferenceId(userReferenceId);
        export.setExportDate(LocalDateTime.now());
        export.setRequestedBy(getCurrentUserId().toString());

        // Export user data (decrypted)
        export.setUserData(createUserDataExport(user));

        // Export addresses
        List<AddressEntity> addresses = addressRepository.findByUserId(user.getId());
        export.setAddresses(addresses.stream()
            .map(this::createAddressDataExport)
            .toList());

        // Export consents
        List<ConsentEntity> consents = consentRepository.findByUserId(user.getId());
        export.setConsents(consents.stream()
            .map(this::createConsentDataExport)
            .toList());

        // Export audit records (limited to user-initiated actions)
        List<UserAuditEntity> auditRecords = userAuditRepository.findUserInitiatedAuditRecords(user.getId());
        export.setAuditRecords(auditRecords.stream()
            .map(this::createAuditDataExport)
            .toList());

        // Create audit record for data export
        auditService.createUserAudit(user, UserAuditEntity.AuditEventType.DATA_EXPORT_GENERATED,
            "User data export generated", getCurrentUserId(),
            Map.of("exportId", export.getExportId().toString()));

        log.info("Generated data export for user: {} with {} records", userReferenceId, export.getTotalRecords());
        return export;
    }

    // Private helper methods

    private void purgePiiFromUser(UserEntity user, DataDeletionResult result) {
        // Replace PII with anonymized values
        user.setFirstNameEnc("DELETED");
        user.setLastNameEnc("DELETED");
        user.setEmailEnc("DELETED");
        user.setPhoneEnc("DELETED");
        user.setEmailHmac("DELETED");
        user.setPhoneHmac("DELETED");
        user.setGender(null);
        user.setAvatarUrl(null);
        user.setDobEnc("DELETED");
        
        // Keep reference ID for audit trail but mark as anonymized
        user.setAnonymized(true);
        user.setAnonymizedAt(LocalDateTime.now());
        
        userRepository.save(user);
        result.incrementDeletedRecords("user_pii", 1);
    }

    private void handleAuditRecordDeletion(UserEntity user, UUID deletionRequestId, DataDeletionResult result) {
        // Delete non-essential audit records, keep compliance-required ones
        List<UserAuditEntity> auditRecords = userAuditRepository.findByUserId(user.getId());
        
        for (UserAuditEntity audit : auditRecords) {
            if (isAuditRecordDeletable(audit)) {
                userAuditRepository.delete(audit);
                result.incrementDeletedRecords("audit_records", 1);
            }
        }
    }

    private void anonymizeAuditRecords(UserEntity user, UUID deletionRequestId, DataDeletionResult result) {
        // Anonymize audit records instead of deleting them
        List<UserAuditEntity> auditRecords = userAuditRepository.findByUserId(user.getId());
        
        for (UserAuditEntity audit : auditRecords) {
            audit.anonymize();
            userAuditRepository.save(audit);
            result.incrementDeletedRecords("anonymized_audit_records", 1);
        }
    }

    private void cleanupOutboxEvents(UserEntity user, DataDeletionResult result) {
        // Delete processed outbox events older than 30 days
        LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
        List<OutboxEventEntity> oldEvents = outboxEventRepository.findProcessedEventsBefore(cutoff);
        
        for (OutboxEventEntity event : oldEvents) {
            outboxEventRepository.delete(event);
            result.incrementDeletedRecords("outbox_events", 1);
        }
    }

    private boolean isAuditRecordDeletable(UserAuditEntity audit) {
        // Keep compliance-critical audit records
        return !audit.getEventType().name().contains("GDPR") &&
               !audit.getEventType().name().contains("CONSENT") &&
               !audit.getEventType().name().contains("DELETION");
    }

    private UUID getCurrentUserId() {
        return RoleHierarchyConfig.SecurityUtils.getCurrentUserId();
    }

    // Data export helper methods
    private UserDataExport.UserData createUserDataExport(UserEntity user) {
        // Implementation for user data export
        return new UserDataExport.UserData(); // Simplified for brevity
    }

    private UserDataExport.AddressData createAddressDataExport(AddressEntity address) {
        // Implementation for address data export
        return new UserDataExport.AddressData(); // Simplified for brevity
    }

    private UserDataExport.ConsentData createConsentDataExport(ConsentEntity consent) {
        // Implementation for consent data export
        return new UserDataExport.ConsentData(); // Simplified for brevity
    }

    private UserDataExport.AuditData createAuditDataExport(UserAuditEntity audit) {
        // Implementation for audit data export
        return new UserDataExport.AuditData(); // Simplified for brevity
    }

    // Exception classes
    public static class DataDeletionException extends RuntimeException {
        public DataDeletionException(String message) {
            super(message);
        }

        public DataDeletionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
