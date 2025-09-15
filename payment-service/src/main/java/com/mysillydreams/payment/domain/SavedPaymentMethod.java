package com.mysillydreams.payment.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.YearMonth;
import java.util.UUID;

/**
 * Secure storage for user payment methods with tokenization
 */
@Entity
@Table(name = "saved_payment_methods",
       indexes = {
           @Index(name = "idx_payment_method_user", columnList = "user_id"),
           @Index(name = "idx_payment_method_token", columnList = "razorpay_token", unique = true)
       })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavedPaymentMethod {
    
    @Id
    @GeneratedValue
    private UUID id;
    
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", nullable = false)
    private PaymentMethodType paymentType;
    
    // Razorpay tokenization
    @Column(name = "razorpay_token", unique = true, length = 100)
    private String razorpayToken; // Razorpay's token for the payment method
    
    @Column(name = "razorpay_customer_id", length = 100)
    private String razorpayCustomerId; // Razorpay customer ID
    
    // Card details (masked for security)
    @Column(name = "card_last_four", length = 4)
    private String cardLastFour; // Only last 4 digits stored
    
    @Column(name = "card_brand", length = 20)
    private String cardBrand; // VISA, MASTERCARD, etc.
    
    @Column(name = "card_type", length = 20)
    private String cardType; // CREDIT, DEBIT
    
    @Column(name = "card_expiry_month")
    private Integer cardExpiryMonth;
    
    @Column(name = "card_expiry_year")
    private Integer cardExpiryYear;
    
    @Column(name = "card_issuer", length = 100)
    private String cardIssuer; // Bank name
    
    // UPI details
    @Column(name = "upi_vpa", length = 100)
    private String upiVpa; // UPI Virtual Payment Address (masked)
    
    // Wallet details
    @Column(name = "wallet_provider", length = 50)
    private String walletProvider; // PAYTM, PHONEPE, etc.
    
    @Column(name = "wallet_phone", length = 15)
    private String walletPhone; // Masked phone number
    
    // Metadata
    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName; // User-friendly name for the payment method
    
    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private Boolean isDefault = false;
    
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
    
    @Column(name = "last_used_at")
    private Instant lastUsedAt;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    @Version
    @Column(nullable = false)
    @Builder.Default
    private Long version = 0L;
    
    /**
     * Check if card is expired
     */
    public boolean isCardExpired() {
        if (paymentType != PaymentMethodType.CARD || cardExpiryMonth == null || cardExpiryYear == null) {
            return false;
        }
        
        YearMonth expiry = YearMonth.of(cardExpiryYear, cardExpiryMonth);
        YearMonth current = YearMonth.now();
        
        return expiry.isBefore(current);
    }
    
    /**
     * Get masked card number for display
     */
    public String getMaskedCardNumber() {
        if (paymentType != PaymentMethodType.CARD || cardLastFour == null) {
            return null;
        }
        return "XXXX XXXX XXXX " + cardLastFour;
    }
    
    /**
     * Get masked UPI VPA for display
     */
    public String getMaskedUpiVpa() {
        if (paymentType != PaymentMethodType.UPI || upiVpa == null) {
            return null;
        }
        
        String[] parts = upiVpa.split("@");
        if (parts.length != 2) {
            return upiVpa;
        }
        
        String username = parts[0];
        String provider = parts[1];
        
        if (username.length() <= 3) {
            return username + "@" + provider;
        }
        
        String masked = username.substring(0, 2) + "***" + username.substring(username.length() - 1);
        return masked + "@" + provider;
    }
    
    /**
     * Mark as used
     */
    public void markAsUsed() {
        this.lastUsedAt = Instant.now();
    }
    
    /**
     * Set as default payment method
     */
    public void setAsDefault() {
        this.isDefault = true;
    }
    
    /**
     * Remove default status
     */
    public void removeDefault() {
        this.isDefault = false;
    }
}
