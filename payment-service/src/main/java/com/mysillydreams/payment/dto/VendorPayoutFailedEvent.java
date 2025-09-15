package com.mysillydreams.payment.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Event representing a failed vendor payout
 */
public record VendorPayoutFailedEvent(
        UUID payoutId,
        UUID vendorId,
        BigDecimal amount,
        String currency,
        String errorMessage
) {}
