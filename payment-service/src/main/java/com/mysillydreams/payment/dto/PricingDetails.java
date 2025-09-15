package com.mysillydreams.payment.dto;

import java.math.BigDecimal;

/**
 * Pricing details for invoice generation
 */
public record PricingDetails(
        BigDecimal baseAmount,
        BigDecimal discountAmount,
        BigDecimal taxAmount,
        BigDecimal convenienceFee,
        BigDecimal platformFee,
        BigDecimal totalAmount,
        String promoCode,
        String promotionName
) {}
