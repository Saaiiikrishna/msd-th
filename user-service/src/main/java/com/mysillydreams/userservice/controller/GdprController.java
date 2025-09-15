package com.mysillydreams.userservice.controller;

import com.mysillydreams.userservice.dto.ApiResponseDto;
import com.mysillydreams.userservice.dto.DataDeletionResult;
import com.mysillydreams.userservice.dto.DataPurgeResult;
import com.mysillydreams.userservice.dto.UserDataExport;
import com.mysillydreams.userservice.service.DataDeletionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * REST Controller for GDPR/DPDP compliance operations.
 * Provides endpoints for data deletion, export, and privacy rights.
 */
@RestController
@RequestMapping("/api/v1/gdpr")
@Validated
@Slf4j
@Tag(name = "GDPR/DPDP Compliance", description = "APIs for GDPR and DPDP compliance operations")
@SecurityRequirement(name = "bearerAuth")
public class GdprController {

    private final DataDeletionService dataDeletionService;

    public GdprController(DataDeletionService dataDeletionService) {
        this.dataDeletionService = dataDeletionService;
    }

    /**
     * Processes right to be forgotten request (GDPR Article 17)
     */
    @PostMapping("/users/{userReferenceId}/delete")
    @Operation(
        summary = "Right to be forgotten",
        description = "Processes GDPR Article 17 right to be forgotten request with complete data erasure"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Deletion request processed successfully",
            content = @Content(schema = @Schema(implementation = DataDeletionResult.class))),
        @ApiResponse(responseCode = "404", description = "User not found",
            content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions (Admin/DPO only)",
            content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
        @ApiResponse(responseCode = "409", description = "Deletion already in progress",
            content = @Content(schema = @Schema(implementation = ApiResponseDto.class)))
    })
    public ResponseEntity<ApiResponseDto<DataDeletionResult>> processRightToBeForgotten(
            @Parameter(description = "User reference ID", example = "01234567-89ab-cdef-0123-456789abcdef")
            @PathVariable @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$", message = "Invalid UUID format")
            String userReferenceId,
            @Parameter(description = "Reason for deletion request", example = "User requested account deletion")
            @RequestParam @NotBlank(message = "Deletion reason is required") 
            String reason,
            @Parameter(description = "Whether to retain audit trail for compliance", example = "true")
            @RequestParam(defaultValue = "true") 
            boolean retainAuditTrail) {
        
        long startTime = System.currentTimeMillis();
        String requestId = getRequestId();
        
        try {
            log.info("Processing right to be forgotten for user: {} with reason: {}", userReferenceId, reason);
            
            DataDeletionResult result = dataDeletionService.processRightToBeForgotten(
                userReferenceId, reason, retainAuditTrail);
            
            ApiResponseDto<DataDeletionResult> response = ApiResponseDto.success(
                "Right to be forgotten request processed successfully", result)
                .withRequestId(requestId)
                .withVersion("v1")
                .withProcessingTime(System.currentTimeMillis() - startTime);
            
            return ResponseEntity.ok(response);
            
        } catch (DataDeletionService.DataDeletionException e) {
            log.warn("Data deletion failed for user {}: {}", userReferenceId, e.getMessage());
            
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponseDto.<DataDeletionResult>error("USER_NOT_FOUND", "User not found")
                        .withRequestId(requestId));
            } else {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponseDto.<DataDeletionResult>error("DELETION_FAILED", e.getMessage())
                        .withRequestId(requestId));
            }
            
        } catch (Exception e) {
            log.error("Failed to process right to be forgotten for user {}: {}", userReferenceId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponseDto.<DataDeletionResult>error("DELETION_PROCESSING_FAILED", "Failed to process deletion request")
                    .withRequestId(requestId));
        }
    }

    /**
     * Generates data export for right to data portability (GDPR Article 20)
     */
    @GetMapping("/users/{userReferenceId}/export")
    @Operation(
        summary = "Export user data",
        description = "Generates comprehensive data export for GDPR Article 20 right to data portability"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Data export generated successfully",
            content = @Content(schema = @Schema(implementation = UserDataExport.class))),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<ApiResponseDto<UserDataExport>> exportUserData(
            @Parameter(description = "User reference ID", example = "01234567-89ab-cdef-0123-456789abcdef")
            @PathVariable @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$", message = "Invalid UUID format")
            String userReferenceId) {
        
        long startTime = System.currentTimeMillis();
        String requestId = getRequestId();
        
        try {
            log.info("Generating data export for user: {}", userReferenceId);
            
            UserDataExport export = dataDeletionService.generateDataExport(userReferenceId);
            
            ApiResponseDto<UserDataExport> response = ApiResponseDto.success(
                "Data export generated successfully", export)
                .withRequestId(requestId)
                .withVersion("v1")
                .withProcessingTime(System.currentTimeMillis() - startTime);
            
            // Add export metadata to response headers
            HttpHeaders headers = new HttpHeaders();
            headers.add("X-Export-ID", export.getExportId().toString());
            headers.add("X-Export-Date", export.getExportDate().toString());
            headers.add("X-Total-Records", String.valueOf(export.getTotalRecords()));
            
            return ResponseEntity.ok()
                .headers(headers)
                .body(response);
            
        } catch (DataDeletionService.DataDeletionException e) {
            log.warn("Data export failed for user {}: {}", userReferenceId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponseDto.<UserDataExport>error("USER_NOT_FOUND", "User not found")
                    .withRequestId(requestId));
                    
        } catch (Exception e) {
            log.error("Failed to generate data export for user {}: {}", userReferenceId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponseDto.<UserDataExport>error("EXPORT_GENERATION_FAILED", "Failed to generate data export")
                    .withRequestId(requestId));
        }
    }

    /**
     * Downloads data export as JSON file
     */
    @GetMapping("/users/{userReferenceId}/export/download")
    @Operation(
        summary = "Download data export",
        description = "Downloads user data export as JSON file"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Data export file downloaded successfully"),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<UserDataExport> downloadUserDataExport(
            @Parameter(description = "User reference ID", example = "USR12345678")
            @PathVariable @Pattern(regexp = "^USR[0-9]{8,12}$", message = "Invalid reference ID format") 
            String userReferenceId) {
        
        try {
            log.info("Downloading data export for user: {}", userReferenceId);
            
            UserDataExport export = dataDeletionService.generateDataExport(userReferenceId);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setContentDispositionFormData("attachment", 
                String.format("user-data-export-%s-%s.json", userReferenceId, export.getExportId()));
            headers.add("X-Export-ID", export.getExportId().toString());
            
            return ResponseEntity.ok()
                .headers(headers)
                .body(export);
            
        } catch (DataDeletionService.DataDeletionException e) {
            log.warn("Data export download failed for user {}: {}", userReferenceId, e.getMessage());
            return ResponseEntity.notFound().build();
            
        } catch (Exception e) {
            log.error("Failed to download data export for user {}: {}", userReferenceId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Purges expired users based on retention policy (Admin only)
     */
    @PostMapping("/purge-expired")
    @Operation(
        summary = "Purge expired users",
        description = "Purges users deleted more than specified days ago (Admin/DPO only)"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "202", description = "Purge operation started successfully"),
        @ApiResponse(responseCode = "403", description = "Admin/DPO access required"),
        @ApiResponse(responseCode = "400", description = "Invalid retention days parameter")
    })
    public ResponseEntity<ApiResponseDto<String>> purgeExpiredUsers(
            @Parameter(description = "Retention period in days", example = "90")
            @RequestParam @Min(value = 30, message = "Retention period must be at least 30 days") 
            int retentionDays) {
        
        long startTime = System.currentTimeMillis();
        String requestId = getRequestId();
        
        try {
            log.info("Starting purge of users deleted more than {} days ago", retentionDays);
            
            // Start async purge operation
            CompletableFuture<DataPurgeResult> purgeOperation = 
                dataDeletionService.purgeExpiredUsers(retentionDays);
            
            // Store operation reference for tracking (in real implementation, you'd use a job tracking system)
            String operationId = UUID.randomUUID().toString();
            
            ApiResponseDto<String> response = ApiResponseDto.success(
                "Purge operation started successfully", operationId)
                .withRequestId(requestId)
                .withVersion("v1")
                .withProcessingTime(System.currentTimeMillis() - startTime);
            
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
            
        } catch (Exception e) {
            log.error("Failed to start purge operation: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponseDto.<String>error("PURGE_START_FAILED", "Failed to start purge operation")
                    .withRequestId(requestId));
        }
    }

    /**
     * Gets deletion status for a user
     */
    @GetMapping("/users/{userReferenceId}/deletion-status")
    @Operation(
        summary = "Get deletion status",
        description = "Gets the current deletion status for a user"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Deletion status retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<ApiResponseDto<DeletionStatus>> getDeletionStatus(
            @Parameter(description = "User reference ID", example = "01234567-89ab-cdef-0123-456789abcdef")
            @PathVariable @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$", message = "Invalid UUID format")
            String userReferenceId) {
        
        long startTime = System.currentTimeMillis();
        String requestId = getRequestId();
        
        try {
            log.debug("Getting deletion status for user: {}", userReferenceId);
            
            // This would typically query a deletion tracking table
            DeletionStatus status = new DeletionStatus();
            status.setUserReferenceId(userReferenceId);
            status.setStatus("NOT_DELETED"); // Simplified implementation
            
            ApiResponseDto<DeletionStatus> response = ApiResponseDto.success(
                "Deletion status retrieved successfully", status)
                .withRequestId(requestId)
                .withVersion("v1")
                .withProcessingTime(System.currentTimeMillis() - startTime);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to get deletion status for user {}: {}", userReferenceId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponseDto.<DeletionStatus>error("STATUS_RETRIEVAL_FAILED", "Failed to retrieve deletion status")
                    .withRequestId(requestId));
        }
    }

    // Helper methods
    private String getRequestId() {
        String requestId = MDC.get("requestId");
        return requestId != null ? requestId : UUID.randomUUID().toString();
    }

    // Supporting DTOs
    @Schema(description = "User deletion status information")
    public static class DeletionStatus {
        @Schema(description = "User reference ID")
        private String userReferenceId;
        
        @Schema(description = "Current deletion status")
        private String status;
        
        @Schema(description = "Deletion request date")
        private java.time.LocalDateTime deletionRequestedAt;
        
        @Schema(description = "Deletion completion date")
        private java.time.LocalDateTime deletionCompletedAt;

        // Getters and setters
        public String getUserReferenceId() { return userReferenceId; }
        public void setUserReferenceId(String userReferenceId) { this.userReferenceId = userReferenceId; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public java.time.LocalDateTime getDeletionRequestedAt() { return deletionRequestedAt; }
        public void setDeletionRequestedAt(java.time.LocalDateTime deletionRequestedAt) { this.deletionRequestedAt = deletionRequestedAt; }
        public java.time.LocalDateTime getDeletionCompletedAt() { return deletionCompletedAt; }
        public void setDeletionCompletedAt(java.time.LocalDateTime deletionCompletedAt) { this.deletionCompletedAt = deletionCompletedAt; }
    }
}
