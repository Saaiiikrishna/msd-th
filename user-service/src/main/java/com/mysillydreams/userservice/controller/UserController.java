package com.mysillydreams.userservice.controller;

import com.mysillydreams.userservice.dto.*;
import com.mysillydreams.userservice.security.InternalApiKeyFilter;
import com.mysillydreams.userservice.service.UserService;
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
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for User Management operations.
 * Provides comprehensive CRUD operations with role-based access control.
 */
@RestController
@RequestMapping("/api/v1/users")
@Validated
@Slf4j
@Tag(name = "User Management", description = "APIs for user CRUD operations, profile management, and user administration")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;
    private final ConsentService consentService;

    public UserController(UserService userService, ConsentService consentService) {
        this.userService = userService;
        this.consentService = consentService;
    }

    /**
     * Creates a new user account
     */
    @PostMapping
    @Operation(
        summary = "Create new user",
        description = "Creates a new user account with optional address and consent preferences"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "User created successfully",
            content = @Content(schema = @Schema(implementation = UserDto.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input data",
            content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
        @ApiResponse(responseCode = "409", description = "Email or phone already exists",
            content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions",
            content = @Content(schema = @Schema(implementation = ApiResponseDto.class)))
    })
    public ResponseEntity<ApiResponseDto<UserDto>> createUser(
            @Valid @RequestBody UserCreateRequestDto request,
            HttpServletRequest httpRequest) {
        
        long startTime = System.currentTimeMillis();
        String requestId = getRequestId();
        
        try {
            // Check if this is an internal service request
            boolean isInternalRequest = InternalApiKeyFilter.isInternalServiceRequest(httpRequest);

            log.info("🎯 === USER CREATION REQUEST ===");
            log.info("🎯 Email: {}", maskEmail(request.getEmail()));
            log.info("🎯 Name: {} {}", request.getFirstName(), request.getLastName());
            log.info("🎯 Request Type: {}", isInternalRequest ? "INTERNAL_SERVICE" : "EXTERNAL_API");
            log.info("🎯 Request ID: {}", requestId);

            if (isInternalRequest) {
                log.info("🔐 Internal service request detected - bypassing OAuth2 authentication");
                log.info("🔐 Source: Auth Service (user registration flow)");
            } else {
                log.info("🔑 External API request - OAuth2 JWT authentication required");
            }

            // Convert request DTO to service DTO
            UserDto userDto = request.toUserDto();

            // Create user
            log.info("📝 Calling UserService.createUser()...");
            UserDto createdUser = userService.createUser(userDto);

            log.info("✅ User created successfully in User Service");
            log.info("✅ User Reference ID: {}", createdUser.getReferenceId());

            // Process consents if provided
            if (request.getConsents() != null && !request.getConsents().isEmpty()) {
                log.info("📝 Processing {} consent(s)...", request.getConsents().size());
                List<ConsentDto> consentDtos = request.toConsentDtos();
                for (ConsentDto consentDto : consentDtos) {
                    try {
                        consentService.grantConsent(createdUser.getReferenceId(),
                            consentDto.getConsentKey(),
                            consentDto.getConsentVersion(),
                            null, // IP address - not available in this context
                            "USER_REGISTRATION"); // User agent
                        log.info("✅ Consent processed: {} = {}", consentDto.getConsentKey(), consentDto.getGranted());
                    } catch (Exception e) {
                        log.warn("⚠️ Failed to process consent {}: {}", consentDto.getConsentKey(), e.getMessage());
                    }
                }
            }

            log.info("✅ Processing Time: {}ms", System.currentTimeMillis() - startTime);

            // Build response
            ApiResponseDto<UserDto> response = ApiResponseDto.success("User created successfully", createdUser)
                .withRequestId(requestId)
                .withVersion("v1")
                .withProcessingTime(System.currentTimeMillis() - startTime);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (UserService.UserValidationException e) {
            log.warn("User creation validation failed: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponseDto.<UserDto>error("VALIDATION_ERROR", e.getMessage())
                    .withRequestId(requestId));
                    
        } catch (Exception e) {
            log.error("User creation failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponseDto.<UserDto>error("USER_CREATION_FAILED", "Failed to create user")
                    .withRequestId(requestId));
        }
    }

    /**
     * Retrieves user by reference ID
     */
    @GetMapping("/{referenceId}")
    @Operation(
        summary = "Get user by reference ID",
        description = "Retrieves user information with role-based field masking"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User found",
            content = @Content(schema = @Schema(implementation = UserDto.class))),
        @ApiResponse(responseCode = "404", description = "User not found",
            content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
        @ApiResponse(responseCode = "403", description = "Access denied",
            content = @Content(schema = @Schema(implementation = ApiResponseDto.class)))
    })
    public ResponseEntity<ApiResponseDto<UserDto>> getUserByReferenceId(
            @Parameter(description = "User reference ID", example = "01234567-89ab-cdef-0123-456789abcdef")
            @PathVariable @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$", message = "Invalid UUID format")
            String referenceId) {
        
        long startTime = System.currentTimeMillis();
        String requestId = getRequestId();
        
        try {
            log.debug("Retrieving user: {}", referenceId);
            
            UserDto user = userService.getUserByReferenceId(referenceId);
            
            ApiResponseDto<UserDto> response = ApiResponseDto.success("User retrieved successfully", user)
                .withRequestId(requestId)
                .withVersion("v1")
                .withProcessingTime(System.currentTimeMillis() - startTime);
            
            return ResponseEntity.ok(response);
            
        } catch (UserService.UserNotFoundException e) {
            log.warn("User not found: {}", referenceId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponseDto.<UserDto>error("USER_NOT_FOUND", "User not found")
                    .withRequestId(requestId));
                    
        } catch (Exception e) {
            log.error("Failed to retrieve user {}: {}", referenceId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponseDto.<UserDto>error("USER_RETRIEVAL_FAILED", "Failed to retrieve user")
                    .withRequestId(requestId));
        }
    }

    /**
     * Updates user information
     */
    @PutMapping("/{referenceId}")
    @Operation(
        summary = "Update user",
        description = "Updates user information with validation and audit trail"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User updated successfully",
            content = @Content(schema = @Schema(implementation = UserDto.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input data",
            content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
        @ApiResponse(responseCode = "404", description = "User not found",
            content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
        @ApiResponse(responseCode = "409", description = "Email or phone conflict",
            content = @Content(schema = @Schema(implementation = ApiResponseDto.class)))
    })
    public ResponseEntity<ApiResponseDto<UserDto>> updateUser(
            @Parameter(description = "User reference ID", example = "01234567-89ab-cdef-0123-456789abcdef")
            @PathVariable @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$", message = "Invalid UUID format")
            String referenceId,
            @Valid @RequestBody UserDto userDto) {
        
        long startTime = System.currentTimeMillis();
        String requestId = getRequestId();
        
        try {
            log.info("Updating user: {}", referenceId);
            
            UserDto updatedUser = userService.updateUser(referenceId, userDto);
            
            ApiResponseDto<UserDto> response = ApiResponseDto.success("User updated successfully", updatedUser)
                .withRequestId(requestId)
                .withVersion("v1")
                .withProcessingTime(System.currentTimeMillis() - startTime);
            
            return ResponseEntity.ok(response);
            
        } catch (UserService.UserNotFoundException e) {
            log.warn("User not found for update: {}", referenceId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponseDto.<UserDto>error("USER_NOT_FOUND", "User not found")
                    .withRequestId(requestId));
                    
        } catch (UserService.UserValidationException e) {
            log.warn("User update validation failed: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponseDto.<UserDto>error("VALIDATION_ERROR", e.getMessage())
                    .withRequestId(requestId));
                    
        } catch (Exception e) {
            log.error("Failed to update user {}: {}", referenceId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponseDto.<UserDto>error("USER_UPDATE_FAILED", "Failed to update user")
                    .withRequestId(requestId));
        }
    }

    /**
     * Soft deletes a user account
     */
    @DeleteMapping("/{referenceId}")
    @Operation(
        summary = "Delete user",
        description = "Soft deletes user account with audit trail"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User deleted successfully"),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
        @ApiResponse(responseCode = "409", description = "Cannot delete last admin user")
    })
    public ResponseEntity<ApiResponseDto<Void>> deleteUser(
            @Parameter(description = "User reference ID", example = "01234567-89ab-cdef-0123-456789abcdef")
            @PathVariable @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$", message = "Invalid UUID format")
            String referenceId,
            @Parameter(description = "Reason for deletion")
            @RequestParam(required = false) String reason) {
        
        long startTime = System.currentTimeMillis();
        String requestId = getRequestId();
        
        try {
            log.info("Deleting user: {} with reason: {}", referenceId, reason);
            
            userService.deleteUser(referenceId, reason);
            
            ApiResponseDto<Void> response = ApiResponseDto.<Void>success("User deleted successfully")
                .withRequestId(requestId)
                .withVersion("v1")
                .withProcessingTime(System.currentTimeMillis() - startTime);
            
            return ResponseEntity.ok(response);
            
        } catch (UserService.UserNotFoundException e) {
            log.warn("User not found for deletion: {}", referenceId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponseDto.<Void>error("USER_NOT_FOUND", "User not found")
                    .withRequestId(requestId));
                    
        } catch (UserService.UserServiceException e) {
            log.warn("User deletion failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponseDto.<Void>error("USER_DELETION_FAILED", e.getMessage())
                    .withRequestId(requestId));
                    
        } catch (Exception e) {
            log.error("Failed to delete user {}: {}", referenceId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponseDto.<Void>error("USER_DELETION_FAILED", "Failed to delete user")
                    .withRequestId(requestId));
        }
    }

    /**
     * Lists users with pagination and filtering
     */
    @GetMapping
    @Operation(
        summary = "List users",
        description = "Retrieves paginated list of users with optional filtering"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Users retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    public ResponseEntity<ApiResponseDto<Page<UserDto>>> listUsers(
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort field", example = "createdAt")
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction", example = "desc")
            @RequestParam(defaultValue = "desc") String sortDir,
            @Parameter(description = "Include inactive users")
            @RequestParam(defaultValue = "false") boolean includeInactive) {

        long startTime = System.currentTimeMillis();
        String requestId = getRequestId();

        try {
            log.debug("Listing users - page: {}, size: {}, includeInactive: {}", page, size, includeInactive);

            // Create pageable with sorting
            Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
            Pageable pageable = PageRequest.of(page, size, sort);

            Page<UserDto> users = userService.listUsers(pageable, includeInactive);

            // Create pagination info
            ApiResponseDto.PaginationInfo paginationInfo = new ApiResponseDto.PaginationInfo(
                users.getNumber(), users.getSize(), users.getTotalElements(), users.getTotalPages()
            );

            ApiResponseDto<Page<UserDto>> response = ApiResponseDto.success("Users retrieved successfully", users)
                .withRequestId(requestId)
                .withVersion("v1")
                .withProcessingTime(System.currentTimeMillis() - startTime)
                .withPagination(paginationInfo);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to list users: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponseDto.<Page<UserDto>>error("USER_LIST_FAILED", "Failed to retrieve users")
                    .withRequestId(requestId));
        }
    }

    /**
     * Searches users by partial reference ID
     */
    @GetMapping("/search")
    @Operation(
        summary = "Search users",
        description = "Searches users by partial reference ID"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Search completed successfully"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    public ResponseEntity<ApiResponseDto<Page<UserDto>>> searchUsers(
            @Parameter(description = "Partial reference ID to search", example = "USR123")
            @RequestParam String query,
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size) {

        long startTime = System.currentTimeMillis();
        String requestId = getRequestId();

        try {
            log.debug("Searching users with query: {}", query);

            Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
            Page<UserDto> users = userService.searchUsers(query, pageable);

            // Create pagination info
            ApiResponseDto.PaginationInfo paginationInfo = new ApiResponseDto.PaginationInfo(
                users.getNumber(), users.getSize(), users.getTotalElements(), users.getTotalPages()
            );

            ApiResponseDto<Page<UserDto>> response = ApiResponseDto.success("Search completed successfully", users)
                .withRequestId(requestId)
                .withVersion("v1")
                .withProcessingTime(System.currentTimeMillis() - startTime)
                .withPagination(paginationInfo);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("User search failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponseDto.<Page<UserDto>>error("USER_SEARCH_FAILED", "Failed to search users")
                    .withRequestId(requestId));
        }
    }

    /**
     * Reactivates a soft-deleted user
     */
    @PostMapping("/{referenceId}/reactivate")
    @Operation(
        summary = "Reactivate user",
        description = "Reactivates a soft-deleted user account (Admin only)"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User reactivated successfully"),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
        @ApiResponse(responseCode = "409", description = "User is already active")
    })
    public ResponseEntity<ApiResponseDto<UserDto>> reactivateUser(
            @Parameter(description = "User reference ID", example = "01234567-89ab-cdef-0123-456789abcdef")
            @PathVariable @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$", message = "Invalid UUID format")
            String referenceId) {

        long startTime = System.currentTimeMillis();
        String requestId = getRequestId();

        try {
            log.info("Reactivating user: {}", referenceId);

            UserDto reactivatedUser = userService.reactivateUser(referenceId);

            ApiResponseDto<UserDto> response = ApiResponseDto.success("User reactivated successfully", reactivatedUser)
                .withRequestId(requestId)
                .withVersion("v1")
                .withProcessingTime(System.currentTimeMillis() - startTime);

            return ResponseEntity.ok(response);

        } catch (UserService.UserNotFoundException e) {
            log.warn("User not found for reactivation: {}", referenceId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponseDto.<UserDto>error("USER_NOT_FOUND", "User not found")
                    .withRequestId(requestId));

        } catch (UserService.UserServiceException e) {
            log.warn("User reactivation failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponseDto.<UserDto>error("USER_REACTIVATION_FAILED", e.getMessage())
                    .withRequestId(requestId));

        } catch (Exception e) {
            log.error("Failed to reactivate user {}: {}", referenceId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponseDto.<UserDto>error("USER_REACTIVATION_FAILED", "Failed to reactivate user")
                    .withRequestId(requestId));
        }
    }

    /**
     * Gets dashboard data for a user
     */
    @GetMapping("/{referenceId}/dashboard")
    @Operation(
        summary = "Get user dashboard data",
        description = "Retrieves dashboard data including user profile and statistics"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Dashboard data retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<ApiResponseDto<Map<String, Object>>> getUserDashboard(
            @Parameter(description = "User reference ID", example = "01234567-89ab-cdef-0123-456789abcdef")
            @PathVariable @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$", message = "Invalid UUID format")
            String referenceId) {

        long startTime = System.currentTimeMillis();
        String requestId = getRequestId();

        try {
            log.debug("Retrieving dashboard data for user: {}", referenceId);

            UserDto user = userService.getUserByReferenceId(referenceId);

            // Create dashboard data
            Map<String, Object> dashboardData = Map.of(
                "user", Map.of(
                    "referenceId", user.getReferenceId(),
                    "firstName", user.getFirstName(),
                    "lastName", user.getLastName(),
                    "email", user.getEmail(),
                    "phone", user.getPhone(),
                    "active", user.getActive(),
                    "createdAt", user.getCreatedAt()
                ),
                "stats", Map.of(
                    "totalBookings", 0, // TODO: Get from booking service
                    "completedAdventures", 0, // TODO: Get from treasure service
                    "upcomingBookings", 0, // TODO: Get from booking service
                    "totalSpent", 0, // TODO: Get from payment service
                    "favoriteAdventures", 0, // TODO: Get from user preferences
                    "averageRating", 0.0, // TODO: Get from review service
                    "memberSince", user.getCreatedAt().getYear(),
                    "loyaltyPoints", 0 // TODO: Get from loyalty service
                ),
                "recentBookings", List.of(), // TODO: Get from booking service
                "upcomingAdventures", List.of() // TODO: Get from treasure service
            );

            ApiResponseDto<Map<String, Object>> response = ApiResponseDto.success("Dashboard data retrieved successfully", dashboardData)
                .withRequestId(requestId)
                .withVersion("v1")
                .withProcessingTime(System.currentTimeMillis() - startTime);

            return ResponseEntity.ok(response);

        } catch (UserService.UserNotFoundException e) {
            log.warn("User not found: {}", referenceId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponseDto.<Map<String, Object>>error("USER_NOT_FOUND", "User not found")
                    .withRequestId(requestId));
        } catch (Exception e) {
            log.error("Failed to retrieve dashboard data for user: {}", referenceId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponseDto.<Map<String, Object>>error("INTERNAL_ERROR", "Failed to retrieve dashboard data")
                    .withRequestId(requestId));
        }
    }

    // Helper methods
    private String getRequestId() {
        String requestId = MDC.get("requestId");
        return requestId != null ? requestId : UUID.randomUUID().toString();
    }

    private String maskEmail(String email) {
        if (email == null || email.length() < 3) {
            return "***";
        }
        int atIndex = email.indexOf('@');
        if (atIndex <= 0) {
            return "***";
        }
        return email.substring(0, Math.min(2, atIndex)) + "***@***";
    }
}
