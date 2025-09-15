package com.mysillydreams.authservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * Bootstrap Security Configuration for Auth Service
 * Used when starting without Keycloak realm configured
 * Provides minimal security for health checks and admin operations
 */
@Configuration
@EnableWebSecurity
@Profile("bootstrap")
@Slf4j
public class BootstrapSecurityConfig {

    /**
     * Security filter chain configuration for bootstrap mode
     * Allows all requests for initial setup and realm creation
     */
    @Bean
    public SecurityFilterChain bootstrapFilterChain(HttpSecurity http) throws Exception {
        log.info("Configuring bootstrap security - OAuth2 disabled");
        
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authz -> authz
                // Allow all requests during bootstrap mode
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers("/v1/health").permitAll()
                .requestMatchers("/v1/admin/realm/create").permitAll() // Allow realm creation
                .requestMatchers("/v1/admin/realm/status").permitAll() // Allow realm status check
                .anyRequest().permitAll() // Allow everything else during bootstrap
            );

        return http.build();
    }
}
