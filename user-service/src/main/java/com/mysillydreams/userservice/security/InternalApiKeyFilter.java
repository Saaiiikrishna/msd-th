package com.mysillydreams.userservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;

/**
 * Security filter to validate internal API keys for service-to-service communication.
 * This filter runs before OAuth2 authentication and validates requests from internal services.
 */
@Component
@Order(1) // Run before OAuth2 filters
@Slf4j
public class InternalApiKeyFilter extends OncePerRequestFilter {

    private static final String INTERNAL_API_KEY_HEADER = "X-Internal-API-Key";
    private static final String INTERNAL_SERVICE_ATTRIBUTE = "INTERNAL_SERVICE_REQUEST";
    
    // Endpoints that require internal API key validation
    private static final Set<String> INTERNAL_ENDPOINTS = Set.of(
        "/api/v1/users" // User creation endpoint
    );

    @Value("${app.security.internal-api-keys}")
    private Set<String> validApiKeys;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        String requestPath = request.getRequestURI();
        String method = request.getMethod();
        // Try both case variations of the header
        String apiKey = request.getHeader(INTERNAL_API_KEY_HEADER);
        if (apiKey == null) {
            apiKey = request.getHeader("x-internal-api-key");
        }
        
        log.debug("🔍 InternalApiKeyFilter processing request: {} {}", method, requestPath);
        
        // Check if this is an internal endpoint that requires API key validation
        if (isInternalEndpoint(requestPath, method)) {
            log.info("🔐 Internal endpoint detected: {} {}", method, requestPath);
            
            if (apiKey == null || apiKey.trim().isEmpty()) {
                log.warn("❌ Missing internal API key for endpoint: {} {}", method, requestPath);
                sendUnauthorizedResponse(response, "Missing internal API key");
                return;
            }
            
            if (!isValidApiKey(apiKey)) {
                log.warn("❌ Invalid internal API key for endpoint: {} {}", method, requestPath);
                log.warn("❌ Provided API key: {}", maskApiKey(apiKey));
                sendUnauthorizedResponse(response, "Invalid internal API key");
                return;
            }
            
            log.info("✅ Valid internal API key provided for: {} {}", method, requestPath);
            // Mark request as internal service request
            request.setAttribute(INTERNAL_SERVICE_ATTRIBUTE, true);

            // Create ServicePrincipal authentication for security expressions
            ServicePrincipal servicePrincipal = new ServicePrincipal("auth-service");
            InternalServiceAuthentication authentication = new InternalServiceAuthentication(servicePrincipal);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        
        // Continue with the filter chain
        filterChain.doFilter(request, response);
    }

    /**
     * Check if the endpoint requires internal API key validation
     */
    private boolean isInternalEndpoint(String path, String method) {
        // Only POST requests to user creation endpoint require internal API key
        if ("POST".equalsIgnoreCase(method)) {
            return INTERNAL_ENDPOINTS.stream().anyMatch(path::startsWith);
        }
        return false;
    }

    /**
     * Validate the provided API key
     */
    private boolean isValidApiKey(String apiKey) {
        if (validApiKeys == null || validApiKeys.isEmpty()) {
            log.warn("⚠️ No valid API keys configured - rejecting all internal requests");
            return false;
        }
        
        return validApiKeys.contains(apiKey);
    }

    /**
     * Send unauthorized response
     */
    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write(String.format(
            "{\"error\":\"UNAUTHORIZED\",\"message\":\"%s\",\"timestamp\":\"%s\"}", 
            message, 
            java.time.Instant.now().toString()
        ));
    }

    /**
     * Mask API key for logging
     */
    private String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() <= 4) {
            return "****";
        }
        return apiKey.substring(0, 4) + "****";
    }

    /**
     * Utility method to check if current request is from internal service
     */
    public static boolean isInternalServiceRequest(HttpServletRequest request) {
        return Boolean.TRUE.equals(request.getAttribute(INTERNAL_SERVICE_ATTRIBUTE));
    }

    /**
     * Method for use in security expressions to check if current request is from internal service
     */
    public boolean isInternalServiceRequest() {
        // Get the current request from RequestContextHolder
        try {
            org.springframework.web.context.request.RequestAttributes requestAttributes =
                org.springframework.web.context.request.RequestContextHolder.currentRequestAttributes();
            if (requestAttributes instanceof org.springframework.web.context.request.ServletRequestAttributes) {
                HttpServletRequest request = ((org.springframework.web.context.request.ServletRequestAttributes) requestAttributes).getRequest();
                return isInternalServiceRequest(request);
            }
        } catch (IllegalStateException e) {
            // No request context available
            log.debug("No request context available for internal service check");
        }
        return false;
    }

    /**
     * Principal representing an internal service
     */
    public static class ServicePrincipal {
        private final String serviceName;

        public ServicePrincipal(String serviceName) {
            this.serviceName = serviceName;
        }

        public String getServiceName() {
            return serviceName;
        }

        @Override
        public String toString() {
            return "ServicePrincipal{serviceName='" + serviceName + "'}";
        }
    }

    /**
     * Authentication token for internal service requests
     */
    public static class InternalServiceAuthentication extends AbstractAuthenticationToken {
        private final ServicePrincipal principal;

        public InternalServiceAuthentication(ServicePrincipal principal) {
            super(Collections.singletonList(new SimpleGrantedAuthority("ROLE_INTERNAL_CONSUMER")));
            this.principal = principal;
            setAuthenticated(true);
        }

        @Override
        public Object getCredentials() {
            return null;
        }

        @Override
        public Object getPrincipal() {
            return principal;
        }
    }
}
