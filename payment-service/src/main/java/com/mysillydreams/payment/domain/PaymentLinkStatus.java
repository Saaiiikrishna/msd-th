package com.mysillydreams.payment.domain;

/**
 * Payment Link Status enumeration
 * Represents the various states of a Razorpay Payment Link
 */
public enum PaymentLinkStatus {
    CREATED,    // Payment link created and active
    PAID,       // Payment completed successfully
    PARTIALLY_PAID, // Partial payment received (if enabled)
    EXPIRED,    // Payment link expired
    CANCELLED   // Payment link cancelled
}
