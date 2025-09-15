package com.mysillydreams.payment.domain;

/**
 * Types of payment methods supported
 */
public enum PaymentMethodType {
    CARD,           // Credit/Debit cards
    UPI,            // UPI payments
    WALLET,         // Digital wallets (Paytm, PhonePe, etc.)
    NET_BANKING     // Net banking
}
