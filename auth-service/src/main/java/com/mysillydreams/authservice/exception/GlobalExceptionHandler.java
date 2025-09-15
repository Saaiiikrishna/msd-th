package com.mysillydreams.authservice.exception;

import com.mysillydreams.authservice.dto.ErrorResponse;
import com.mysillydreams.authservice.service.UserManagementService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Global exception handler for auth-service
 * Provides consistent error responses across all endpoints
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private static final String SERVICE_NAME = "auth-service";

    /**
     * Handle validation errors
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        
        String correlationId = UUID.randomUUID().toString();
        log.warn("[{}] Validation error on {}: {}", correlationId, request.getRequestURI(), ex.getMessage());

        List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> ErrorResponse.FieldError.builder()
                        .field(error.getField())
                        .message(error.getDefaultMessage())
                        .rejectedValue(error.getRejectedValue())
                        .build())
                .collect(Collectors.toList());

        ErrorResponse errorResponse = ErrorResponse.validationError(
                "Validation failed",
                fieldErrors,
                SERVICE_NAME,
                request.getRequestURI()
        );
        errorResponse.setCorrelationId(correlationId);

        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Handle bind exceptions
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorResponse> handleBindException(
            BindException ex, HttpServletRequest request) {
        
        String correlationId = UUID.randomUUID().toString();
        log.warn("[{}] Bind error on {}: {}", correlationId, request.getRequestURI(), ex.getMessage());

        List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> ErrorResponse.FieldError.builder()
                        .field(error.getField())
                        .message(error.getDefaultMessage())
                        .rejectedValue(error.getRejectedValue())
                        .build())
                .collect(Collectors.toList());

        ErrorResponse errorResponse = ErrorResponse.validationError(
                "Request binding failed",
                fieldErrors,
                SERVICE_NAME,
                request.getRequestURI()
        );
        errorResponse.setCorrelationId(correlationId);

        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Handle JSON parsing errors
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleJsonParsingError(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        
        String correlationId = UUID.randomUUID().toString();
        log.warn("[{}] JSON parsing error on {}: {}", correlationId, request.getRequestURI(), ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.of(
                400,
                ErrorResponse.ErrorCodes.INVALID_FORMAT,
                "Invalid request format",
                "The request body contains invalid JSON or missing required fields",
                SERVICE_NAME,
                request.getRequestURI()
        );
        errorResponse.setCorrelationId(correlationId);

        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Handle user creation exceptions
     */
    @ExceptionHandler(UserManagementService.UserCreationException.class)
    public ResponseEntity<ErrorResponse> handleUserCreationException(
            UserManagementService.UserCreationException ex, HttpServletRequest request) {
        
        String correlationId = UUID.randomUUID().toString();
        log.error("[{}] User creation error on {}: {}", correlationId, request.getRequestURI(), ex.getMessage());

        // Determine specific error code based on the exception message
        String errorCode = ErrorResponse.ErrorCodes.INTERNAL_ERROR;
        int status = 500;
        String message = "User registration failed";

        if (ex.getMessage().contains("already exists")) {
            if (ex.getMessage().contains("email")) {
                errorCode = ErrorResponse.ErrorCodes.EMAIL_ALREADY_EXISTS;
                message = "Email address is already registered";
                status = 409;
            } else if (ex.getMessage().contains("phone")) {
                errorCode = ErrorResponse.ErrorCodes.PHONE_ALREADY_EXISTS;
                message = "Phone number is already registered";
                status = 409;
            } else {
                errorCode = ErrorResponse.ErrorCodes.USER_ALREADY_EXISTS;
                message = "User already exists";
                status = 409;
            }
        } else if (ex.getMessage().contains("Keycloak")) {
            errorCode = ErrorResponse.ErrorCodes.KEYCLOAK_ERROR;
            message = "Authentication service error";
        } else if (ex.getMessage().contains("Vault") || ex.getMessage().contains("PII")) {
            errorCode = ErrorResponse.ErrorCodes.VAULT_ERROR;
            message = "Data encryption service error";
        } else if (ex.getMessage().contains("user-service")) {
            errorCode = ErrorResponse.ErrorCodes.SERVICE_UNAVAILABLE;
            message = "User service is currently unavailable";
            status = 503;
        }

        ErrorResponse errorResponse = ErrorResponse.of(
                status,
                errorCode,
                message,
                ex.getMessage(),
                SERVICE_NAME,
                request.getRequestURI()
        );
        errorResponse.setCorrelationId(correlationId);

        return ResponseEntity.status(status).body(errorResponse);
    }

    /**
     * Handle WebClient errors (from downstream services)
     */
    @ExceptionHandler(WebClientResponseException.class)
    public ResponseEntity<ErrorResponse> handleWebClientException(
            WebClientResponseException ex, HttpServletRequest request) {
        
        String correlationId = UUID.randomUUID().toString();
        log.error("[{}] Downstream service error on {}: {} - {}", 
                correlationId, request.getRequestURI(), ex.getStatusCode(), ex.getResponseBodyAsString());

        ErrorResponse errorResponse = ErrorResponse.of(
                ex.getStatusCode().value(),
                ErrorResponse.ErrorCodes.SERVICE_UNAVAILABLE,
                "Downstream service error",
                ex.getResponseBodyAsString(),
                SERVICE_NAME,
                request.getRequestURI()
        );
        errorResponse.setCorrelationId(correlationId);

        return ResponseEntity.status(ex.getStatusCode()).body(errorResponse);
    }

    /**
     * Handle all other exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {
        
        String correlationId = UUID.randomUUID().toString();
        log.error("[{}] Unexpected error on {}: {}", correlationId, request.getRequestURI(), ex.getMessage(), ex);

        ErrorResponse errorResponse = ErrorResponse.internalError(
                "An unexpected error occurred",
                SERVICE_NAME,
                request.getRequestURI()
        );
        errorResponse.setCorrelationId(correlationId);
        errorResponse.setDetails(ex.getMessage());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
