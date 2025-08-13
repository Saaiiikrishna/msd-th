package com.mysillydreams.payment.domain;

/**
 * Payment status for invoices and transactions
 */
public enum PaymentStatus {
    PENDING,
    PAID,
    FAILED,
    REFUNDED,
    CANCELLED
}
