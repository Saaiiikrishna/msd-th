package com.mysillydreams.userservice.controller;

import com.mysillydreams.userservice.dto.ApiResponseDto;
import com.mysillydreams.userservice.dto.ConsentDto;
import com.mysillydreams.userservice.service.ConsentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for Consent Management operations.
 * Provides GDPR/DPDP compliant consent management with audit trail.
 */
@RestController
@RequestMapping("/api/v1/users/{userReferenceId}/consents")
@Validated
@Slf4j
@Tag(name = "Consent Management", description = "APIs for GDPR/DPDP compliant consent management")
@SecurityRequirement(name = "bearerAuth")
public class ConsentController {

    private final ConsentService consentService;

    public ConsentController(ConsentService consentService) {
        this.consentService = consentService;
    }

    /**
     * Grants consent for a user
     */
    @PostMapping("/{consentKey}/grant")
    @Operation(
        summary = "Grant consent",
        description = "Grants consent for the specified user with full audit trail"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Consent granted successfully",
            content = @Content(schema = @Schema(implementation = ConsentDto.class))),
        @ApiResponse(responseCode = "400", description = "Invalid consent key",
            content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
        @ApiResponse(responseCode = "404", description = "User not found",
            content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
        @ApiResponse(responseCode = "403", description = "Access denied",
            content = @Content(schema = @Schema(implementation = ApiResponseDto.class)))
    })
    public ResponseEntity<ApiResponseDto<ConsentDto>> grantConsent(
            @Parameter(description = "User reference ID", example = "01234567-89ab-cdef-0123-456789abcdef")
            @PathVariable @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$", message = "Invalid UUID format")
            String userReferenceId,
            @Parameter(description = "Consent key", example = "marketing_emails")
            @PathVariable @NotBlank(message = "Consent key is required") 
            String consentKey,
            @Parameter(description = "Consent version", example = "v1.0")
            @RequestParam(required = false) String consentVersion,
            HttpServletRequest request) {
        
        long startTime = System.currentTimeMillis();
        String requestId = getRequestId();
        
        try {
            log.info("Granting consent {} for user: {}", consentKey, userReferenceId);
            
            // Extract request context
            InetAddress ipAddress = getClientIpAddress(request);
            String userAgent = request.getHeader("User-Agent");
            
            ConsentDto consent = consentService.grantConsent(userReferenceId, consentKey, 
                consentVersion, ipAddress, userAgent);
            
            ApiResponseDto<ConsentDto> response = ApiResponseDto.success("Consent granted successfully", consent)
                .withRequestId(requestId)
                .withVersion("v1")
                .withProcessingTime(System.currentTimeMillis() - startTime);
            
            return ResponseEntity.ok(response);
            
        } catch (ConsentService.ConsentServiceException e) {
            log.warn("Consent grant failed: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponseDto.<ConsentDto>error("CONSENT_GRANT_FAILED", e.getMessage())
                    .withRequestId(requestId));
                    
        } catch (Exception e) {
            log.error("Failed to grant consent {} for user {}: {}", consentKey, userReferenceId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponseDto.<ConsentDto>error("CONSENT_GRANT_FAILED", "Failed to grant consent")
                    .withRequestId(requestId));
        }
    }

    /**
     * Withdraws consent for a user
     */
    @PostMapping("/{consentKey}/withdraw")
    @Operation(
        summary = "Withdraw consent",
        description = "Withdraws consent for the specified user with full audit trail"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Consent withdrawn successfully",
            content = @Content(schema = @Schema(implementation = ConsentDto.class))),
        @ApiResponse(responseCode = "400", description = "Invalid consent key",
            content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
        @ApiResponse(responseCode = "404", description = "User or consent not found",
            content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
        @ApiResponse(responseCode = "403", description = "Access denied",
            content = @Content(schema = @Schema(implementation = ApiResponseDto.class)))
    })
    public ResponseEntity<ApiResponseDto<ConsentDto>> withdrawConsent(
            @Parameter(description = "User reference ID", example = "01234567-89ab-cdef-0123-456789abcdef")
            @PathVariable @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$", message = "Invalid UUID format")
            String userReferenceId,
            @Parameter(description = "Consent key", example = "marketing_emails")
            @PathVariable @NotBlank(message = "Consent key is required") 
            String consentKey,
            HttpServletRequest request) {
        
        long startTime = System.currentTimeMillis();
        String requestId = getRequestId();
        
        try {
            log.info("Withdrawing consent {} for user: {}", consentKey, userReferenceId);
            
            // Extract request context
            InetAddress ipAddress = getClientIpAddress(request);
            String userAgent = request.getHeader("User-Agent");
            
            ConsentDto consent = consentService.withdrawConsent(userReferenceId, consentKey, 
                ipAddress, userAgent);
            
            ApiResponseDto<ConsentDto> response = ApiResponseDto.success("Consent withdrawn successfully", consent)
                .withRequestId(requestId)
                .withVersion("v1")
                .withProcessingTime(System.currentTimeMillis() - startTime);
            
            return ResponseEntity.ok(response);
            
        } catch (ConsentService.ConsentServiceException e) {
            log.warn("Consent withdrawal failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponseDto.<ConsentDto>error("CONSENT_NOT_FOUND", e.getMessage())
                    .withRequestId(requestId));
                    
        } catch (Exception e) {
            log.error("Failed to withdraw consent {} for user {}: {}", consentKey, userReferenceId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponseDto.<ConsentDto>error("CONSENT_WITHDRAWAL_FAILED", "Failed to withdraw consent")
                    .withRequestId(requestId));
        }
    }

