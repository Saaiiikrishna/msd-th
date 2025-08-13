package com.mysillydreams.payment.dto;

import com.mysillydreams.payment.domain.PaymentMethodType;

import java.util.UUID;

/**
 * Request to save a payment method
 */
public record SavePaymentMethodRequest(
        UUID userId,
        PaymentMethodType paymentType,
        String displayName,
        boolean isDefault,
        String customerEmail,
        
        // Card details
        String cardNumber,
        String cardHolderName,
        Integer cardExpiryMonth,
        Integer cardExpiryYear,
        String cardCvv,
        String cardBrand,
        String cardType,
        String cardIssuer,
        
        // UPI details
        String upiVpa,
        
        // Wallet details
        String walletProvider,
        String walletPhone,
        
        // Net banking details
        String bankName
) {}
