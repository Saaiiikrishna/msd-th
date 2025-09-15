package com.mysillydreams.userservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Generic API response wrapper for consistent response format.
 * Provides standardized success/error responses across all endpoints.
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Standard API response wrapper")
public class ApiResponseDto<T> {

    @Schema(description = "Whether the request was successful", example = "true")
    private boolean success;

    @Schema(description = "Response message", example = "User created successfully")
    private String message;

    @Schema(description = "Response data payload")
    private T data;

    @Schema(description = "Error details if request failed")
    private ErrorDetails error;

    @Schema(description = "Response metadata")
    private ResponseMetadata metadata;

    @Schema(description = "Response timestamp", example = "2024-01-01T12:00:00")
    private LocalDateTime timestamp;

    // Constructors
    public ApiResponseDto() {
        this.timestamp = LocalDateTime.now();
    }

    public ApiResponseDto(boolean success, String message) {
        this();
        this.success = success;
        this.message = message;
    }

    public ApiResponseDto(boolean success, String message, T data) {
        this(success, message);
        this.data = data;
    }

    // Static factory methods for success responses
    public static <T> ApiResponseDto<T> success(T data) {
        return new ApiResponseDto<>(true, "Success", data);
    }

    public static <T> ApiResponseDto<T> success(String message, T data) {
        return new ApiResponseDto<>(true, message, data);
    }

    public static <T> ApiResponseDto<T> success(String message) {
        return new ApiResponseDto<>(true, message, null);
    }

    // Static factory methods for error responses
    public static <T> ApiResponseDto<T> error(String message) {
        ApiResponseDto<T> response = new ApiResponseDto<>(false, message);
        response.setError(new ErrorDetails("GENERAL_ERROR", message));
        return response;
    }

    public static <T> ApiResponseDto<T> error(String code, String message) {
        ApiResponseDto<T> response = new ApiResponseDto<>(false, message);
        response.setError(new ErrorDetails(code, message));
        return response;
    }

    public static <T> ApiResponseDto<T> error(String code, String message, Map<String, Object> details) {
        ApiResponseDto<T> response = new ApiResponseDto<>(false, message);
        response.setError(new ErrorDetails(code, message, details));
        return response;
    }

    public static <T> ApiResponseDto<T> validationError(List<ValidationError> validationErrors) {
        ApiResponseDto<T> response = new ApiResponseDto<>(false, "Validation failed");
        ErrorDetails error = new ErrorDetails("VALIDATION_ERROR", "Validation failed");
        error.setValidationErrors(validationErrors);
        response.setError(error);
        return response;
    }

    // Nested classes for error details
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Error details")
    public static class ErrorDetails {
        @Schema(description = "Error code", example = "USER_NOT_FOUND")
        private String code;

        @Schema(description = "Error message", example = "User not found with the provided ID")
        private String message;

        @Schema(description = "Additional error details")
        private Map<String, Object> details;

        @Schema(description = "Validation errors")
        private List<ValidationError> validationErrors;

        @Schema(description = "Stack trace (only in development)")
        private String stackTrace;

        public ErrorDetails() {}

        public ErrorDetails(String code, String message) {
            this.code = code;
            this.message = message;
        }

        public ErrorDetails(String code, String message, Map<String, Object> details) {
            this.code = code;
            this.message = message;
            this.details = details;
        }
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Validation error details")
    public static class ValidationError {
        @Schema(description = "Field name", example = "email")
        private String field;

        @Schema(description = "Rejected value", example = "invalid-email")
        private Object rejectedValue;

        @Schema(description = "Error message", example = "Invalid email format")
        private String message;

        @Schema(description = "Error code", example = "EMAIL_INVALID")
        private String code;

        public ValidationError() {}

        public ValidationError(String field, Object rejectedValue, String message) {
            this.field = field;
            this.rejectedValue = rejectedValue;
            this.message = message;
        }

        public ValidationError(String field, Object rejectedValue, String message, String code) {
            this.field = field;
            this.rejectedValue = rejectedValue;
            this.message = message;
            this.code = code;
        }
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Response metadata")
    public static class ResponseMetadata {
        @Schema(description = "Request ID for tracing", example = "req-123456")
        private String requestId;

        @Schema(description = "API version", example = "v1")
        private String version;

        @Schema(description = "Processing time in milliseconds", example = "150")
        private Long processingTimeMs;

        @Schema(description = "Rate limit information")
        private RateLimitInfo rateLimitInfo;

        @Schema(description = "Pagination information")
        private PaginationInfo paginationInfo;

        public ResponseMetadata() {}

        public ResponseMetadata(String requestId, String version) {
            this.requestId = requestId;
            this.version = version;
        }
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Rate limit information")
    public static class RateLimitInfo {
        @Schema(description = "Rate limit", example = "1000")
        private Integer limit;

        @Schema(description = "Remaining requests", example = "950")
        private Integer remaining;

        @Schema(description = "Reset time in seconds", example = "3600")
        private Long resetTime;

        public RateLimitInfo() {}

        public RateLimitInfo(Integer limit, Integer remaining, Long resetTime) {
            this.limit = limit;
            this.remaining = remaining;
            this.resetTime = resetTime;
        }
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Pagination information")
    public static class PaginationInfo {
        @Schema(description = "Current page number", example = "1")
        private Integer page;

        @Schema(description = "Page size", example = "20")
        private Integer size;

        @Schema(description = "Total elements", example = "100")
        private Long totalElements;

        @Schema(description = "Total pages", example = "5")
        private Integer totalPages;

        @Schema(description = "Whether this is the first page", example = "true")
        private Boolean first;

        @Schema(description = "Whether this is the last page", example = "false")
        private Boolean last;

        @Schema(description = "Whether there is a next page", example = "true")
        private Boolean hasNext;

        @Schema(description = "Whether there is a previous page", example = "false")
        private Boolean hasPrevious;

        public PaginationInfo() {}

        public PaginationInfo(Integer page, Integer size, Long totalElements, Integer totalPages) {
            this.page = page;
            this.size = size;
            this.totalElements = totalElements;
            this.totalPages = totalPages;
            this.first = page == 0;
            this.last = page == totalPages - 1;
            this.hasNext = page < totalPages - 1;
            this.hasPrevious = page > 0;
        }
    }

    // Helper methods
    public ApiResponseDto<T> withMetadata(ResponseMetadata metadata) {
        this.metadata = metadata;
        return this;
    }

    public ApiResponseDto<T> withRequestId(String requestId) {
        if (this.metadata == null) {
            this.metadata = new ResponseMetadata();
        }
        this.metadata.setRequestId(requestId);
        return this;
    }

    public ApiResponseDto<T> withVersion(String version) {
        if (this.metadata == null) {
            this.metadata = new ResponseMetadata();
        }
        this.metadata.setVersion(version);
        return this;
    }

    public ApiResponseDto<T> withProcessingTime(Long processingTimeMs) {
        if (this.metadata == null) {
            this.metadata = new ResponseMetadata();
        }
        this.metadata.setProcessingTimeMs(processingTimeMs);
        return this;
    }

    public ApiResponseDto<T> withPagination(PaginationInfo paginationInfo) {
        if (this.metadata == null) {
            this.metadata = new ResponseMetadata();
        }
        this.metadata.setPaginationInfo(paginationInfo);
        return this;
    }
}
