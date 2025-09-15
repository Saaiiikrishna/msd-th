package com.mysillydreams.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

/**
 * Gateway Configuration for routing and filters
 */
@Configuration
public class GatewayConfig {

    private final RedisRateLimiter publicCatalogRateLimiter;
    private final RedisRateLimiter enrollmentRateLimiter;
    private final RedisRateLimiter generalRateLimiter;
    private final KeyResolver principalOrIpKeyResolver;

    @Value("${services.auth-service.url}")
    private String authServiceUrl;

    @Value("${services.user-service.url}")
    private String userServiceUrl;

    @Value("${services.treasure-service.url}")
    private String treasureServiceUrl;

    @Value("${services.payment-service.url}")
    private String paymentServiceUrl;

    @Value("${services.keycloak.url}")
    private String keycloakUrl;

    @Value("${services.frontend.url}")
    private String frontendUrl;

    @Value("${fallback.auth.uri}")
    private String authFallbackUri;

    @Value("${fallback.user.uri}")
    private String userFallbackUri;

    @Value("${fallback.treasure.uri}")
    private String treasureFallbackUri;

    @Value("${fallback.payment.uri}")
    private String paymentFallbackUri;

    public GatewayConfig(RedisRateLimiter publicCatalogRateLimiter,
                        RedisRateLimiter enrollmentRateLimiter,
                        RedisRateLimiter generalRateLimiter,
                        KeyResolver principalOrIpKeyResolver) {
        this.publicCatalogRateLimiter = publicCatalogRateLimiter;
        this.enrollmentRateLimiter = enrollmentRateLimiter;
        this.generalRateLimiter = generalRateLimiter;
        this.principalOrIpKeyResolver = principalOrIpKeyResolver;
    }

    /**
     * Configure routes for microservices according to architecture specification
     */
    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // Auth Service Routes
                .route("auth-service", r -> r
                        .path("/api/auth/**")
                        .filters(f -> f
                                .stripPrefix(0)
                                .addRequestHeader("X-Gateway", "msd-gateway")
                                .addRequestHeader("X-Service", "auth-service")
                                .circuitBreaker(config -> config
                                        .setName("auth-service-cb")
                                        .setFallbackUri(authFallbackUri))
                                .requestRateLimiter(config -> config
                                        .setRateLimiter(generalRateLimiter)
                                        .setKeyResolver(principalOrIpKeyResolver))
                                .retry(config -> config
                                        .setRetries(3)
                                        .setMethods(HttpMethod.GET, HttpMethod.POST))
                        )
                        .uri(authServiceUrl)
                )

                // User Service Routes
                .route("user-service", r -> r
                        .path("/api/user-service/**")
                        .filters(f -> f
                                .stripPrefix(0)
                                .addRequestHeader("X-Gateway", "msd-gateway")
                                .addRequestHeader("X-Service", "user-service")
                                .circuitBreaker(config -> config
                                        .setName("user-service-cb")
                                        .setFallbackUri(userFallbackUri))
                                .requestRateLimiter(config -> config
                                        .setRateLimiter(generalRateLimiter)
                                        .setKeyResolver(principalOrIpKeyResolver))
                                .retry(config -> config
                                        .setRetries(3)
                                        .setMethods(HttpMethod.GET))
                        )
                        .uri(userServiceUrl)
                )

                // Treasure Service Routes - Public Catalog (higher rate limit)
                .route("treasure-catalog", r -> r
                        .path("/api/treasure/v1/trips")
                        .and()
                        .method("GET")
                        .filters(f -> f
                                .stripPrefix(0)
                                .addRequestHeader("X-Gateway", "msd-gateway")
                                .addRequestHeader("X-Service", "treasure-service")
                                .requestRateLimiter(config -> config
                                        .setRateLimiter(publicCatalogRateLimiter)
                                        .setKeyResolver(principalOrIpKeyResolver))
                        )
                        .uri(treasureServiceUrl)
                )

