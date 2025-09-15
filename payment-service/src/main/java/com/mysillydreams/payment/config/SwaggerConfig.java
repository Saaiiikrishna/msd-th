package com.mysillydreams.payment.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI paymentServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Payment Service API")
                        .description("""
                                # Payment Service - Treasure Hunt Payment Processing
                                
                                The Payment Service handles all payment processing for the Treasure Hunt platform,
                                including invoice generation, payment method storage, and vendor payouts.
                                
                                ## Key Features
                                - **Invoice Generation**: Automatic invoice creation with registration IDs
                                - **Razorpay Integration**: Complete INR payment processing with test/live modes
                                - **Payment Method Storage**: Secure tokenization and card storage
                                - **Vendor Payouts**: Automatic commission-based payouts to vendors
                                - **Payment Security**: PCI-compliant payment handling with tokenization
                                - **Multi-Payment Methods**: Cards, UPI, Wallets, Net Banking support
                                
                                ## Razorpay Test Mode
                                - Use test API keys for development and testing
                                - Test card numbers available in Razorpay documentation
                                - All transactions are simulated in test mode
                                
                                ## Security
                                - Card numbers are never stored (only last 4 digits)
                                - CVV is never stored
                                - Razorpay tokenization for secure payments
                                - PCI DSS compliant architecture
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Payment Service Team")
                                .email("payments@treasurehunt.com")
                                .url("https://treasurehunt.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8081")
                                .description("Local Development Server"),
                        new Server()
                                .url("https://payments.treasurehunt.com")
                                .description("Production Server")))
                .tags(List.of(
                        new Tag()
                                .name("Treasure Hunt Payments")
                                .description("Payment processing for treasure hunt enrollments"),
                        new Tag()
                                .name("Invoice Management")
                                .description("Invoice generation and management"),
                        new Tag()
                                .name("Payment Methods")
                                .description("Saved payment method management with tokenization"),
                        new Tag()
                                .name("Vendor Payouts")
                                .description("Vendor payout management and commission handling"),
                        new Tag()
                                .name("Payment Webhooks")
                                .description("Razorpay webhook handling for payment status updates"),
                        new Tag()
                                .name("System Health")
                                .description("System health checks and monitoring endpoints")));
    }
}
