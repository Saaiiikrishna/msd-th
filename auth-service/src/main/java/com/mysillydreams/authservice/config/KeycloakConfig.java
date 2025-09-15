package com.mysillydreams.authservice.config;

import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.util.DefaultResourceRetriever;
import com.nimbusds.jose.util.ResourceRetriever;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

/**
 * Keycloak configuration for Auth Service
 * Configures OAuth2 resource server and admin client
 * Only active when NOT in bootstrap mode
 */
@Configuration
@EnableWebSecurity
@Profile("!bootstrap")
@Slf4j
public class KeycloakConfig {

    @Value("${keycloak.auth-server-url}")
    private String keycloakServerUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.admin.username}")
    private String adminUsername;

    @Value("${keycloak.admin.password}")
    private String adminPassword;

    @Value("${keycloak.admin.client-id}")
    private String adminClientId;

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private String jwkSetUri;

    /**
     * Keycloak admin client for user management operations
     */
    @Bean
    public Keycloak keycloakAdminClient() {
        log.info("Initializing Keycloak admin client for realm: {}", realm);
        
        return KeycloakBuilder.builder()
                .serverUrl(keycloakServerUrl)
                .realm("master") // Admin operations use master realm
                .clientId(adminClientId)
                .username(adminUsername)
                .password(adminPassword)
                .build();
    }

    /**
     * JWT decoder for validating tokens from Keycloak with improved fallback mechanism
     */
    @Bean
    public JwtDecoder jwtDecoder() {
        log.info("Configuring JWT decoder with JWK Set URI: {}", jwkSetUri);

        try {
            // Create decoder with timeout configuration
            ResourceRetriever resourceRetriever = new DefaultResourceRetriever(
                    (int) Duration.ofSeconds(30).toMillis(), // Connect timeout
                    (int) Duration.ofSeconds(30).toMillis(), // Read timeout
                    0 // Size limit (0 = no limit)
            );

            NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri)
                    .build();

            // Test connectivity to Keycloak (non-blocking)
            testKeycloakConnectivity();

            log.info("✅ JWT decoder configured successfully with JWK Set URI: {}", jwkSetUri);
            return decoder;

        } catch (Exception e) {
            log.warn("⚠️ Failed to configure JWT decoder with JWK Set URI: {}. Error: {}",
                    jwkSetUri, e.getMessage());
            log.warn("⚠️ Creating resilient decoder that will retry Keycloak connection on token validation");

            // Create a resilient decoder that attempts to reconnect on each validation
            return new ResilientJwtDecoder(jwkSetUri);
        }
    }

    /**
     * Test Keycloak connectivity without blocking application startup
     */
    private void testKeycloakConnectivity() {
        try {
            // Simple HTTP GET to test connectivity
            java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();

            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(jwkSetUri))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
            log.info("✅ Keycloak connectivity test successful");

        } catch (Exception e) {
            log.warn("⚠️ Keycloak connectivity test failed: {}. Service will retry on token validation.", e.getMessage());
        }
    }

    /**
     * Resilient JWT decoder that retries connection to Keycloak
     */
    private static class ResilientJwtDecoder implements JwtDecoder {
        private final String jwkSetUri;
        private volatile NimbusJwtDecoder delegate;
        private volatile long lastAttempt = 0;
        private static final long RETRY_INTERVAL_MS = 30000; // 30 seconds

        public ResilientJwtDecoder(String jwkSetUri) {
            this.jwkSetUri = jwkSetUri;
        }

        @Override
        public Jwt decode(String token) throws org.springframework.security.oauth2.jwt.JwtException {
            NimbusJwtDecoder decoder = getOrCreateDecoder();
            if (decoder != null) {
                return decoder.decode(token);
            }
            throw new org.springframework.security.oauth2.jwt.JwtException(
                "JWT decoder not available - Keycloak connection failed. Please check Keycloak service status.");
        }

        private NimbusJwtDecoder getOrCreateDecoder() {
            if (delegate != null) {
                return delegate;
            }

            long now = System.currentTimeMillis();
            if (now - lastAttempt < RETRY_INTERVAL_MS) {
                return null; // Too soon to retry
            }

            lastAttempt = now;
            try {
                delegate = NimbusJwtDecoder.withJwkSetUri(jwkSetUri)
                        .build();
                log.info("✅ JWT decoder reconnected successfully to: {}", jwkSetUri);
                return delegate;
            } catch (Exception e) {
                log.warn("⚠️ JWT decoder reconnection failed: {}. Will retry in {} seconds",
                        e.getMessage(), RETRY_INTERVAL_MS / 1000);
                return null;
            }
        }
    }

    /**
     * Security filter chain for public endpoints - no OAuth2 resource server
     */
    @Bean
    @Order(1)
    public SecurityFilterChain publicSecurityFilterChain(HttpSecurity http) throws Exception {
        log.info("🔧 CREATING PUBLIC SECURITY FILTER CHAIN (NO OAUTH2)");
        log.info("🚀 PUBLIC FILTER CHAIN METHOD CALLED - TIMESTAMP: {}", System.currentTimeMillis());

        return http
            .securityMatcher(
                "/api/auth/v1/register",
                "/api/auth/v1/login",
                "/api/auth/v1/login-start",
                "/api/auth/v1/callback",
                "/api/auth/v1/refresh",
                "/api/auth/v1/logout",
                "/api/auth/v1/health",
                "/api/auth/v1/test", // For testing
                "/actuator/**",
                "/v3/api-docs/**",
                "/swagger-ui/**",
                "/swagger-ui.html"
            )
            .csrf(csrf -> {
                csrf.disable();
                log.info("🔧 CSRF disabled for public endpoints");
            })
            .sessionManagement(session -> {
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS);
                log.info("🔧 Session management set to STATELESS for public endpoints");
            })
            .authorizeHttpRequests(authz -> {
                log.info("🔧 Configuring public authorization rules...");

                // Allow OPTIONS requests (CORS preflight)
                authz.requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll();
                log.info("🔧 OPTIONS /** -> permitAll() for public endpoints");

                // All matched endpoints are public
                authz.anyRequest().permitAll();
                log.info("🔧 All public endpoints -> permitAll()");
            })
            .addFilterBefore(requestLoggingFilter(), UsernamePasswordAuthenticationFilter.class)
            .build();
    }

    /**
     * Security filter chain for protected endpoints - with OAuth2 resource server
     */
    @Bean
    @Order(2)
    public SecurityFilterChain protectedSecurityFilterChain(HttpSecurity http) throws Exception {
        log.info("🔧 CREATING PROTECTED SECURITY FILTER CHAIN WITH OAUTH2");

        return http
            .csrf(csrf -> {
                csrf.disable();
                log.info("🔧 CSRF disabled for protected endpoints");
            })
            .sessionManagement(session -> {
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS);
                log.info("🔧 Session management set to STATELESS for protected endpoints");
            })
            .authorizeHttpRequests(authz -> {
                log.info("🔧 Configuring protected authorization rules...");

                // All other endpoints require authentication
                authz.anyRequest().authenticated();
                log.info("🔧 anyRequest() -> authenticated() for protected endpoints");
            })
            .oauth2ResourceServer(oauth -> {
                log.info("🔧 Configuring OAuth2 Resource Server for protected endpoints...");
                oauth.jwt(jwt -> {
                    jwt.decoder(jwtDecoder());
                    log.info("🔧 JWT decoder configured for OAuth2 Resource Server");
                });
                // Apply the simple bearer token resolver
                oauth.bearerTokenResolver(bearerTokenResolver());
                log.info("🔧 Simple bearer token resolver applied to OAuth2 Resource Server");
                log.info("🔧 OAuth2 Resource Server configuration completed");
            })
            .addFilterBefore(requestLoggingFilter(), UsernamePasswordAuthenticationFilter.class)
            .build();
    }

    /**
     * Simple request logging filter to trace all incoming requests
     */
    @Bean
    public OncePerRequestFilter requestLoggingFilter() {
        log.info("🔧 CREATING REQUEST LOGGING FILTER");
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                    throws ServletException, IOException {
                String method = request.getMethod();
                String uri = request.getRequestURI();
                String authHeader = request.getHeader("Authorization");
                String queryString = request.getQueryString();

                log.info("📥 REQUEST FILTER START: {} {} | Query: {} | Auth: {} | Content-Type: {} | Remote: {}",
                    method, uri, queryString,
                    authHeader != null ? "Present" : "None",
                    request.getContentType(),
                    request.getRemoteAddr());

                // Log all headers for debugging
                log.info("📥 REQUEST HEADERS:");
                request.getHeaderNames().asIterator().forEachRemaining(headerName -> {
                    log.info("📥   {}: {}", headerName, request.getHeader(headerName));
                });

                long startTime = System.currentTimeMillis();

                try {
                    log.info("📥 CALLING FILTER CHAIN for {} {}", method, uri);
                    filterChain.doFilter(request, response);
                    long duration = System.currentTimeMillis() - startTime;
                    log.info("📤 REQUEST FILTER END: {} {} | Status: {} | Duration: {}ms",
                        method, uri, response.getStatus(), duration);
                } catch (Exception e) {
                    long duration = System.currentTimeMillis() - startTime;
                    log.error("💥 REQUEST FILTER EXCEPTION: {} {} | Exception: {} | Duration: {}ms",
                        method, uri, e.getMessage(), duration);
                    log.error("💥 EXCEPTION STACK TRACE:", e);
                    throw e;
                }
            }
        };
    }

    /**
     * RestTemplate bean for making HTTP requests (including Google API calls)
     */
    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        // Configure timeout and user agent for Google API calls
        restTemplate.getInterceptors().add((request, body, execution) -> {
            request.getHeaders().add("User-Agent", "MySillyDreams-TreasureHunt/1.0");
            return execution.execute(request, body);
        });
        return restTemplate;
    }

    /**
     * ObjectMapper bean for JSON processing
     */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }



    /**
     * Simple bearer token resolver that extracts tokens from Authorization header or access_token cookie
     * Does NOT make authorization decisions - that's the job of the SecurityFilterChain
     */
    @Component
    public static class CookieAndHeaderBearerTokenResolver implements BearerTokenResolver {

        private static final String AUTH_HEADER = "Authorization";
        private static final String BEARER = "Bearer ";
        private static final String ACCESS_TOKEN_COOKIE = "access_token";

        @Override
        public String resolve(HttpServletRequest request) {
            // 1) Authorization header
            String authHeader = request.getHeader(AUTH_HEADER);
            if (authHeader != null && authHeader.regionMatches(true, 0, BEARER, 0, BEARER.length())) {
                String token = authHeader.substring(BEARER.length()).trim();
                log.debug("🔑 Token found in Authorization header");
                return token;
            }

            // 2) Cookie (BFF/gateway pattern)
            if (request.getCookies() != null) {
                for (Cookie cookie : request.getCookies()) {
                    if (ACCESS_TOKEN_COOKIE.equals(cookie.getName()) &&
                        cookie.getValue() != null && !cookie.getValue().isBlank()) {
                        log.debug("🔑 Token found in access_token cookie");
                        return cookie.getValue();
                    }
                }
            }

            // 3) No token – fine, let authorization decide
            log.debug("🔍 No token found in request");
            return null;
        }
    }

    @Bean
    public BearerTokenResolver bearerTokenResolver() {
        log.info("🔧 CREATING SIMPLE BEARER TOKEN RESOLVER BEAN");
        return new CookieAndHeaderBearerTokenResolver();
    }

    /**
     * Startup verification to prove which resolver is actually running
     */
    @Bean
    public ApplicationListener<ApplicationStartedEvent> resolverVerification() {
        return event -> {
            BearerTokenResolver resolver = bearerTokenResolver();
            log.info("🔍 BearerTokenResolver in use: {}@{}",
                resolver.getClass().getName(), System.identityHashCode(resolver));
        };
    }
}