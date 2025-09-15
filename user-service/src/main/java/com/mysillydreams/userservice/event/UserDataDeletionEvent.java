package com.mysillydreams.userservice.event;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

/**
 * Event published when user data is deleted for GDPR compliance
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserDataDeletionEvent extends UserEvent {
    private UUID deletionRequestId;
    private String deletionReason;
    private int totalRecordsDeleted;
    private Map<String, Integer> deletedRecordsByCategory;
    private boolean retainAuditTrail;
    private UUID requestedBy;

    public UserDataDeletionEvent(String userReferenceId, UUID userId, UUID deletionRequestId,
                                String deletionReason, int totalRecordsDeleted,
                                Map<String, Integer> deletedRecordsByCategory,
                                boolean retainAuditTrail, UUID requestedBy) {
        super("USER_DATA_DELETION", userReferenceId, userId);
        this.deletionRequestId = deletionRequestId;
        this.deletionReason = deletionReason;
        this.totalRecordsDeleted = totalRecordsDeleted;
        this.deletedRecordsByCategory = deletedRecordsByCategory;
        this.retainAuditTrail = retainAuditTrail;
        this.requestedBy = requestedBy;
    }
}
