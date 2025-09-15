package com.mysillydreams.userservice.config;

import com.mysillydreams.userservice.repository.UserAuditRepository;
import com.mysillydreams.userservice.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Configuration class to add extensive logging for repository initialization
 * and startup flow debugging. This configuration is only active in the 'dev' profile.
 */
@Configuration
@Slf4j
@Profile("dev")
public class RepositoryLoggingConfig {

    @Bean
    public CommandLineRunner logRepositoryInitialization(
            @Autowired(required = false) UserRepository userRepository,
            @Autowired(required = false) UserAuditRepository userAuditRepository) {
        
        return args -> {
            log.info("=== REPOSITORY INITIALIZATION LOGGING (dev profile) ===");
            
            if (userRepository != null) {
                log.info("UserRepository successfully initialized: {}", userRepository.getClass().getName());
                try {
                    long userCount = userRepository.count();
                    log.info("UserRepository is functional - user count: {}", userCount);
                } catch (Exception e) {
                    log.error("UserRepository initialization check failed: {}", e.getMessage(), e);
                }
            } else {
                log.error("UserRepository is NULL - not initialized!");
            }
            
            if (userAuditRepository != null) {
                log.info("UserAuditRepository successfully initialized: {}", userAuditRepository.getClass().getName());
                try {
                    long auditCount = userAuditRepository.count();
                    log.info("UserAuditRepository is functional - audit record count: {}", auditCount);
                    
                    // Test the problematic method
                    log.info("Testing countGdprEventsAfter method...");
                    long gdprCount = userAuditRepository.countGdprEventsAfter(java.time.LocalDateTime.now().minusDays(30));
                    log.info("countGdprEventsAfter method executed successfully - count: {}", gdprCount);
                    
                } catch (Exception e) {
                    log.error("UserAuditRepository initialization check failed: {}", e.getMessage(), e);
                    log.error("Full stack trace:", e);
                }
            } else {
                log.error("UserAuditRepository is NULL - not initialized!");
            }
            
            log.info("=== REPOSITORY INITIALIZATION LOGGING COMPLETE ===");
        };
    }
}
