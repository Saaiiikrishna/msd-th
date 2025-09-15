package com.mysillydreams.userservice.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "User Service API",
        version = "v1.0",
        description = "API for managing user identity profiles, roles, consents, addresses, and session listings in the MySillyDreams Platform. " +
                     "This service focuses on core user identity management with field-level encryption for PII data and GDPR/DPDP compliance.",
        contact = @Contact(name = "API Support", email = "support@mysillydreams.com", url = "https://mysillydreams.com/support"),
        license = @License(name = "Apache 2.0", url = "http://www.apache.org/licenses/LICENSE-2.0.html"),
        termsOfService = "https://mysillydreams.com/terms"
    ),
    servers = {
        @Server(url = "http://localhost:8082/api/user-service/v1", description = "Local development server"),
        @Server(url = "https://api-staging.mysillydreams.com/api/user-service/v1", description = "Staging server"),
        @Server(url = "https://api.mysillydreams.com/api/user-service/v1", description = "Production server")
    },
    externalDocs = @ExternalDocumentation(
        description = "User Service Documentation",
        url = "https://docs.mysillydreams.com/services/user-service"
    )
)
// Security schemes for different authentication methods
@SecurityScheme(
    name = "bearerAuthUser",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    in = SecuritySchemeIn.HEADER,
    description = "JWT Bearer token authentication. Tokens are validated as OAuth2 Resource Server."
)
@SecurityScheme(
    name = "internalApiKey",
    type = SecuritySchemeType.APIKEY,
    in = SecuritySchemeIn.HEADER,
    paramName = "X-Internal-API-Key",
    description = "Internal API key for service-to-service communication."
)
public class OpenApiConfig {

    /**
     * Customizes the OpenAPI documentation
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new io.swagger.v3.oas.models.info.Info()
                .title("User Service API")
                .version("v1.0")
                .description("""
                    ## User Service API

                    The User Service manages core user identity, profiles, roles, consents, addresses, and session listings.

                    ### Key Features:
                    - **Field-level PII Encryption**: All sensitive data is encrypted using Vault Transit
                    - **HMAC Search**: Email and phone uniqueness/search using deterministic HMAC
                    - **GDPR/DPDP Compliance**: Full data subject rights support
                    - **Role-based Authorization**: Hierarchical role management
                    - **Audit Trail**: Comprehensive audit logging for all operations
                    - **Session Management**: Read-only session metadata (actual sessions managed by Auth Service)

                    ### API Versioning:
                    - Base Path: `/api/user-service/v1`
                    - All endpoints are versioned and backward compatible

                    ### Authentication:
                    - **Public Endpoints**: User CRUD operations (require session token)
                    - **Admin Endpoints**: Administrative operations (require ROLE_ADMIN)
                    - **Internal Endpoints**: Service-to-service communication (require API key)

                    ### Data Masking:
                    - PII fields are masked in responses based on user roles
                    - Admin users see full data, regular users see masked data
                    - Internal services receive minimal safe cards

                    ### Rate Limiting:
                    - Public API: 100 RPS per user
                    - Internal API: 200 RPS per service
                    - Lookup API: Higher limits for service consumers
                    """)
                .contact(new io.swagger.v3.oas.models.info.Contact()
                    .name("MySillyDreams API Team")
                    .email("api-support@mysillydreams.com")
                    .url("https://mysillydreams.com/support"))
                .license(new io.swagger.v3.oas.models.info.License()
                    .name("Apache 2.0")
                    .url("http://www.apache.org/licenses/LICENSE-2.0.html")))
            .externalDocs(new io.swagger.v3.oas.models.ExternalDocumentation()
                .description("User Service Documentation")
                .url("https://docs.mysillydreams.com/services/user-service"));
    }
}
