package com.mysillydreams.userservice.controller;

import com.mysillydreams.userservice.dto.ApiResponseDto;
import com.mysillydreams.userservice.service.AuditService;
import com.mysillydreams.userservice.service.AuditService.AuditStatistics;
import com.mysillydreams.userservice.service.ConsentService;
import com.mysillydreams.userservice.service.ConsentService.ConsentStatistics;
import com.mysillydreams.userservice.service.UserRoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for Administrative operations.
 * Provides admin-only endpoints for system management and reporting.
 */
@RestController
@RequestMapping("/api/v1/admin")
@Validated
@Slf4j
@Tag(name = "Administration", description = "Admin-only APIs for system management and reporting")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserRoleService userRoleService;
    private final ConsentService consentService;
    private final AuditService auditService;

    public AdminController(UserRoleService userRoleService,
                          ConsentService consentService,
                          AuditService auditService) {
        this.userRoleService = userRoleService;
        this.consentService = consentService;
        this.auditService = auditService;
    }

    /**
     * Gets role statistics for admin dashboard
     */
    @GetMapping("/roles/statistics")
    @Operation(
        summary = "Get role statistics",
        description = "Retrieves role distribution statistics for admin dashboard"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Role statistics retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Admin access required")
    })
    public ResponseEntity<ApiResponseDto<Map<String, Long>>> getRoleStatistics() {
        long startTime = System.currentTimeMillis();
        String requestId = getRequestId();
        
        try {
            log.debug("Retrieving role statistics");
            
            Map<String, Long> statistics = userRoleService.getRoleStatistics();
            
            ApiResponseDto<Map<String, Long>> response = ApiResponseDto.success("Role statistics retrieved successfully", statistics)
                .withRequestId(requestId)
                .withVersion("v1")
                .withProcessingTime(System.currentTimeMillis() - startTime);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to retrieve role statistics: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponseDto.<Map<String,Long>>error("STATISTICS_RETRIEVAL_FAILED", "Failed to retrieve role statistics")
                    .withRequestId(requestId));
        }
    }

    /**
     * Gets users with a specific role
     */
    @GetMapping("/roles/{role}/users")
    @Operation(
        summary = "Get users with role",
        description = "Retrieves all users assigned to a specific role"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Users retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Admin access required")
    })
    public ResponseEntity<ApiResponseDto<List<String>>> getUsersWithRole(
            @Parameter(description = "Role name", example = "ROLE_CUSTOMER")
            @PathVariable String role) {
        
        long startTime = System.currentTimeMillis();
        String requestId = getRequestId();
        
        try {
            log.debug("Retrieving users with role: {}", role);
            
            List<String> users = userRoleService.getUsersWithRole(role);
            
            ApiResponseDto<List<String>> response = ApiResponseDto.success("Users retrieved successfully", users)
                .withRequestId(requestId)
                .withVersion("v1")
                .withProcessingTime(System.currentTimeMillis() - startTime);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to retrieve users with role {}: {}", role, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponseDto.<List<String>>error("USER_RETRIEVAL_FAILED", "Failed to retrieve users with role")
                    .withRequestId(requestId));
        }
    }

    /**
     * Deactivates expired roles
     */
    @PostMapping("/roles/cleanup-expired")
    @Operation(
        summary = "Cleanup expired roles",
        description = "Deactivates all expired role assignments"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Expired roles cleaned up successfully"),
        @ApiResponse(responseCode = "403", description = "Admin access required")
    })
    public ResponseEntity<ApiResponseDto<Map<String, Object>>> cleanupExpiredRoles() {
        long startTime = System.currentTimeMillis();
        String requestId = getRequestId();
        
        try {
            log.info("Cleaning up expired roles");
            
            int deactivatedCount = userRoleService.deactivateExpiredRoles();
            
            Map<String, Object> result = Map.of(
                "deactivatedRoles", deactivatedCount,
                "cleanupTime", LocalDateTime.now()
            );
            
            ApiResponseDto<Map<String, Object>> response = ApiResponseDto.success("Expired roles cleaned up successfully", result)
                .withRequestId(requestId)
                .withVersion("v1")
                .withProcessingTime(System.currentTimeMillis() - startTime);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to cleanup expired roles: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponseDto.<Map<String,Object>>error("CLEANUP_FAILED", "Failed to cleanup expired roles")
                    .withRequestId(requestId));
        }
    }

    /**
     * Gets consent statistics for compliance dashboard
     */
    @GetMapping("/consents/statistics")
    @Operation(
        summary = "Get consent statistics",
        description = "Retrieves consent statistics for compliance reporting"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Consent statistics retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Admin access required")
    })
    public ResponseEntity<ApiResponseDto<Map<String, ConsentService.ConsentStatistics>>> getConsentStatistics() {
        long startTime = System.currentTimeMillis();
        String requestId = getRequestId();
        
        try {
            log.debug("Retrieving consent statistics");
            
            Map<String, ConsentService.ConsentStatistics> statistics = consentService.getConsentStatistics();
            
            ApiResponseDto<Map<String, ConsentService.ConsentStatistics>> response = 
                ApiResponseDto.success("Consent statistics retrieved successfully", statistics)
                    .withRequestId(requestId)
                    .withVersion("v1")
                    .withProcessingTime(System.currentTimeMillis() - startTime);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to retrieve consent statistics: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponseDto.<Map<String,ConsentStatistics>>error("STATISTICS_RETRIEVAL_FAILED", "Failed to retrieve consent statistics")
                    .withRequestId(requestId));
        }
    }

    /**
     * Gets users with marketing consents
     */
    @GetMapping("/consents/marketing/users")
    @Operation(
        summary = "Get users with marketing consents",
        description = "Retrieves users who have granted marketing consents"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Users retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Admin access required")
    })
    public ResponseEntity<ApiResponseDto<List<UUID>>> getUsersWithMarketingConsents() {
        long startTime = System.currentTimeMillis();
        String requestId = getRequestId();
        
        try {
            log.debug("Retrieving users with marketing consents");
            
            List<UUID> users = consentService.getUsersWithMarketingConsents();
            
            ApiResponseDto<List<UUID>> response = ApiResponseDto.success("Users retrieved successfully", users)
                .withRequestId(requestId)
                .withVersion("v1")
                .withProcessingTime(System.currentTimeMillis() - startTime);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to retrieve users with marketing consents: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponseDto.<List<UUID>>error("USER_RETRIEVAL_FAILED", "Failed to retrieve users with marketing consents")
                    .withRequestId(requestId));
        }
    }

    /**
     * Gets audit statistics for compliance reporting
     */
    @GetMapping("/audit/statistics")
    @Operation(
        summary = "Get audit statistics",
        description = "Retrieves audit statistics for compliance reporting"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Audit statistics retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Admin access required")
    })
    public ResponseEntity<ApiResponseDto<AuditService.AuditStatistics>> getAuditStatistics(
            @Parameter(description = "Start date for statistics", example = "2024-01-01T00:00:00")
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) 
            LocalDateTime since) {
        
        long startTime = System.currentTimeMillis();
        String requestId = getRequestId();
        
        try {
            LocalDateTime sinceDate = since != null ? since : LocalDateTime.now().minusDays(30);
            log.debug("Retrieving audit statistics since: {}", sinceDate);
            
            AuditService.AuditStatistics statistics = auditService.getAuditStatistics(sinceDate);
            
            ApiResponseDto<AuditService.AuditStatistics> response = 
                ApiResponseDto.success("Audit statistics retrieved successfully", statistics)
                    .withRequestId(requestId)
                    .withVersion("v1")
                    .withProcessingTime(System.currentTimeMillis() - startTime);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to retrieve audit statistics: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponseDto.<AuditStatistics>error("STATISTICS_RETRIEVAL_FAILED", "Failed to retrieve audit statistics")
                    .withRequestId(requestId));
        }
    }

    /**
     * Gets users with failed login attempts
     */
    @GetMapping("/security/failed-logins")
    @Operation(
        summary = "Get users with failed logins",
        description = "Retrieves users with failed login attempts for security monitoring"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Failed login users retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Admin access required")
    })
    public ResponseEntity<ApiResponseDto<List<UUID>>> getUsersWithFailedLogins(
            @Parameter(description = "Start date for failed login check", example = "2024-01-01T00:00:00")
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) 
            LocalDateTime since) {
        
        long startTime = System.currentTimeMillis();
        String requestId = getRequestId();
        
        try {
            LocalDateTime sinceDate = since != null ? since : LocalDateTime.now().minusHours(24);
            log.debug("Retrieving users with failed logins since: {}", sinceDate);
            
            List<UUID> users = auditService.getUsersWithFailedLogins(sinceDate);
            
            ApiResponseDto<List<UUID>> response = ApiResponseDto.success("Failed login users retrieved successfully", users)
                .withRequestId(requestId)
                .withVersion("v1")
                .withProcessingTime(System.currentTimeMillis() - startTime);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to retrieve users with failed logins: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponseDto.<List<UUID>>error("USER_RETRIEVAL_FAILED", "Failed to retrieve users with failed logins")
                    .withRequestId(requestId));
        }
    }

    // Helper method
    private String getRequestId() {
        String requestId = MDC.get("requestId");
        return requestId != null ? requestId : UUID.randomUUID().toString();
    }
}
