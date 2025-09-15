package com.mysillydreams.userservice;

import com.mysillydreams.userservice.converter.CryptoConverter; // Import to reference
import com.mysillydreams.userservice.service.EncryptionService; // Import to reference
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing; // For @CreationTimestamp etc.
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity; // Updated for Spring Boot 3.x
import org.springframework.vault.core.VaultOperations;

@SpringBootApplication(exclude = {RedisRepositoriesAutoConfiguration.class})
@EnableDiscoveryClient
@EnableJpaAuditing // To enable @CreationTimestamp and @UpdateTimestamp in entities
@EnableMethodSecurity(prePostEnabled = true, jsr250Enabled = true) // Enable method security
@EnableJpaRepositories(basePackages = "com.mysillydreams.userservice.repository")
@Slf4j
public class UserServiceApplication {

    public static void main(String[] args) {
        log.info("=== STARTING USER SERVICE APPLICATION ===");
        log.info("Java version: {}", System.getProperty("java.version"));
        log.info("Spring Boot starting...");

        try {
            ApplicationContext context = SpringApplication.run(UserServiceApplication.class, args);
            log.info("=== USER SERVICE APPLICATION STARTED SUCCESSFULLY ===");
        } catch (Exception e) {
            log.error("=== USER SERVICE APPLICATION STARTUP FAILED ===");
            log.error("Startup error: {}", e.getMessage(), e);
            throw e;
        }

        // Manual check/trigger for CryptoConverter static injection if needed,
        // though @Component on CryptoConverter and @Autowired on its setter should handle it.
        // This is more of a diagnostic step or a fallback if issues are seen with the static injection.
        // EncryptionService encryptionService = context.getBean(EncryptionService.class);
        // CryptoConverter cryptoConverter = context.getBean(CryptoConverter.class); // If it's a bean
        // cryptoConverter.setEncryptionService(encryptionService); // This would be redundant if @Autowired setter works
    }

    // Optional: Define a Spring Cloud Vault specific health indicator if needed for more detailed Vault health.
    // By default, Spring Boot Actuator includes a general Vault health indicator if spring-cloud-starter-vault-config is present.
    @Bean
    public HealthIndicator vaultHealthIndicator(VaultOperations vaultOperations) {
        // Simple check if Vault is accessible
        return () -> {
            try {
                vaultOperations.opsForSys().health(); // Check Vault's health endpoint
                return Health.up().withDetail("vault", "Accessible").build();
            } catch (Exception e) {
                return Health.down().withDetail("vault", "Not accessible").withException(e).build();
            }
        };
    }

}
