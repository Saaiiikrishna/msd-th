package com.mysillydreams.userservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Result object for bulk data purge operations.
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Result of bulk data purge operation")
public class DataPurgeResult {

    @Schema(description = "When the purge operation started")
    private LocalDateTime startedAt;

    @Schema(description = "When the purge operation completed")
    private LocalDateTime completedAt;

    @Schema(description = "Cutoff date for purging records")
    private LocalDateTime cutoffDate;

    @Schema(description = "Successfully deleted users")
    private Map<String, DataDeletionResult> successfulDeletions = new HashMap<>();

    @Schema(description = "Failed deletion attempts")
    private Map<String, String> failedDeletions = new HashMap<>();

    public void addSuccessfulDeletion(String userReferenceId, DataDeletionResult result) {
        successfulDeletions.put(userReferenceId, result);
    }

    public void addFailedDeletion(String userReferenceId, String errorMessage) {
        failedDeletions.put(userReferenceId, errorMessage);
    }

    public int getTotalProcessed() {
        return successfulDeletions.size() + failedDeletions.size();
    }

    public int getTotalRecordsDeleted() {
        return successfulDeletions.values().stream()
            .mapToInt(DataDeletionResult::getTotalDeletedRecords)
            .sum();
    }

    public long getDurationMs() {
        if (startedAt == null || completedAt == null) return 0;
        return java.time.Duration.between(startedAt, completedAt).toMillis();
    }
}
