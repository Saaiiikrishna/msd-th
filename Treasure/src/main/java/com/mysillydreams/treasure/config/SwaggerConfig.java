package com.mysillydreams.treasure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Swagger/OpenAPI configuration for the Treasure Service API.
 * Provides comprehensive API documentation with detailed descriptions and examples.
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI treasureOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Treasure Hunt API")
                        .description("""
                                # Treasure Hunt Gaming Platform API

                                The Treasure Hunt API is a comprehensive gaming platform for managing treasure hunt competitions,
                                user enrollments, leaderboards, and promotional campaigns. It supports both individual and team
                                participation with advanced scoring and ranking systems.

                                ## Key Features
                                - **Registration System**: Unique registration IDs (TH-MMYY-IND/TEAM-XXXXXX format)
                                - **Team & Individual Enrollments**: Support for both individual and team participation
                                - **Leaderboards**: Real-time rankings across multiple difficulty levels and time periods
                                - **Statistics Tracking**: Comprehensive user performance metrics and achievements
                                - **Discount System**: Promo codes and promotional campaigns with advanced rules
                                - **Payment Integration**: INR payments via Razorpay with invoice generation
                                - **Plan Management**: Treasure hunt plans with difficulty levels and pricing
                                - **Progress Tracking**: User level progression and task completion tracking
                                
                                ## Architecture
                                - **Database**: PostgreSQL with PostGIS for spatial data
                                - **Messaging**: Kafka for event-driven communication
                                - **Caching**: Redis for performance optimization
                                - **Monitoring**: Actuator endpoints for health checks and metrics
                                
                                ## Authentication
                                Currently, admin endpoints are available for testing. Production deployment will include 
                                proper authentication and authorization mechanisms.
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Treasure Development Team")
                                .email("dev@mysillydreams.com")
                                .url("https://mysillydreams.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Local Development Server"),
                        new Server()
                                .url("https://api.mysillydreams.com")
                                .description("Production Server")))
                .tags(List.of(
                        new Tag()
                                .name("Treasure Hunt - Enrollment")
                                .description("User enrollment operations for treasure hunt plans with registration ID generation"),
                        new Tag()
                                .name("Treasure Hunt - Leaderboards")
                                .description("Leaderboard and user statistics endpoints for rankings and performance tracking"),
                        new Tag()
                                .name("Treasure Hunt - Discounts")
                                .description("Promo codes and promotional campaign management"),
                        new Tag()
                                .name("Admin - Categories")
                                .description("Administrative operations for managing categories and subcategories"),
                        new Tag()
                                .name("Admin - Plans")
                                .description("Administrative operations for managing treasure hunt plans"),
                        new Tag()
                                .name("Admin - Policies")
                                .description("Administrative operations for managing progression and pricing policies"),
                        new Tag()
                                .name("Public - Catalog")
                                .description("Public endpoints for browsing available treasure hunt plans"),
                        new Tag()
                                .name("System - Health")
                                .description("System health checks and monitoring endpoints")));
    }
}
