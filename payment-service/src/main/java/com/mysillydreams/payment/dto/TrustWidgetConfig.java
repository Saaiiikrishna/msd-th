package com.mysillydreams.payment.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Complete trust widget configuration
 */
@Data
@Builder
public class TrustWidgetConfig {
    private MoneyBackGuarantee moneyBackGuarantee;
    private SecurityBadge securityBadge;
    private PaymentMethodWidget paymentMethods;
    private List<Map<String, Object>> trustIndicators;
    private List<Map<String, Object>> customerTestimonials;
    private Map<String, Object> razorpayTrustElements;
    private Map<String, Object> dynamicTrustScore;
    private Map<String, Object> paymentSecurityInfo;
    private Map<String, Object> refundPolicyWidget;
}


