package com.mysillydreams.payment.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Event representing a payment request
 */
public record PaymentRequestedEvent(
        UUID invoiceId,
        UUID enrollmentId,
        UUID userId,
        BigDecimal amount,
        String currency,
        String description
) {}
