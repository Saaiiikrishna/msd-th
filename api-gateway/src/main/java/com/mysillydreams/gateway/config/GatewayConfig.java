package com.mysillydreams.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

/**
 * Gateway Configuration for routing and filters
 */
@Configuration
public class GatewayConfig {

    /**
     * Configure routes for microservices
     */
    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // Payment Service Routes
                .route("payment-service", r -> r
                        .path("/api/payments/**")
                        .filters(f -> f
                                .stripPrefix(0)
                                .addRequestHeader("X-Gateway", "treasure-hunt-gateway")
                                .addRequestHeader("X-Service", "payment-service")
                        )
                        .uri("http://localhost:8081")
                )
                
                // Treasure Service Routes (main treasure hunt API)
                .route("treasure-service", r -> r
                        .path("/api/treasure/**", "/api/plans/**", "/api/registrations/**")
                        .filters(f -> f
                                .stripPrefix(0)
                                .addRequestHeader("X-Gateway", "treasure-hunt-gateway")
                                .addRequestHeader("X-Service", "treasure-service")
                        )
                        .uri("http://localhost:8080")
                )
                
                // Trust Widgets Route (from Payment Service)
                .route("trust-widgets", r -> r
                        .path("/api/trust/**")
                        .filters(f -> f
                                .rewritePath("/api/trust/(?<segment>.*)", "/api/payments/v1/trust/${segment}")
                                .addRequestHeader("X-Gateway", "treasure-hunt-gateway")
                        )
                        .uri("http://localhost:8081")
                )

                // Payment Service Swagger UI
                .route("payment-swagger", r -> r
                        .path("/payment/**")
                        .filters(f -> f
                                .rewritePath("/payment/(?<segment>.*)", "/${segment}")
                                .addRequestHeader("X-Gateway", "treasure-hunt-gateway")
                        )
                        .uri("http://localhost:8081")
                )

                // Treasure Service Swagger UI
                .route("treasure-swagger", r -> r
                        .path("/treasure/**")
                        .filters(f -> f
                                .rewritePath("/treasure/(?<segment>.*)", "/${segment}")
                                .addRequestHeader("X-Gateway", "treasure-hunt-gateway")
                        )
                        .uri("http://localhost:8080")
                )

                // Default Swagger UI routes (will route to treasure service by default)
                .route("default-swagger-ui", r -> r
                        .path("/swagger-ui/**", "/v3/api-docs/**")
                        .filters(f -> f
                                .addRequestHeader("X-Gateway", "treasure-hunt-gateway")
                        )
                        .uri("http://localhost:8080")
                )
                
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
                                .addRequestHeader("X-Gateway", "treasure-hunt-gateway")
                                .addRequestHeader("X-Frontend-Route", "true")
                        )
                        .uri("http://localhost:3000")
                )
                .build();
    }

    // Rate limiting beans commented out for initial testing
    // Uncomment when Redis is available and rate limiting is needed

    /*
    /**
     * Redis-based rate limiter configuration
     * 10 requests per second with burst capacity of 20
     */
    /*
    @Bean
    public RedisRateLimiter redisRateLimiter() {
        return new RedisRateLimiter(10, 20, 1);
    }

    /**
     * Key resolver for rate limiting based on user IP address
     */
    /*
    @Bean
    @Primary
    public KeyResolver userKeyResolver() {
        return exchange -> {
            String clientIp = exchange.getRequest().getRemoteAddress() != null
                    ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                    : "unknown";
            return Mono.just(clientIp);
        };
    }

    /**
     * Key resolver for rate limiting based on authenticated user
     */
    /*
    @Bean
    public KeyResolver authenticatedUserKeyResolver() {
        return exchange -> {
            // Extract user ID from JWT token in Authorization header
            String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                // TODO: Extract user ID from JWT token
                return Mono.just("user-from-jwt");
            }
            // Fallback to IP-based rate limiting
            String clientIp = exchange.getRequest().getRemoteAddress() != null
                    ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                    : "unknown";
            return Mono.just("ip-" + clientIp);
        };
    }
    */
}
