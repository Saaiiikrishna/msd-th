package com.mysillydreams.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * API Gateway Application for Treasure Hunt Platform
 *
 * This gateway serves as the single entry point for all client requests,
 * routing them to appropriate microservices and handling cross-cutting concerns
 * like authentication, rate limiting, and request/response transformation.
 */
@SpringBootApplication
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
