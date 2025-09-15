package com.mysillydreams.userservice.controller;

import com.mysillydreams.userservice.dto.AddressDto;
import com.mysillydreams.userservice.dto.ApiResponseDto;
import com.mysillydreams.userservice.service.AddressService;
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
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for Address Management operations.
 * Provides CRUD operations for user addresses with PII encryption.
 */
@RestController
@RequestMapping("/api/v1/users/{userReferenceId}/addresses")
@Validated
@Slf4j
@Tag(name = "Address Management", description = "APIs for managing user addresses with PII encryption")
@SecurityRequirement(name = "bearerAuth")
public class AddressController {

    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    /**
     * Adds a new address for a user
     */
    @PostMapping
    @Operation(
        summary = "Add user address",
        description = "Adds a new address for the specified user with PII encryption"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Address created successfully",
            content = @Content(schema = @Schema(implementation = AddressDto.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input data",
            content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
        @ApiResponse(responseCode = "404", description = "User not found",
            content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
        @ApiResponse(responseCode = "403", description = "Access denied",
            content = @Content(schema = @Schema(implementation = ApiResponseDto.class)))
    })
    public ResponseEntity<ApiResponseDto<AddressDto>> addAddress(
            @Parameter(description = "User reference ID", example = "USR12345678")
            @PathVariable @Pattern(regexp = "^USR[0-9]{8,12}$", message = "Invalid reference ID format") 
            String userReferenceId,
            @Valid @RequestBody AddressDto addressDto) {
        
        long startTime = System.currentTimeMillis();
        String requestId = getRequestId();
        
        try {
            log.info("Adding address for user: {}", userReferenceId);
            
            AddressDto createdAddress = addressService.addAddress(userReferenceId, addressDto);
            
            ApiResponseDto<AddressDto> response = ApiResponseDto.success("Address created successfully", createdAddress)
                .withRequestId(requestId)
                .withVersion("v1")
                .withProcessingTime(System.currentTimeMillis() - startTime);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (AddressService.AddressValidationException e) {
            log.warn("Address creation validation failed: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponseDto.<AddressDto>error("VALIDATION_ERROR", e.getMessage())
                    .withRequestId(requestId));
                    
        } catch (AddressService.AddressServiceException e) {
            log.warn("Address creation failed: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponseDto.<AddressDto>error("ADDRESS_CREATION_FAILED", e.getMessage())
                    .withRequestId(requestId));
                    
        } catch (Exception e) {
            log.error("Failed to create address for user {}: {}", userReferenceId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponseDto.<AddressDto>error("ADDRESS_CREATION_FAILED", "Failed to create address")
                    .withRequestId(requestId));
        }
    }

    /**
     * Gets all addresses for a user
     */
    @GetMapping
    @Operation(
        summary = "Get user addresses",
        description = "Retrieves all addresses for the specified user with role-based masking"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Addresses retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<ApiResponseDto<List<AddressDto>>> getUserAddresses(
            @Parameter(description = "User reference ID", example = "USR12345678")
            @PathVariable @Pattern(regexp = "^USR[0-9]{8,12}$", message = "Invalid reference ID format") 
            String userReferenceId) {
        
        long startTime = System.currentTimeMillis();
        String requestId = getRequestId();
        
        try {
            log.debug("Retrieving addresses for user: {}", userReferenceId);
            
            List<AddressDto> addresses = addressService.getUserAddresses(userReferenceId);
            
            ApiResponseDto<List<AddressDto>> response = ApiResponseDto.success("Addresses retrieved successfully", addresses)
                .withRequestId(requestId)
                .withVersion("v1")
                .withProcessingTime(System.currentTimeMillis() - startTime);
            
            return ResponseEntity.ok(response);
            
        } catch (AddressService.AddressServiceException e) {
            log.warn("Failed to retrieve addresses for user {}: {}", userReferenceId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponseDto.<List<AddressDto>>error("USER_NOT_FOUND", "User not found")
                    .withRequestId(requestId));
                    
        } catch (Exception e) {
            log.error("Failed to retrieve addresses for user {}: {}", userReferenceId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponseDto.<List<AddressDto>>error("ADDRESS_RETRIEVAL_FAILED", "Failed to retrieve addresses")
                    .withRequestId(requestId));
        }
    }

    /**
     * Gets primary address for a user
     */
    @GetMapping("/primary")
    @Operation(
        summary = "Get primary address",
        description = "Retrieves the primary address for the specified user"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Primary address retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "User or primary address not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<ApiResponseDto<AddressDto>> getPrimaryAddress(
            @Parameter(description = "User reference ID", example = "USR12345678")
            @PathVariable @Pattern(regexp = "^USR[0-9]{8,12}$", message = "Invalid reference ID format") 
            String userReferenceId) {
        
        long startTime = System.currentTimeMillis();
        String requestId = getRequestId();
        
        try {
            log.debug("Retrieving primary address for user: {}", userReferenceId);
            
            AddressDto primaryAddress = addressService.getPrimaryAddress(userReferenceId);
            
            if (primaryAddress == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponseDto.<AddressDto>error("PRIMARY_ADDRESS_NOT_FOUND", "Primary address not found")
                        .withRequestId(requestId));
            }
            
            ApiResponseDto<AddressDto> response = ApiResponseDto.success("Primary address retrieved successfully", primaryAddress)
                .withRequestId(requestId)
                .withVersion("v1")
                .withProcessingTime(System.currentTimeMillis() - startTime);
            
            return ResponseEntity.ok(response);
            
        } catch (AddressService.AddressServiceException e) {
            log.warn("Failed to retrieve primary address for user {}: {}", userReferenceId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponseDto.<AddressDto>error("USER_NOT_FOUND", "User not found")
                    .withRequestId(requestId));
                    
        } catch (Exception e) {
            log.error("Failed to retrieve primary address for user {}: {}", userReferenceId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponseDto.<AddressDto>error("ADDRESS_RETRIEVAL_FAILED", "Failed to retrieve primary address")
                    .withRequestId(requestId));
        }
    }

    /**
     * Updates an existing address
     */
    @PutMapping("/{addressId}")
    @Operation(
        summary = "Update address",
        description = "Updates an existing address with validation and audit trail"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Address updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "404", description = "User or address not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<ApiResponseDto<AddressDto>> updateAddress(
            @Parameter(description = "User reference ID", example = "USR12345678")
            @PathVariable @Pattern(regexp = "^USR[0-9]{8,12}$", message = "Invalid reference ID format") 
            String userReferenceId,
            @Parameter(description = "Address ID")
            @PathVariable UUID addressId,
            @Valid @RequestBody AddressDto addressDto) {
        
        long startTime = System.currentTimeMillis();
        String requestId = getRequestId();
        
        try {
            log.info("Updating address {} for user: {}", addressId, userReferenceId);
            
            AddressDto updatedAddress = addressService.updateAddress(userReferenceId, addressId, addressDto);
            
            ApiResponseDto<AddressDto> response = ApiResponseDto.success("Address updated successfully", updatedAddress)
                .withRequestId(requestId)
                .withVersion("v1")
                .withProcessingTime(System.currentTimeMillis() - startTime);
            
            return ResponseEntity.ok(response);
            
        } catch (AddressService.AddressValidationException e) {
            log.warn("Address update validation failed: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponseDto.<AddressDto>error("VALIDATION_ERROR", e.getMessage())
                    .withRequestId(requestId));
                    
        } catch (AddressService.AddressServiceException e) {
            log.warn("Address update failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponseDto.<AddressDto>error("ADDRESS_NOT_FOUND", e.getMessage())
                    .withRequestId(requestId));
                    
        } catch (Exception e) {
            log.error("Failed to update address {} for user {}: {}", addressId, userReferenceId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponseDto.<AddressDto>error("ADDRESS_UPDATE_FAILED", "Failed to update address")
                    .withRequestId(requestId));
        }
    }

    /**
     * Deletes an address
     */
    @DeleteMapping("/{addressId}")
    @Operation(
        summary = "Delete address",
        description = "Deletes an address with automatic primary reassignment if needed"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Address deleted successfully"),
        @ApiResponse(responseCode = "404", description = "User or address not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<ApiResponseDto<Void>> deleteAddress(
            @Parameter(description = "User reference ID", example = "USR12345678")
            @PathVariable @Pattern(regexp = "^USR[0-9]{8,12}$", message = "Invalid reference ID format")
            String userReferenceId,
            @Parameter(description = "Address ID")
            @PathVariable UUID addressId) {

        long startTime = System.currentTimeMillis();
        String requestId = getRequestId();

        try {
            log.info("Deleting address {} for user: {}", addressId, userReferenceId);

            addressService.deleteAddress(userReferenceId, addressId);

            ApiResponseDto<Void> response = ApiResponseDto.<Void>success("Address deleted successfully")
                .withRequestId(requestId)
                .withVersion("v1")
                .withProcessingTime(System.currentTimeMillis() - startTime);

            return ResponseEntity.ok(response);

        } catch (AddressService.AddressServiceException e) {
            log.warn("Address deletion failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponseDto.<Void>error("ADDRESS_NOT_FOUND", e.getMessage())
                    .withRequestId(requestId));

        } catch (Exception e) {
            log.error("Failed to delete address {} for user {}: {}", addressId, userReferenceId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponseDto.<Void>error("ADDRESS_DELETION_FAILED", "Failed to delete address")
                    .withRequestId(requestId));
        }
    }

    /**
     * Sets an address as primary
     */
    @PostMapping("/{addressId}/set-primary")
    @Operation(
        summary = "Set primary address",
        description = "Sets the specified address as the primary address for the user"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Primary address set successfully"),
        @ApiResponse(responseCode = "404", description = "User or address not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<ApiResponseDto<Void>> setPrimaryAddress(
            @Parameter(description = "User reference ID", example = "USR12345678")
            @PathVariable @Pattern(regexp = "^USR[0-9]{8,12}$", message = "Invalid reference ID format")
            String userReferenceId,
            @Parameter(description = "Address ID")
            @PathVariable UUID addressId) {

        long startTime = System.currentTimeMillis();
        String requestId = getRequestId();

        try {
            log.info("Setting primary address {} for user: {}", addressId, userReferenceId);

            addressService.setPrimaryAddress(userReferenceId, addressId);

            ApiResponseDto<Void> response = ApiResponseDto.<Void>success("Primary address set successfully")
                .withRequestId(requestId)
                .withVersion("v1")
                .withProcessingTime(System.currentTimeMillis() - startTime);

            return ResponseEntity.ok(response);

        } catch (AddressService.AddressServiceException e) {
            log.warn("Failed to set primary address: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponseDto.<Void>error("ADDRESS_NOT_FOUND", e.getMessage())
                    .withRequestId(requestId));

        } catch (Exception e) {
            log.error("Failed to set primary address {} for user {}: {}", addressId, userReferenceId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponseDto.<Void>error("PRIMARY_ADDRESS_UPDATE_FAILED", "Failed to set primary address")
                    .withRequestId(requestId));
        }
    }

    // Helper method
    private String getRequestId() {
        String requestId = MDC.get("requestId");
        return requestId != null ? requestId : UUID.randomUUID().toString();
    }
}
