package com.mysillydreams.userservice.config;

import com.mysillydreams.userservice.security.InternalApiKeyFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security configuration for User Service.
 * Configured as an OAuth2 Resource Server, validating JWTs from the Auth Service.
 * Implements role hierarchy and method-level security.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity // Enables @PreAuthorize, @PostAuthorize, etc.
public class SecurityConfig {

    private final RoleHierarchy roleHierarchy;
    private final InternalApiKeyFilter internalApiKeyFilter;

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuerUri;

    public SecurityConfig(RoleHierarchy roleHierarchy, InternalApiKeyFilter internalApiKeyFilter) {
        this.roleHierarchy = roleHierarchy;
        this.internalApiKeyFilter = internalApiKeyFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF for stateless API
            .csrf(AbstractHttpConfigurer::disable)

            // CORS is handled at the API Gateway, no need to configure here
            .cors(AbstractHttpConfigurer::disable)

            // Stateless session management as we are using JWTs
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Add internal API key filter before OAuth2 processing
            .addFilterBefore(internalApiKeyFilter, UsernamePasswordAuthenticationFilter.class)

            // Configure as an OAuth2 Resource Server with explicit JWT validation
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.decoder(jwtDecoder())))

            // Configure authorization rules
            .authorizeHttpRequests(authz -> authz
                // Public endpoints (no authentication required)
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-resources/**").permitAll()
                .requestMatchers("/favicon.ico", "/error").permitAll()

                // User creation endpoint is now secured by InternalApiKeyFilter
                // POST requests require valid internal API key, GET requests require authentication
                .requestMatchers("POST", "/api/v1/users").permitAll() // Secured by InternalApiKeyFilter
                .requestMatchers("GET", "/api/v1/users/**").authenticated() // Requires JWT token

                // Admin endpoints (require ROLE_ADMIN)
                .requestMatchers("/admin/**").hasRole("ADMIN")

                // Internal endpoints (require ROLE_INTERNAL_CONSUMER)
                .requestMatchers("/internal/**").hasRole("INTERNAL_CONSUMER")

                // All other requests require authentication
                .anyRequest().authenticated()
            );

        return http.build();
    }

    /**
     * Creates a JWT decoder that validates tokens against the configured issuer URI.
     */
    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withIssuerLocation(issuerUri).build();
    }

    /**
     * Method security expression handler with role hierarchy support.
     * This is essential for @PreAuthorize annotations to understand the role hierarchy.
     */
    @Bean
    public MethodSecurityExpressionHandler methodSecurityExpressionHandler() {
        DefaultMethodSecurityExpressionHandler expressionHandler = new DefaultMethodSecurityExpressionHandler();
        expressionHandler.setRoleHierarchy(roleHierarchy);
        return expressionHandler;
    }
}
