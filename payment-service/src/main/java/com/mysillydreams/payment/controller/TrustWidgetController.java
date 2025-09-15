package com.mysillydreams.payment.controller;

import com.mysillydreams.payment.dto.TrustWidgetConfig;
import com.mysillydreams.payment.service.TrustWidgetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST API for trust and security widgets
 */
@RestController
@RequestMapping("/api/payments/v1/trust")
@RequiredArgsConstructor
@Tag(name = "Trust Widgets", description = "Trust and security widget APIs")
public class TrustWidgetController {

    private final TrustWidgetService trustWidgetService;

    /**
     * Get complete trust widget configuration
     */
    @GetMapping("/config")
    @Operation(summary = "Get trust widget configuration", 
               description = "Returns complete configuration for all trust and security widgets")
    public ResponseEntity<TrustWidgetConfig> getTrustWidgetConfig() {
        TrustWidgetConfig config = trustWidgetService.getTrustWidgetConfig();
        return ResponseEntity.ok(config);
    }

    /**
     * Get money back guarantee widget
     */
    @GetMapping("/money-back-guarantee")
    @Operation(summary = "Get money back guarantee widget", 
               description = "Returns money back guarantee widget configuration")
    public ResponseEntity<Map<String, Object>> getMoneyBackGuarantee() {
        var guarantee = trustWidgetService.getMoneyBackGuaranteeConfig();
        return ResponseEntity.ok(Map.of("moneyBackGuarantee", guarantee));
    }

    /**
     * Get security badge widget
     */
    @GetMapping("/security-badge")
    @Operation(summary = "Get security badge widget", 
               description = "Returns security badge widget configuration")
    public ResponseEntity<Map<String, Object>> getSecurityBadge() {
        var badge = trustWidgetService.getSecurityBadgeConfig();
        return ResponseEntity.ok(Map.of("securityBadge", badge));
    }

    /**
     * Get payment methods widget
     */
    @GetMapping("/payment-methods")
    @Operation(summary = "Get payment methods widget", 
               description = "Returns supported payment methods widget configuration")
    public ResponseEntity<Map<String, Object>> getPaymentMethods() {
        var methods = trustWidgetService.getPaymentMethodsConfig();
        return ResponseEntity.ok(Map.of("paymentMethods", methods));
    }

    /**
     * Get trust indicators
     */
    @GetMapping("/indicators")
    @Operation(summary = "Get trust indicators", 
               description = "Returns trust indicators like customer count, success rate, etc.")
    public ResponseEntity<Map<String, Object>> getTrustIndicators() {
        List<Map<String, Object>> indicators = trustWidgetService.getTrustIndicatorsConfig();
        return ResponseEntity.ok(Map.of("trustIndicators", indicators));
    }

    /**
     * Get customer testimonials
     */
    @GetMapping("/testimonials")
    @Operation(summary = "Get customer testimonials", 
               description = "Returns customer testimonials for trust building")
    public ResponseEntity<Map<String, Object>> getCustomerTestimonials() {
        List<Map<String, Object>> testimonials = trustWidgetService.getCustomerTestimonialsConfig();
        return ResponseEntity.ok(Map.of("testimonials", testimonials));
    }

    /**
     * Get Razorpay trust elements
     */
    @GetMapping("/razorpay-elements")
    @Operation(summary = "Get Razorpay trust elements", 
               description = "Returns Razorpay-specific trust elements and branding")
    public ResponseEntity<Map<String, Object>> getRazorpayTrustElements() {
        Map<String, Object> elements = trustWidgetService.getRazorpayTrustElements();
        return ResponseEntity.ok(elements);
    }

    /**
     * Get dynamic trust score
     */
    @GetMapping("/score")
    @Operation(summary = "Get dynamic trust score", 
               description = "Returns real-time trust score based on recent transactions")
    public ResponseEntity<Map<String, Object>> getDynamicTrustScore() {
        Map<String, Object> score = trustWidgetService.getDynamicTrustScore();
        return ResponseEntity.ok(score);
    }

    /**
     * Get payment security information
     */
    @GetMapping("/security-info")
    @Operation(summary = "Get payment security information", 
               description = "Returns detailed payment security and compliance information")
    public ResponseEntity<Map<String, Object>> getPaymentSecurityInfo() {
        Map<String, Object> securityInfo = trustWidgetService.getPaymentSecurityInfo();
        return ResponseEntity.ok(securityInfo);
    }

    /**
     * Get refund policy widget
     */
    @GetMapping("/refund-policy")
    @Operation(summary = "Get refund policy widget", 
               description = "Returns refund policy widget configuration")
    public ResponseEntity<Map<String, Object>> getRefundPolicyWidget() {
        Map<String, Object> refundPolicy = trustWidgetService.getRefundPolicyWidget();
        return ResponseEntity.ok(refundPolicy);
    }
}
