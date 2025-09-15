package com.mysillydreams.gateway.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Circuit Breaker configuration for API Gateway
 * Implements circuit breaker patterns as specified in the architecture
 */
@Configuration
@Slf4j
public class CircuitBreakerConfig {

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    /**
     * Customize circuit breaker configuration for different services
     */
    @Bean
    public Customizer<ReactiveResilience4JCircuitBreakerFactory> defaultCustomizer() {
        return factory -> {
            // Auth Service Circuit Breaker - Critical service, fail fast
            factory.configure(builder -> builder
                    .circuitBreakerConfig(io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
                            .slidingWindowSize(10)
                            .slidingWindowType(SlidingWindowType.COUNT_BASED)
                            .failureRateThreshold(50.0f)
                            .waitDurationInOpenState(Duration.ofSeconds(30))
                            .minimumNumberOfCalls(5)
                            .permittedNumberOfCallsInHalfOpenState(3)
                            .automaticTransitionFromOpenToHalfOpenEnabled(true)
                            .build())
                    .timeLimiterConfig(TimeLimiterConfig.custom()
                            .timeoutDuration(Duration.ofSeconds(5))
                            .build()),
                    "auth-service-cb");

            // User Service Circuit Breaker - Important service, moderate tolerance
            factory.configure(builder -> builder
                    .circuitBreakerConfig(io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
                            .slidingWindowSize(20)
                            .slidingWindowType(SlidingWindowType.COUNT_BASED)
                            .failureRateThreshold(60.0f)
                            .waitDurationInOpenState(Duration.ofSeconds(45))
                            .minimumNumberOfCalls(10)
                            .permittedNumberOfCallsInHalfOpenState(5)
                            .automaticTransitionFromOpenToHalfOpenEnabled(true)
                            .build())
                    .timeLimiterConfig(TimeLimiterConfig.custom()
                            .timeoutDuration(Duration.ofSeconds(8))
                            .build()),
                    "user-service-cb");

            // Treasure Service Circuit Breaker - Core business service
            factory.configure(builder -> builder
                    .circuitBreakerConfig(io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
                            .slidingWindowSize(20)
                            .slidingWindowType(SlidingWindowType.COUNT_BASED)
                            .failureRateThreshold(50.0f)
                            .waitDurationInOpenState(Duration.ofSeconds(60))
                            .minimumNumberOfCalls(10)
                            .permittedNumberOfCallsInHalfOpenState(5)
                            .automaticTransitionFromOpenToHalfOpenEnabled(true)
                            .build())
                    .timeLimiterConfig(TimeLimiterConfig.custom()
                            .timeoutDuration(Duration.ofSeconds(10))
                            .build()),
                    "treasure-service-cb");

            // Payment Service Circuit Breaker - Critical for transactions
            factory.configure(builder -> builder
                    .circuitBreakerConfig(io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
                            .slidingWindowSize(15)
                            .slidingWindowType(SlidingWindowType.COUNT_BASED)
                            .failureRateThreshold(40.0f)
                            .waitDurationInOpenState(Duration.ofSeconds(30))
                            .minimumNumberOfCalls(5)
                            .permittedNumberOfCallsInHalfOpenState(3)
                            .automaticTransitionFromOpenToHalfOpenEnabled(true)
                            .build())
                    .timeLimiterConfig(TimeLimiterConfig.custom()
                            .timeoutDuration(Duration.ofSeconds(15))
                            .build()),
                    "payment-service-cb");

            log.info("Custom circuit breaker configurations applied for all services");
        };
    }

    /**
     * Attaches logging event listeners to all circuit breakers after they have been configured.
     * This method runs automatically after the bean has been initialized.
     */
    @PostConstruct
    public void attachCircuitBreakerLoggers() {
        log.info("Attaching loggers to all configured circuit breakers...");

        circuitBreakerRegistry.getAllCircuitBreakers().forEach(cb -> {
            String cbName = cb.getName();

            cb.getEventPublisher()
                .onStateTransition(event -> {
                    log.warn("-> [CircuitBreaker:{}] State changed from {} to {}",
                        cbName, event.getStateTransition().getFromState(), event.getStateTransition().getToState());
                });

            cb.getEventPublisher()
                .onCallNotPermitted(event -> {
                    log.warn("-> [CircuitBreaker:{}] Call NOT PERMITTED. Circuit is open.", cbName);
                });

            cb.getEventPublisher()
                .onError(event -> {
                    log.error("-> [CircuitBreaker:{}] Call FAILED. Duration: {}ms. Error: {}",
                        cbName, event.getElapsedDuration().toMillis(), event.getThrowable().getMessage());
                });

            cb.getEventPublisher()
                .onSuccess(event -> {
                    log.info("-> [CircuitBreaker:{}] Call SUCCESS. Duration: {}ms.",
                        cbName, event.getElapsedDuration().toMillis());
                });

            log.info("-> [CircuitBreaker:{}] Successfully attached event listeners.", cbName);
        });
    }
}
