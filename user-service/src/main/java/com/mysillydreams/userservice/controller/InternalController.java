package com.mysillydreams.userservice.controller;

import com.mysillydreams.userservice.dto.ApiResponseDto;
import com.mysillydreams.userservice.dto.UserDto;
import com.mysillydreams.userservice.service.UserLookupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Internal REST Controller for User Service operations.
 * Provides internal APIs for other microservices with minimal user data.
 * These endpoints are secured for internal service-to-service communication only.
 */
@RestController
@RequestMapping("/api/v1/internal/users")
@Validated
@Slf4j
@Tag(name = "Internal User APIs", description = "Internal APIs for service-to-service user data access")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('INTERNAL_CONSUMER')")
public class InternalController {

    private final UserLookupService userLookupService;

    public InternalController(UserLookupService userLookupService) {
        this.userLookupService = userLookupService;
    }

    /**
     * Bulk lookup users by multiple criteria (HMAC-based search)
     * Used by other services like Treasure Service for user enrichment
     */
    @PostMapping("/bulk-lookup")
    @Operation(
        summary = "Bulk lookup users",
        description = "Performs bulk user lookup using HMAC-indexed search for emails/phones and user references. Returns minimal user cards for performance."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Bulk lookup completed successfully",
            content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
        @ApiResponse(responseCode = "400", description = "Invalid lookup request"),
        @ApiResponse(responseCode = "403", description = "Internal service access required")
    })
    public ResponseEntity<ApiResponseDto<List<UserDto>>> bulkLookup(
            @Valid @RequestBody BulkLookupRequestDto request) {
        
        long startTime = System.currentTimeMillis();
        String requestId = getRequestId();
        
        try {
            log.debug("Processing bulk lookup request with {} criteria", request.getCriteriaCount());
            
            // Convert DTO to service request
            UserLookupService.BulkLookupRequest serviceRequest = request.toServiceRequest();
            
            // Perform bulk lookup
            List<UserDto> users = userLookupService.bulkLookup(serviceRequest);
            
            log.debug("Bulk lookup returned {} users", users.size());
            
            ApiResponseDto<List<UserDto>> response = ApiResponseDto.success(
                "Bulk lookup completed successfully", users)
                .withRequestId(requestId)
                .withVersion("v1")
                .withProcessingTime(System.currentTimeMillis() - startTime);
            
            return ResponseEntity.ok(response);
            
        } catch (UserLookupService.UserLookupException e) {
            log.warn("Bulk lookup failed: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponseDto.<List<UserDto>>error("BULK_LOOKUP_FAILED", e.getMessage())
                    .withRequestId(requestId));
                    
        } catch (Exception e) {
            log.error("Bulk lookup error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponseDto.<List<UserDto>>error("BULK_LOOKUP_ERROR", "Internal lookup error")
                    .withRequestId(requestId));
        }
    }

    /**
     * Get minimal user information by user reference
     * Used by other services for user data enrichment with minimal PII exposure
     */
    @GetMapping("/{userRef}/minimal")
    @Operation(
        summary = "Get minimal user info",
        description = "Retrieves minimal user information (masked PII) for internal service use"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User info retrieved successfully",
            content = @Content(schema = @Schema(implementation = UserDto.class))),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "403", description = "Internal service access required")
    })
    public ResponseEntity<ApiResponseDto<UserDto>> getMinimalUserInfo(
            @Parameter(description = "User reference ID", example = "01234567-89ab-cdef-0123-456789abcdef")
            @PathVariable @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$", 
                message = "Invalid user reference format") 
            String userRef) {
        
        long startTime = System.currentTimeMillis();
        String requestId = getRequestId();
        
        try {
            log.debug("Getting minimal user info for reference: {}", userRef);
            
            Optional<UserDto> userOpt = userLookupService.getMinimalUserInfo(userRef);
            
            if (userOpt.isPresent()) {
                ApiResponseDto<UserDto> response = ApiResponseDto.success(
                    "User info retrieved successfully", userOpt.get())
                    .withRequestId(requestId)
                    .withVersion("v1")
                    .withProcessingTime(System.currentTimeMillis() - startTime);
                
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponseDto.<UserDto>error("USER_NOT_FOUND", "User not found")
                        .withRequestId(requestId));
            }
            
        } catch (UserLookupService.UserLookupException e) {
            log.warn("Minimal user lookup failed for {}: {}", userRef, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponseDto.<UserDto>error("USER_NOT_FOUND", "User not found")
                    .withRequestId(requestId));
                    
        } catch (Exception e) {
            log.error("Failed to get minimal user info for {}: {}", userRef, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponseDto.<UserDto>error("USER_LOOKUP_ERROR", "Internal lookup error")
                    .withRequestId(requestId));
        }
    }

    // Helper methods
    private String getRequestId() {
        String requestId = MDC.get("requestId");
        return requestId != null ? requestId : UUID.randomUUID().toString();
    }

    /**
     * DTO for bulk lookup requests
     */
    @Schema(description = "Bulk lookup request with multiple search criteria")
    public static class BulkLookupRequestDto {
        
        @Schema(description = "List of user reference IDs to lookup")
        private List<String> userReferenceIds;
        
        @Schema(description = "List of user UUIDs to lookup")
        private List<UUID> userIds;
        
        @Schema(description = "List of email addresses to lookup (will be HMAC-indexed)")
        private List<String> emails;
        
        @Schema(description = "List of phone numbers to lookup (will be HMAC-indexed)")
        private List<String> phones;

        // Constructors
        public BulkLookupRequestDto() {}

        public BulkLookupRequestDto(List<String> userReferenceIds, List<UUID> userIds, 
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

        public int getCriteriaCount() {
            int count = 0;
            if (userReferenceIds != null) count += userReferenceIds.size();
            if (userIds != null) count += userIds.size();
            if (emails != null) count += emails.size();
            if (phones != null) count += phones.size();
            return count;
        }

        public UserLookupService.BulkLookupRequest toServiceRequest() {
            return new UserLookupService.BulkLookupRequest(
                userReferenceIds, userIds, emails, phones);
        }
    }
}
