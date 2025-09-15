package com.mysillydreams.authservice;

import com.mysillydreams.authservice.service.KeycloakService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Auth Service Application
 * Provides authentication and session management for the MySillyDreams treasure hunt platform
 */
@SpringBootApplication(exclude = {
    org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration.class,
    org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration.class
})
@EnableCaching
@EnableFeignClients
@EnableScheduling
@Slf4j
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }

    /**
     * Initialize Keycloak realm on startup
     * Only active when NOT in bootstrap mode
     */
    @Bean
    @Profile("!bootstrap")
    public CommandLineRunner initializeKeycloak(KeycloakService keycloakService) {
        return args -> {
            try {
                log.info("Initializing Keycloak realm...");
                keycloakService.initializeRealm();
                log.info("Keycloak realm initialization completed");
            } catch (Exception e) {
                log.error("Failed to initialize Keycloak realm", e);
            }
        };
    }
}
