package com.mysillydreams.userservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Result object for GDPR/DPDP data deletion operations.
 * Tracks the progress and outcome of data deletion requests.
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Result of GDPR/DPDP data deletion operation")
public class DataDeletionResult {

    @Schema(description = "Unique identifier for the deletion request", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID deletionRequestId;

    @Schema(description = "User reference ID", example = "USR12345678")
    private String userReferenceId;

    @Schema(description = "Current status of the deletion operation")
    private Status status;

    @Schema(description = "When the deletion operation started", example = "2024-01-01T12:00:00")
    private LocalDateTime startedAt;

    @Schema(description = "When the deletion operation completed", example = "2024-01-01T12:05:00")
    private LocalDateTime completedAt;

    @Schema(description = "Number of records deleted by category")
    private Map<String, Integer> deletedRecordsByCategory;

    @Schema(description = "Total number of records deleted")
    private int totalDeletedRecords;

    @Schema(description = "Any errors encountered during deletion")
    private String errorMessage;

    @Schema(description = "Additional details about the deletion operation")
    private Map<String, Object> details;

    public DataDeletionResult() {
        this.deletedRecordsByCategory = new HashMap<>();
        this.details = new HashMap<>();
        this.startedAt = LocalDateTime.now();
        this.status = Status.IN_PROGRESS;
    }

    public DataDeletionResult(UUID deletionRequestId, String userReferenceId) {
        this();
        this.deletionRequestId = deletionRequestId;
        this.userReferenceId = userReferenceId;
    }

    public void incrementDeletedRecords(String category, int count) {
        deletedRecordsByCategory.merge(category, count, Integer::sum);
        this.totalDeletedRecords += count;
    }

    public void addDetail(String key, Object value) {
        this.details.put(key, value);
    }

    public void setError(String errorMessage) {
        this.status = Status.FAILED;
        this.errorMessage = errorMessage;
        this.completedAt = LocalDateTime.now();
    }

    public long getDurationMs() {
        if (startedAt == null) return 0;
        LocalDateTime endTime = completedAt != null ? completedAt : LocalDateTime.now();
        return java.time.Duration.between(startedAt, endTime).toMillis();
    }

    public enum Status {
        @Schema(description = "Deletion operation is in progress")
        IN_PROGRESS,
        
        @Schema(description = "Deletion operation completed successfully")
        COMPLETED,
        
        @Schema(description = "Deletion operation failed")
        FAILED,
        
        @Schema(description = "Deletion operation was cancelled")
        CANCELLED
    }

    // Summary methods for reporting
    public DeletionSummary getSummary() {
        DeletionSummary summary = new DeletionSummary();
        summary.setDeletionRequestId(deletionRequestId);
        summary.setUserReferenceId(userReferenceId);
        summary.setStatus(status);
        summary.setTotalRecordsDeleted(totalDeletedRecords);
        summary.setDurationMs(getDurationMs());
        summary.setCompletedAt(completedAt);
        return summary;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Summary of deletion operation")
    public static class DeletionSummary {
        @Schema(description = "Deletion request ID")
        private UUID deletionRequestId;
        
        @Schema(description = "User reference ID")
        private String userReferenceId;
        
        @Schema(description = "Operation status")
        private Status status;
        
        @Schema(description = "Total records deleted")
        private int totalRecordsDeleted;
        
        @Schema(description = "Operation duration in milliseconds")
        private long durationMs;
        
        @Schema(description = "Completion timestamp")
        private LocalDateTime completedAt;
    }
}


