package com.mysillydreams.payment.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Event representing a successful vendor payout
 */
public record VendorPayoutSucceededEvent(
        UUID payoutId,
        UUID vendorId,
        BigDecimal amount,
        String currency,
        String razorpayPayoutId
) {}
