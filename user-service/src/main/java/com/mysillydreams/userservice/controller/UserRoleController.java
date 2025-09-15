package com.mysillydreams.userservice.controller;

import com.mysillydreams.userservice.dto.ApiResponseDto;
import com.mysillydreams.userservice.service.UserRoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * REST Controller for User Role Management operations.
 * Provides role assignment, removal, and querying with admin access control.
 */
@RestController
@RequestMapping("/api/v1/users/{userReferenceId}/roles")
@Validated
@Slf4j
@Tag(name = "User Role Management", description = "APIs for managing user roles and permissions")
@SecurityRequirement(name = "bearerAuth")
public class UserRoleController {

    private final UserRoleService userRoleService;

    public UserRoleController(UserRoleService userRoleService) {
        this.userRoleService = userRoleService;
    }

    /**
     * Assigns a role to a user
     */
    @PostMapping("/{role}")
    @Operation(
        summary = "Assign role to user",
        description = "Assigns a role to the specified user with optional expiration (Admin only)"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Role assigned successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid role or user",
            content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
        @ApiResponse(responseCode = "404", description = "User not found",
            content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions",
            content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
        @ApiResponse(responseCode = "409", description = "Role already assigned",
            content = @Content(schema = @Schema(implementation = ApiResponseDto.class)))
    })
    public ResponseEntity<ApiResponseDto<Void>> assignRole(
            @Parameter(description = "User reference ID", example = "USR12345678")
            @PathVariable @Pattern(regexp = "^USR[0-9]{8,12}$", message = "Invalid reference ID format") 
            String userReferenceId,
            @Parameter(description = "Role to assign", example = "ROLE_CUSTOMER")
            @PathVariable @NotBlank(message = "Role is required") 
            String role,
            @Parameter(description = "Role expiration date (optional)", example = "2024-12-31T23:59:59")
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) 
            LocalDateTime expiresAt) {
        
        long startTime = System.currentTimeMillis();
        String requestId = getRequestId();
        
        try {
            log.info("Assigning role {} to user: {} with expiration: {}", role, userReferenceId, expiresAt);
            
            userRoleService.assignRole(userReferenceId, role, expiresAt);
            
            ApiResponseDto<Void> response = ApiResponseDto.<Void>success("Role assigned successfully")
                .withRequestId(requestId)
                .withVersion("v1")
                .withProcessingTime(System.currentTimeMillis() - startTime);
            
            return ResponseEntity.ok(response);
            
        } catch (UserRoleService.RoleServiceException e) {
            log.warn("Role assignment failed: {}", e.getMessage());
            
            if (e.getMessage().contains("already assigned")) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponseDto.<Void>error("ROLE_ALREADY_ASSIGNED", e.getMessage())
                        .withRequestId(requestId));
            } else if (e.getMessage().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponseDto.<Void>error("USER_NOT_FOUND", e.getMessage())
                        .withRequestId(requestId));
            } else {
                return ResponseEntity.badRequest()
                    .body(ApiResponseDto.<Void>error("ROLE_ASSIGNMENT_FAILED", e.getMessage())
                        .withRequestId(requestId));
            }
            
        } catch (Exception e) {
            log.error("Failed to assign role {} to user {}: {}", role, userReferenceId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponseDto.<Void>error("ROLE_ASSIGNMENT_FAILED", "Failed to assign role")
                    .withRequestId(requestId));
        }
    }

    /**
     * Removes a role from a user
     */
    @DeleteMapping("/{role}")
    @Operation(
        summary = "Remove role from user",
        description = "Removes a role from the specified user (Admin only)"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Role removed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid role or user"),
        @ApiResponse(responseCode = "404", description = "User or role not found"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
        @ApiResponse(responseCode = "409", description = "Cannot remove last admin role")
    })
    public ResponseEntity<ApiResponseDto<Void>> removeRole(
            @Parameter(description = "User reference ID", example = "USR12345678")
            @PathVariable @Pattern(regexp = "^USR[0-9]{8,12}$", message = "Invalid reference ID format") 
            String userReferenceId,
            @Parameter(description = "Role to remove", example = "ROLE_CUSTOMER")
            @PathVariable @NotBlank(message = "Role is required") 
            String role) {
        
        long startTime = System.currentTimeMillis();
        String requestId = getRequestId();
        
        try {
            log.info("Removing role {} from user: {}", role, userReferenceId);
            
            userRoleService.removeRole(userReferenceId, role);
            
            ApiResponseDto<Void> response = ApiResponseDto.<Void>success("Role removed successfully")
                .withRequestId(requestId)
                .withVersion("v1")
                .withProcessingTime(System.currentTimeMillis() - startTime);
            
            return ResponseEntity.ok(response);
            
        } catch (UserRoleService.RoleServiceException e) {
            log.warn("Role removal failed: {}", e.getMessage());
            
            if (e.getMessage().contains("not assigned") || e.getMessage().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponseDto.<Void>error("ROLE_NOT_FOUND", e.getMessage())
                        .withRequestId(requestId));
            } else if (e.getMessage().contains("last admin")) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponseDto.<Void>error("CANNOT_REMOVE_LAST_ADMIN", e.getMessage())
                        .withRequestId(requestId));
            } else {
                return ResponseEntity.badRequest()
                    .body(ApiResponseDto.<Void>error("ROLE_REMOVAL_FAILED", e.getMessage())
                        .withRequestId(requestId));
            }
            
        } catch (Exception e) {
            log.error("Failed to remove role {} from user {}: {}", role, userReferenceId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponseDto.<Void>error("ROLE_REMOVAL_FAILED", "Failed to remove role")
                    .withRequestId(requestId));
        }
    }

    /**
     * Gets all roles for a user
     */
    @GetMapping
    @Operation(
        summary = "Get user roles",
        description = "Retrieves all active roles for the specified user"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Roles retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<ApiResponseDto<Set<String>>> getUserRoles(
            @Parameter(description = "User reference ID", example = "USR12345678")
            @PathVariable @Pattern(regexp = "^USR[0-9]{8,12}$", message = "Invalid reference ID format") 
            String userReferenceId) {
        
        long startTime = System.currentTimeMillis();
        String requestId = getRequestId();
        
        try {
            log.debug("Retrieving roles for user: {}", userReferenceId);
            
            Set<String> roles = userRoleService.getUserRoles(userReferenceId);
            
            ApiResponseDto<Set<String>> response = ApiResponseDto.success("Roles retrieved successfully", roles)
                .withRequestId(requestId)
                .withVersion("v1")
                .withProcessingTime(System.currentTimeMillis() - startTime);
            
            return ResponseEntity.ok(response);
            
        } catch (UserRoleService.RoleServiceException e) {
            log.warn("Failed to retrieve roles for user {}: {}", userReferenceId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponseDto.<Set<String>>error("USER_NOT_FOUND", "User not found")
                    .withRequestId(requestId));
                    
        } catch (Exception e) {
            log.error("Failed to retrieve roles for user {}: {}", userReferenceId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponseDto.<Set<String>>error("ROLE_RETRIEVAL_FAILED", "Failed to retrieve roles")
                    .withRequestId(requestId));
        }
    }

    /**
     * Checks if user has a specific role
     */
    @GetMapping("/{role}/check")
    @Operation(
        summary = "Check user role",
        description = "Checks if the user has a specific role"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Role check completed successfully"),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<ApiResponseDto<Map<String, Object>>> checkUserRole(
            @Parameter(description = "User reference ID", example = "USR12345678")
            @PathVariable @Pattern(regexp = "^USR[0-9]{8,12}$", message = "Invalid reference ID format") 
            String userReferenceId,
            @Parameter(description = "Role to check", example = "ROLE_CUSTOMER")
            @PathVariable @NotBlank(message = "Role is required") 
            String role) {
        
        long startTime = System.currentTimeMillis();
        String requestId = getRequestId();
        
        try {
            log.debug("Checking role {} for user: {}", role, userReferenceId);
            
            boolean hasRole = userRoleService.hasRole(userReferenceId, role);
            
            Map<String, Object> result = Map.of(
                "userReferenceId", userReferenceId,
                "role", role,
                "hasRole", hasRole
            );
            
            ApiResponseDto<Map<String, Object>> response = ApiResponseDto.success("Role check completed successfully", result)
                .withRequestId(requestId)
                .withVersion("v1")
                .withProcessingTime(System.currentTimeMillis() - startTime);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to check role {} for user {}: {}", role, userReferenceId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponseDto.<Map<String,Object>>error("ROLE_CHECK_FAILED", "Failed to check role")
                    .withRequestId(requestId));
        }
    }

    // Helper method
    private String getRequestId() {
        String requestId = MDC.get("requestId");
        return requestId != null ? requestId : UUID.randomUUID().toString();
    }
}
