package com.mysillydreams.userservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Standardized error response format for MySillyDreams microservices
 * This format ensures consistent error handling across all services
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    /**
     * HTTP status code
     */
    private int status;

    /**
     * Error code for programmatic handling
     */
    private String errorCode;

    /**
     * Human-readable error message
     */
    private String message;

    /**
     * Detailed error description
     */
    private String details;

    /**
     * Service that generated the error
     */
    private String service;

    /**
     * Timestamp when the error occurred
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private LocalDateTime timestamp;

    /**
     * Request path that caused the error
     */
    private String path;

    /**
     * Correlation ID for tracing
     */
    private String correlationId;

    /**
     * Field-specific validation errors
     */
    private List<FieldError> fieldErrors;

    /**
     * Additional context information
     */
    private Map<String, Object> context;

    /**
     * Nested error from downstream service
     */
    private ErrorResponse cause;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FieldError {
        private String field;
        private String message;
        private Object rejectedValue;
    }

    /**
     * Common error codes for consistent handling
     */
    public static class ErrorCodes {
        // Authentication & Authorization
        public static final String UNAUTHORIZED = "AUTH_001";
        public static final String FORBIDDEN = "AUTH_002";
        public static final String INVALID_CREDENTIALS = "AUTH_003";
        public static final String TOKEN_EXPIRED = "AUTH_004";
        public static final String TOKEN_INVALID = "AUTH_005";

        // User Management
        public static final String USER_NOT_FOUND = "USER_001";
        public static final String USER_ALREADY_EXISTS = "USER_002";
        public static final String EMAIL_ALREADY_EXISTS = "USER_003";
        public static final String PHONE_ALREADY_EXISTS = "USER_004";
        public static final String INVALID_USER_DATA = "USER_005";

        // Validation
        public static final String VALIDATION_FAILED = "VAL_001";
        public static final String MISSING_REQUIRED_FIELD = "VAL_002";
        public static final String INVALID_FORMAT = "VAL_003";

        // External Services
        public static final String KEYCLOAK_ERROR = "EXT_001";
        public static final String VAULT_ERROR = "EXT_002";
        public static final String DATABASE_ERROR = "EXT_003";
        public static final String NETWORK_ERROR = "EXT_004";

        // System
        public static final String INTERNAL_ERROR = "SYS_001";
        public static final String SERVICE_UNAVAILABLE = "SYS_002";
        public static final String CONFIGURATION_ERROR = "SYS_003";
    }

    /**
     * Create a simple error response
     */
    public static ErrorResponse of(int status, String errorCode, String message, String service) {
        return ErrorResponse.builder()
                .status(status)
                .errorCode(errorCode)
                .message(message)
                .service(service)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Create an error response with details
     */
    public static ErrorResponse of(int status, String errorCode, String message, String details, String service, String path) {
        return ErrorResponse.builder()
                .status(status)
                .errorCode(errorCode)
                .message(message)
                .details(details)
                .service(service)
                .path(path)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Create a validation error response
     */
    public static ErrorResponse validationError(String message, List<FieldError> fieldErrors, String service, String path) {
        return ErrorResponse.builder()
                .status(400)
                .errorCode(ErrorCodes.VALIDATION_FAILED)
                .message(message)
                .service(service)
                .path(path)
                .fieldErrors(fieldErrors)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Create an internal server error response
     */
    public static ErrorResponse internalError(String message, String service, String path) {
        return ErrorResponse.builder()
                .status(500)
                .errorCode(ErrorCodes.INTERNAL_ERROR)
                .message(message)
                .service(service)
                .path(path)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
