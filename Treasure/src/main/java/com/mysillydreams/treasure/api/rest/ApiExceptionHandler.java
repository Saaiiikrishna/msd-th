package com.mysillydreams.treasure.api.rest;

import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<?> illegalState(IllegalStateException ex) {
        return problem(HttpStatus.CONFLICT, "conflict", ex.getMessage());
    }

    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class})
    public ResponseEntity<?> badRequest(Exception ex) {
        return problem(HttpStatus.BAD_REQUEST, "bad_request", ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> unknown(Exception ex) {
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "error", "Unexpected error");
    }

    private ResponseEntity<Map<String,Object>> problem(HttpStatus status, String code, String detail) {
        return ResponseEntity.status(status).body(Map.of(
                "type","about:blank",
                "title", status.getReasonPhrase(),
                "status", status.value(),
                "code", code,
                "detail", detail,
                "timestamp", OffsetDateTime.now().toString()
        ));
    }
}
