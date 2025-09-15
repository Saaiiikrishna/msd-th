package com.mysillydreams.userservice.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Filter for comprehensive request/response logging with performance metrics.
 * Provides detailed logging for debugging, monitoring, and audit purposes.
 */
@Component
@Order(1)
@Slf4j
public class RequestResponseLoggingFilter implements Filter {

    private static final String REQUEST_ID_HEADER = "X-Request-ID";
    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String USER_ID_HEADER = "X-User-ID";
    
    // Sensitive headers that should not be logged
    private static final String[] SENSITIVE_HEADERS = {
        "authorization", "cookie", "set-cookie", "x-auth-token", "x-api-key"
    };

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
            chain.doFilter(request, response);
            return;
        }

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Skip logging for health check and actuator endpoints
        String requestURI = httpRequest.getRequestURI();
        if (shouldSkipLogging(requestURI)) {
            chain.doFilter(request, response);
            return;
        }

        // Wrap request and response for content caching
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(httpRequest);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(httpResponse);

        // Setup MDC context
        setupMDCContext(wrappedRequest);

        long startTime = System.currentTimeMillis();
        LocalDateTime requestTime = LocalDateTime.now();

        try {
            // Log incoming request
            logRequest(wrappedRequest, requestTime);

            // Process the request
            chain.doFilter(wrappedRequest, wrappedResponse);

            // Calculate processing time
            long processingTime = System.currentTimeMillis() - startTime;

            // Log outgoing response
            logResponse(wrappedRequest, wrappedResponse, processingTime);

            // Copy response content back to original response
            wrappedResponse.copyBodyToResponse();

        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;
            logError(wrappedRequest, e, processingTime);
            throw e;
        } finally {
            // Clean up MDC
            clearMDCContext();
        }
    }

    private void setupMDCContext(HttpServletRequest request) {
        // Request ID
        String requestId = request.getHeader(REQUEST_ID_HEADER);
        if (requestId == null || requestId.trim().isEmpty()) {
            requestId = UUID.randomUUID().toString();
        }
        MDC.put("requestId", requestId);

        // Correlation ID
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        if (correlationId != null && !correlationId.trim().isEmpty()) {
            MDC.put("correlationId", correlationId);
        }

        // User ID
        String userId = request.getHeader(USER_ID_HEADER);
        if (userId != null && !userId.trim().isEmpty()) {
            MDC.put("userId", userId);
        }

        // Request details
        MDC.put("method", request.getMethod());
        MDC.put("uri", request.getRequestURI());
        MDC.put("remoteAddr", getClientIpAddress(request));
        MDC.put("userAgent", request.getHeader("User-Agent"));
    }

    private void clearMDCContext() {
        MDC.remove("requestId");
        MDC.remove("correlationId");
        MDC.remove("userId");
        MDC.remove("method");
        MDC.remove("uri");
        MDC.remove("remoteAddr");
        MDC.remove("userAgent");
    }

    private void logRequest(ContentCachingRequestWrapper request, LocalDateTime requestTime) {
        try {
            Map<String, Object> requestLog = new HashMap<>();
            requestLog.put("type", "REQUEST");
            requestLog.put("timestamp", requestTime);
            requestLog.put("method", request.getMethod());
            requestLog.put("uri", request.getRequestURI());
            requestLog.put("queryString", request.getQueryString());
            requestLog.put("remoteAddr", getClientIpAddress(request));
            requestLog.put("userAgent", request.getHeader("User-Agent"));
            requestLog.put("contentType", request.getContentType());
            requestLog.put("contentLength", request.getContentLength());
            
            // Log headers (excluding sensitive ones)
            Map<String, String> headers = getFilteredHeaders(request);
            requestLog.put("headers", headers);

            // Log request body for POST/PUT/PATCH requests
            if (shouldLogRequestBody(request)) {
                String requestBody = getRequestBody(request);
                if (requestBody != null && !requestBody.trim().isEmpty()) {
                    requestLog.put("body", maskSensitiveData(requestBody));
                }
            }

            log.info("HTTP Request: {}", requestLog);

        } catch (Exception e) {
            log.warn("Error logging request: {}", e.getMessage());
        }
    }

    private void logResponse(ContentCachingRequestWrapper request, ContentCachingResponseWrapper response, 
                           long processingTime) {
        try {
            Map<String, Object> responseLog = new HashMap<>();
            responseLog.put("type", "RESPONSE");
            responseLog.put("timestamp", LocalDateTime.now());
            responseLog.put("method", request.getMethod());
            responseLog.put("uri", request.getRequestURI());
            responseLog.put("status", response.getStatus());
            responseLog.put("processingTimeMs", processingTime);
            responseLog.put("contentType", response.getContentType());
            responseLog.put("contentLength", response.getContentSize());

            // Log response headers (excluding sensitive ones)
            Map<String, String> responseHeaders = new HashMap<>();
            for (String headerName : response.getHeaderNames()) {
                if (!isSensitiveHeader(headerName)) {
                    responseHeaders.put(headerName, response.getHeader(headerName));
                }
            }
            responseLog.put("headers", responseHeaders);

            // Log response body for errors or if explicitly enabled
            if (shouldLogResponseBody(response)) {
                String responseBody = getResponseBody(response);
                if (responseBody != null && !responseBody.trim().isEmpty()) {
                    responseLog.put("body", maskSensitiveData(responseBody));
                }
            }

            // Performance classification
            String performanceLevel = classifyPerformance(processingTime);
            responseLog.put("performanceLevel", performanceLevel);

            if (response.getStatus() >= 400) {
                log.warn("HTTP Response (Error): {}", responseLog);
            } else if (processingTime > 1000) {
                log.warn("HTTP Response (Slow): {}", responseLog);
            } else {
                log.info("HTTP Response: {}", responseLog);
            }

        } catch (Exception e) {
            log.warn("Error logging response: {}", e.getMessage());
        }
    }

    private void logError(ContentCachingRequestWrapper request, Exception error, long processingTime) {
        try {
            Map<String, Object> errorLog = new HashMap<>();
            errorLog.put("type", "ERROR");
            errorLog.put("timestamp", LocalDateTime.now());
            errorLog.put("method", request.getMethod());
            errorLog.put("uri", request.getRequestURI());
            errorLog.put("processingTimeMs", processingTime);
            errorLog.put("errorType", error.getClass().getSimpleName());
            errorLog.put("errorMessage", error.getMessage());

            log.error("HTTP Request Error: {}", errorLog, error);

        } catch (Exception e) {
            log.warn("Error logging error: {}", e.getMessage());
        }
    }

    private boolean shouldSkipLogging(String requestURI) {
        return requestURI.startsWith("/actuator/") ||
               requestURI.equals("/health") ||
               requestURI.equals("/metrics") ||
               requestURI.equals("/favicon.ico") ||
               requestURI.startsWith("/webjars/") ||
               requestURI.startsWith("/css/") ||
               requestURI.startsWith("/js/") ||
               requestURI.startsWith("/images/");
    }

    private boolean shouldLogRequestBody(HttpServletRequest request) {
        String method = request.getMethod();
        return "POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method);
    }

    private boolean shouldLogResponseBody(HttpServletResponse response) {
        return response.getStatus() >= 400; // Log response body for errors
    }

    private Map<String, String> getFilteredHeaders(HttpServletRequest request) {
        Map<String, String> headers = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        
        if (headerNames != null) {
            for (String headerName : Collections.list(headerNames)) {
                if (!isSensitiveHeader(headerName)) {
                    headers.put(headerName, request.getHeader(headerName));
                }
            }
        }
        
        return headers;
    }

    private boolean isSensitiveHeader(String headerName) {
        if (headerName == null) return false;
        
        String lowerCaseHeader = headerName.toLowerCase();
        for (String sensitiveHeader : SENSITIVE_HEADERS) {
            if (lowerCaseHeader.contains(sensitiveHeader)) {
                return true;
            }
        }
        return false;
    }

    private String getRequestBody(ContentCachingRequestWrapper request) {
        try {
            byte[] content = request.getContentAsByteArray();
            if (content.length > 0) {
                return new String(content, request.getCharacterEncoding());
            }
        } catch (Exception e) {
            log.debug("Error reading request body: {}", e.getMessage());
        }
        return null;
    }

    private String getResponseBody(ContentCachingResponseWrapper response) {
        try {
            byte[] content = response.getContentAsByteArray();
            if (content.length > 0) {
                return new String(content, response.getCharacterEncoding());
            }
        } catch (Exception e) {
            log.debug("Error reading response body: {}", e.getMessage());
        }
        return null;
    }

    private String maskSensitiveData(String content) {
        if (content == null) return null;
        
        // Mask common sensitive fields in JSON
        return content
            .replaceAll("(\"password\"\\s*:\\s*\")[^\"]*\"", "$1***\"")
            .replaceAll("(\"token\"\\s*:\\s*\")[^\"]*\"", "$1***\"")
            .replaceAll("(\"secret\"\\s*:\\s*\")[^\"]*\"", "$1***\"")
            .replaceAll("(\"key\"\\s*:\\s*\")[^\"]*\"", "$1***\"")
            .replaceAll("(\"authorization\"\\s*:\\s*\")[^\"]*\"", "$1***\"");
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }

    private String classifyPerformance(long processingTime) {
        if (processingTime < 100) return "FAST";
        if (processingTime < 500) return "NORMAL";
        if (processingTime < 1000) return "SLOW";
        if (processingTime < 5000) return "VERY_SLOW";
        return "CRITICAL";
    }
}
