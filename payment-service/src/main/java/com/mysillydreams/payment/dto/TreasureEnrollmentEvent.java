package com.mysillydreams.payment.dto;

import com.mysillydreams.payment.domain.EnrollmentType;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Event from Treasure service for enrollment payment processing
 */
public record TreasureEnrollmentEvent(
        UUID enrollmentId,
        String registrationId,
        UUID userId,
        UUID planId,
        String planTitle,
        EnrollmentType enrollmentType,
        String teamName,
        Integer teamSize,
        
        // Pricing details
        BigDecimal baseAmount,
        BigDecimal discountAmount,
        BigDecimal taxAmount,
        BigDecimal convenienceFee,
        BigDecimal platformFee,
        BigDecimal totalAmount,
        String currency,
        
        // Discount details
        String promoCode,
        String promotionName,
        
        // Billing information
        String billingName,
        String billingEmail,
        String billingPhone,
        String billingAddress,

        // Vendor information for payouts
        UUID vendorId, // null if no vendor assigned to plan
        BigDecimal vendorCommissionRate // vendor-specific commission rate
) {}