    /**
     * Gets all consents for a user
     */
    @GetMapping
    @Operation(
        summary = "Get user consents",
        description = "Retrieves all consent records for the specified user"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Consents retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<ApiResponseDto<List<ConsentDto>>> getUserConsents(
            @Parameter(description = "User reference ID", example = "01234567-89ab-cdef-0123-456789abcdef")
            @PathVariable @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$", message = "Invalid UUID format")
            String userReferenceId) {
        
        long startTime = System.currentTimeMillis();
        String requestId = getRequestId();
        
        try {
            log.debug("Retrieving consents for user: {}", userReferenceId);
            
            List<ConsentDto> consents = consentService.getUserConsents(userReferenceId);
            
            ApiResponseDto<List<ConsentDto>> response = ApiResponseDto.success("Consents retrieved successfully", consents)
                .withRequestId(requestId)
                .withVersion("v1")
                .withProcessingTime(System.currentTimeMillis() - startTime);
            
            return ResponseEntity.ok(response);
            
        } catch (ConsentService.ConsentServiceException e) {
            log.warn("Failed to retrieve consents for user {}: {}", userReferenceId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponseDto.<List<ConsentDto>>error("USER_NOT_FOUND", "User not found")
                    .withRequestId(requestId));
                    
        } catch (Exception e) {
            log.error("Failed to retrieve consents for user {}: {}", userReferenceId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponseDto.<List<ConsentDto>>error("CONSENT_RETRIEVAL_FAILED", "Failed to retrieve consents")
                    .withRequestId(requestId));
        }
    }

    /**
     * Gets active consents for a user
     */
    @GetMapping("/active")
    @Operation(
        summary = "Get active consents",
        description = "Retrieves only active consent records for the specified user"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Active consents retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<ApiResponseDto<List<ConsentDto>>> getUserActiveConsents(
            @Parameter(description = "User reference ID", example = "01234567-89ab-cdef-0123-456789abcdef")
            @PathVariable @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$", message = "Invalid UUID format")
            String userReferenceId) {
        
        long startTime = System.currentTimeMillis();
        String requestId = getRequestId();
        
        try {
            log.debug("Retrieving active consents for user: {}", userReferenceId);
            
            List<ConsentDto> consents = consentService.getUserActiveConsents(userReferenceId);
            
            ApiResponseDto<List<ConsentDto>> response = ApiResponseDto.success("Active consents retrieved successfully", consents)
                .withRequestId(requestId)
                .withVersion("v1")
                .withProcessingTime(System.currentTimeMillis() - startTime);
            
            return ResponseEntity.ok(response);
            
        } catch (ConsentService.ConsentServiceException e) {
            log.warn("Failed to retrieve active consents for user {}: {}", userReferenceId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponseDto.<List<ConsentDto>>error("USER_NOT_FOUND", "User not found")
                    .withRequestId(requestId));
                    
        } catch (Exception e) {
            log.error("Failed to retrieve active consents for user {}: {}", userReferenceId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponseDto.<List<ConsentDto>>error("CONSENT_RETRIEVAL_FAILED", "Failed to retrieve active consents")
                    .withRequestId(requestId));
        }
    }

    /**
     * Checks if user has granted a specific consent
     */
    @GetMapping("/{consentKey}/check")
    @Operation(
        summary = "Check consent status",
        description = "Checks if the user has granted a specific consent"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Consent status retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<ApiResponseDto<Map<String, Object>>> checkConsent(
            @Parameter(description = "User reference ID", example = "01234567-89ab-cdef-0123-456789abcdef")
            @PathVariable @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$", message = "Invalid UUID format")
            String userReferenceId,
            @Parameter(description = "Consent key", example = "marketing_emails")
            @PathVariable @NotBlank(message = "Consent key is required")
            String consentKey) {

        long startTime = System.currentTimeMillis();
        String requestId = getRequestId();

        try {
            log.debug("Checking consent {} for user: {}", consentKey, userReferenceId);

            boolean hasConsent = consentService.hasConsent(userReferenceId, consentKey);

            Map<String, Object> result = Map.of(
                "userReferenceId", userReferenceId,
                "consentKey", consentKey,
                "hasConsent", hasConsent
            );

            ApiResponseDto<Map<String, Object>> response = ApiResponseDto.success("Consent status retrieved successfully", result)
                .withRequestId(requestId)
                .withVersion("v1")
                .withProcessingTime(System.currentTimeMillis() - startTime);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to check consent {} for user {}: {}", consentKey, userReferenceId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponseDto.<Map<String,Object>>error("CONSENT_CHECK_FAILED", "Failed to check consent status")
                    .withRequestId(requestId));
        }
    }

    // Helper methods
    private String getRequestId() {
        String requestId = MDC.get("requestId");
        return requestId != null ? requestId : UUID.randomUUID().toString();
    }

    private InetAddress getClientIpAddress(HttpServletRequest request) {
        try {
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                return InetAddress.getByName(xForwardedFor.split(",")[0].trim());
            }

            String xRealIp = request.getHeader("X-Real-IP");
            if (xRealIp != null && !xRealIp.isEmpty()) {
                return InetAddress.getByName(xRealIp);
            }

            return InetAddress.getByName(request.getRemoteAddr());
        } catch (UnknownHostException e) {
            log.debug("Failed to parse IP address: {}", e.getMessage());
            try {
                return InetAddress.getByName("127.0.0.1");
            } catch (UnknownHostException ex) {
                return null;
            }
        }
    }
}
