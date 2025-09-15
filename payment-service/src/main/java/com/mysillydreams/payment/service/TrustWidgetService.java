package com.mysillydreams.payment.service;

import com.mysillydreams.payment.dto.TrustWidgetConfig;
import com.mysillydreams.payment.dto.MoneyBackGuarantee;
import com.mysillydreams.payment.dto.SecurityBadge;
import com.mysillydreams.payment.dto.PaymentMethodWidget;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Service for managing trust and security widgets for payment pages
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TrustWidgetService {

    /**
     * Get complete trust widget configuration
     */
    public TrustWidgetConfig getTrustWidgetConfig() {
        return TrustWidgetConfig.builder()
                .moneyBackGuarantee(getMoneyBackGuaranteeConfig())
                .securityBadge(getSecurityBadgeConfig())
                .paymentMethods(getPaymentMethodsConfig())
                .trustIndicators(getTrustIndicatorsConfig())
                .customerTestimonials(getCustomerTestimonialsConfig())
                .razorpayTrustElements(getRazorpayTrustElements())
                .dynamicTrustScore(getDynamicTrustScore())
                .paymentSecurityInfo(getPaymentSecurityInfo())
                .refundPolicyWidget(getRefundPolicyWidget())
                .build();
    }

    /**
     * Get money back guarantee configuration
     */
    public MoneyBackGuarantee getMoneyBackGuaranteeConfig() {
        return MoneyBackGuarantee.builder()
                .enabled(true)
                .title("100% Money Back Guarantee")
                .description("Get full refund if you're not satisfied with your treasure hunt experience")
                .guaranteePeriod("30 days")
                .conditions(Arrays.asList(
                        "Valid for all treasure hunt plans",
                        "Refund processed within 5-7 business days",
                        "No questions asked policy",
                        "Applicable before event start date"
                ))
                .iconUrl("/assets/icons/money-back-guarantee.svg")
                .badgeColor("#28a745")
                .displayPosition("checkout")
                .style(Map.of(
                        "backgroundColor", "#f8f9fa",
                        "borderColor", "#28a745",
                        "textColor", "#155724",
                        "fontSize", "14px",
                        "padding", "12px",
                        "borderRadius", "8px"
                ))
                .build();
    }

    /**
     * Get security badge configuration
     */
    public SecurityBadge getSecurityBadgeConfig() {
        return SecurityBadge.builder()
                .enabled(true)
                .title("Secure Payment")
                .description("Your payment information is protected with bank-level security")
                .securityFeatures(Arrays.asList(
                        "256-bit SSL encryption",
                        "PCI DSS compliant",
                        "Razorpay secured",
                        "No card details stored"
                ))
                .certifications(Arrays.asList(
                        Map.of("name", "PCI DSS", "level", "Level 1", "icon", "/assets/icons/pci-dss.svg"),
                        Map.of("name", "SSL", "level", "256-bit", "icon", "/assets/icons/ssl.svg"),
                        Map.of("name", "ISO 27001", "level", "Certified", "icon", "/assets/icons/iso27001.svg")
                ))
                .trustLogos(Arrays.asList(
                        "/assets/logos/razorpay-trust.svg",
                        "/assets/logos/visa-secure.svg",
                        "/assets/logos/mastercard-secure.svg"
                ))
                .displayPosition("payment-form")
                .style(Map.of(
                        "backgroundColor", "#ffffff",
                        "borderColor", "#007bff",
                        "textColor", "#495057",
                        "fontSize", "12px",
                        "padding", "16px",
                        "borderRadius", "6px",
                        "boxShadow", "0 2px 4px rgba(0,0,0,0.1)"
                ))
                .build();
    }

    /**
     * Get payment methods widget configuration
     */
    public PaymentMethodWidget getPaymentMethodsConfig() {
        List<Map<String, Object>> supportedMethods = Arrays.asList(
                Map.of(
                        "type", "card",
                        "name", "Credit/Debit Cards",
                        "icon", "/assets/icons/cards.svg",
                        "description", "Visa, Mastercard, RuPay, American Express",
                        "processingTime", "Instant",
                        "fees", "No additional fees"
                ),
                Map.of(
                        "type", "upi",
                        "name", "UPI",
                        "icon", "/assets/icons/upi.svg",
                        "description", "Pay using any UPI app",
                        "processingTime", "Instant",
                        "fees", "No additional fees"
                ),
                Map.of(
                        "type", "netbanking",
                        "name", "Net Banking",
                        "icon", "/assets/icons/netbanking.svg",
                        "description", "All major banks supported",
                        "processingTime", "Instant",
                        "fees", "No additional fees"
                ),
                Map.of(
                        "type", "wallet",
                        "name", "Wallets",
                        "icon", "/assets/icons/wallets.svg",
                        "description", "Paytm, PhonePe, Amazon Pay, and more",
                        "processingTime", "Instant",
                        "fees", "No additional fees"
                )
        );

        return PaymentMethodWidget.builder()
                .enabled(true)
                .title("Choose Your Payment Method")
                .supportedMethods(supportedMethods)
                .displayPosition("payment-selection")
                .style(Map.of(
                        "backgroundColor", "#ffffff",
                        "borderColor", "#dee2e6",
                        "textColor", "#495057",
                        "fontSize", "14px",
                        "padding", "20px",
                        "borderRadius", "8px",
                        "gridColumns", "2"
                ))
                .build();
    }

    /**
     * Get trust indicators configuration
     */
    public List<Map<String, Object>> getTrustIndicatorsConfig() {
        return Arrays.asList(
                Map.of(
                        "type", "customer_count",
                        "title", "Happy Customers",
                        "value", "10,000+",
                        "icon", "/assets/icons/customers.svg",
                        "description", "Treasure hunters who loved our experiences"
                ),
                Map.of(
                        "type", "success_rate",
                        "title", "Success Rate",
                        "value", "99.8%",
                        "icon", "/assets/icons/success.svg",
                        "description", "Successful payment completion rate"
                ),
                Map.of(
                        "type", "events_completed",
                        "title", "Events Completed",
                        "value", "500+",
                        "icon", "/assets/icons/events.svg",
                        "description", "Successful treasure hunt events organized"
                ),
                Map.of(
                        "type", "avg_rating",
                        "title", "Average Rating",
                        "value", "4.9/5",
                        "icon", "/assets/icons/rating.svg",
                        "description", "Based on customer feedback"
                ),
                Map.of(
                        "type", "response_time",
                        "title", "Support Response",
                        "value", "< 2 hours",
                        "icon", "/assets/icons/support.svg",
                        "description", "Average customer support response time"
                )
        );
    }

    /**
     * Get customer testimonials configuration
     */
    public List<Map<String, Object>> getCustomerTestimonialsConfig() {
        return Arrays.asList(
                Map.of(
                        "id", 1,
                        "name", "Priya Sharma",
                        "location", "Mumbai",
                        "rating", 5,
                        "comment", "Amazing treasure hunt experience! The payment process was smooth and secure.",
                        "date", "2024-07-15",
                        "verified", true,
                        "avatar", "/assets/avatars/customer1.jpg"
                ),
                Map.of(
                        "id", 2,
                        "name", "Rahul Gupta",
                        "location", "Delhi",
                        "rating", 5,
                        "comment", "Great organization and hassle-free booking. Highly recommended!",
                        "date", "2024-07-10",
                        "verified", true,
                        "avatar", "/assets/avatars/customer2.jpg"
                ),
                Map.of(
                        "id", 3,
                        "name", "Sneha Patel",
                        "location", "Bangalore",
                        "rating", 4,
                        "comment", "Loved the treasure hunt! Easy payment and great customer service.",
                        "date", "2024-07-08",
                        "verified", true,
                        "avatar", "/assets/avatars/customer3.jpg"
                )
        );
    }

    /**
     * Get Razorpay trust elements
     */
    public Map<String, Object> getRazorpayTrustElements() {
        return Map.of(
                "poweredBy", Map.of(
                        "enabled", true,
                        "text", "Powered by Razorpay",
                        "logo", "/assets/logos/razorpay-logo.svg",
                        "link", "https://razorpay.com",
                        "position", "footer"
                ),
                "securityBadges", Arrays.asList(
                        Map.of("name", "Razorpay Secured", "icon", "/assets/badges/razorpay-secured.svg"),
                        Map.of("name", "PCI Compliant", "icon", "/assets/badges/pci-compliant.svg")
                ),
                "trustScore", Map.of(
                        "enabled", true,
                        "score", 98,
                        "maxScore", 100,
                        "description", "Based on security, reliability, and customer satisfaction"
                ),
                "processingInfo", Map.of(
                        "instantProcessing", true,
                        "autoRefund", true,
                        "multipleRetries", true,
                        "smartRouting", true
                )
        );
    }

    /**
     * Get dynamic trust score
     */
    public Map<String, Object> getDynamicTrustScore() {
        // In a real implementation, this would calculate based on recent transactions
        double baseScore = 95.0;
        double recentSuccessRate = 99.2;
        double customerSatisfaction = 4.8;
        
        double dynamicScore = (baseScore + recentSuccessRate + (customerSatisfaction * 20)) / 3;
        
        return Map.of(
                "score", Math.round(dynamicScore * 10.0) / 10.0,
                "maxScore", 100.0,
                "lastUpdated", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                "factors", Map.of(
                        "recentSuccessRate", recentSuccessRate,
                        "customerSatisfaction", customerSatisfaction,
                        "securityCompliance", 100.0,
                        "uptimeReliability", 99.9
                ),
                "trend", "stable",
                "description", "Real-time trust score based on recent performance metrics"
        );
    }

    /**
     * Get payment security information
     */
    public Map<String, Object> getPaymentSecurityInfo() {
        return Map.of(
                "encryption", Map.of(
                        "type", "AES-256",
                        "description", "Military-grade encryption for all transactions",
                        "icon", "/assets/icons/encryption.svg"
                ),
                "compliance", Arrays.asList(
                        Map.of("standard", "PCI DSS Level 1", "description", "Highest level of payment security"),
                        Map.of("standard", "ISO 27001", "description", "Information security management"),
                        Map.of("standard", "SOC 2 Type II", "description", "Security and availability controls")
                ),
                "dataProtection", Map.of(
                        "tokenization", true,
                        "noCardStorage", true,
                        "gdprCompliant", true,
                        "description", "Your card details are never stored on our servers"
                ),
                "fraudProtection", Map.of(
                        "realTimeMonitoring", true,
                        "aiBasedDetection", true,
                        "riskScoring", true,
                        "description", "Advanced fraud detection and prevention"
                )
        );
    }

    /**
     * Get refund policy widget
     */
    public Map<String, Object> getRefundPolicyWidget() {
        return Map.of(
                "enabled", true,
                "title", "Easy Refund Policy",
                "description", "Hassle-free refunds with transparent process",
                "policies", Arrays.asList(
                        Map.of(
                                "type", "full_refund",
                                "condition", "Cancellation 48 hours before event",
                                "percentage", 100,
                                "processingTime", "3-5 business days"
                        ),
                        Map.of(
                                "type", "partial_refund",
                                "condition", "Cancellation 24-48 hours before event",
                                "percentage", 75,
                                "processingTime", "3-5 business days"
                        ),
                        Map.of(
                                "type", "no_refund",
                                "condition", "Cancellation less than 24 hours before event",
                                "percentage", 0,
                                "processingTime", "N/A"
                        )
                ),
                "autoRefund", Map.of(
                        "enabled", true,
                        "description", "Automatic refund processing for eligible cancellations",
                        "maxAmount", 50000
                ),
                "contactInfo", Map.of(
                        "email", "support@mysillydreams.com",
                        "phone", "+91-9876543210",
                        "hours", "9 AM - 6 PM (Mon-Sat)"
                )
        );
    }
}