                // Treasure Service Routes - Enrollment endpoints (lower rate limit)
                .route("treasure-enrollments", r -> r
                        .path("/api/treasure/v1/trips/*/enrollments", "/api/treasure/v1/enrollments/**")
                        .filters(f -> f
                                .stripPrefix(0)
                                .addRequestHeader("X-Gateway", "msd-gateway")
                                .addRequestHeader("X-Service", "treasure-service")
                                .circuitBreaker(config -> config
                                        .setName("treasure-service-cb")
                                        .setFallbackUri(treasureFallbackUri))
                                .requestRateLimiter(config -> config
                                        .setRateLimiter(enrollmentRateLimiter)
                                        .setKeyResolver(principalOrIpKeyResolver))
                        )
                        .uri(treasureServiceUrl)
                )

                // Treasure Service Routes - General (standard rate limit)
                .route("treasure-service", r -> r
                        .path("/api/treasure/**")
                        .filters(f -> f
                                .stripPrefix(0)
                                .addRequestHeader("X-Gateway", "msd-gateway")
                                .addRequestHeader("X-Service", "treasure-service")
                                .circuitBreaker(config -> config
                                        .setName("treasure-service-cb")
                                        .setFallbackUri(treasureFallbackUri))
                                .requestRateLimiter(config -> config
                                        .setRateLimiter(generalRateLimiter)
                                        .setKeyResolver(principalOrIpKeyResolver))
                        )
                        .uri(treasureServiceUrl)
                )

                // Payment Service Routes
                .route("payment-service", r -> r
                        .path("/api/payments/**")
                        .filters(f -> f
                                .stripPrefix(0)
                                .addRequestHeader("X-Gateway", "msd-gateway")
                                .addRequestHeader("X-Service", "payment-service")
                                .circuitBreaker(config -> config
                                        .setName("payment-service-cb")
                                        .setFallbackUri(paymentFallbackUri))
                                .retry(config -> config
                                        .setRetries(2)
                                        .setMethods(HttpMethod.GET))
                                .requestRateLimiter(config -> config
                                        .setRateLimiter(generalRateLimiter)
                                        .setKeyResolver(principalOrIpKeyResolver))
                        )
                        .uri(paymentServiceUrl)
                )

                // Payment Service Swagger UI
                .route("payment-swagger", r -> r
                        .path("/payment/**")
                        .filters(f -> f
                                .rewritePath("/payment/(?<segment>.*)", "/${segment}")
                                .addRequestHeader("X-Gateway", "msd-gateway")
                        )
                        .uri(paymentServiceUrl)
                )

                // Treasure Service Swagger UI
                .route("treasure-swagger", r -> r
                        .path("/treasure/**")
                        .filters(f -> f
                                .rewritePath("/treasure/(?<segment>.*)", "/${segment}")
                                .addRequestHeader("X-Gateway", "msd-gateway")
                        )
                        .uri(treasureServiceUrl)
                )

                // Default Swagger UI routes (will route to treasure service by default)
                .route("default-swagger-ui", r -> r
                        .path("/swagger-ui/**", "/v3/api-docs/**")
                        .filters(f -> f
                                .addRequestHeader("X-Gateway", "msd-gateway")
                        )
                        .uri(treasureServiceUrl)
                )

                // Keycloak Routes
                .route("keycloak", r -> r
                        .path("/realms/**", "/resources/**")
                        .filters(f -> f
                                .stripPrefix(0)
                                .addRequestHeader("X-Gateway", "msd-gateway")
                                .addRequestHeader("X-Service", "keycloak"))
                        .uri(keycloakUrl))

                // Fallback route for frontend (everything else)
                .route("frontend-fallback", r -> r
                        .path("/**")
                        .and()
                        .not(p -> p.path("/api/**"))
                        .and()
                        .not(p -> p.path("/actuator/**"))
                        .and()
                        .not(p -> p.path("/payment/**"))
                        .and()
                        .not(p -> p.path("/treasure/**"))
                        .and()
                        .not(p -> p.path("/swagger-ui/**"))
                        .and()
                        .not(p -> p.path("/v3/api-docs/**"))
                        .filters(f -> f
                                .addRequestHeader("X-Gateway", "msd-gateway")
                                .addRequestHeader("X-Frontend-Route", "true")
                        )
                        .uri(frontendUrl)
                )
                .build();
    }


}
