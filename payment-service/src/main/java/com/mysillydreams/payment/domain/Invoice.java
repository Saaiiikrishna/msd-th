package com.mysillydreams.payment.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Invoice entity for treasure hunt plan enrollments
 */
@Entity
@Table(name = "invoices")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Invoice {
    
    @Id
    @GeneratedValue
    private UUID id;
    
    @Column(name = "invoice_number", nullable = false, unique = true, length = 50)
    private String invoiceNumber; // Format: TH-MMYY-IND/TEAM-XXXXXX (same as registration ID)
    
    @Column(name = "enrollment_id", nullable = false)
    private UUID enrollmentId;
    
    @Column(name = "registration_id", nullable = false, length = 50)
    private String registrationId; // TH-MMYY-IND/TEAM-XXXXXX
    
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    
    @Column(name = "plan_id", nullable = false)
    private UUID planId;
    
    @Column(name = "plan_title", nullable = false, length = 200)
    private String planTitle;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "enrollment_type", nullable = false)
    private EnrollmentType enrollmentType;
    
    @Column(name = "team_name", length = 255)
    private String teamName;
    
    @Column(name = "team_size")
    private Integer teamSize;
    
    // Pricing details
    @Column(name = "base_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal baseAmount;
    
    @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;
    
    @Column(name = "tax_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal taxAmount = BigDecimal.ZERO;
    
    @Column(name = "convenience_fee", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal convenienceFee = BigDecimal.ZERO;
    
    @Column(name = "platform_fee", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal platformFee = BigDecimal.ZERO;
    
    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;
    
    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private String currency = "INR";
    
    // Payment details
    @Column(name = "payment_transaction_id")
    private UUID paymentTransactionId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;
    
    @Column(name = "payment_method", length = 50)
    private String paymentMethod;
    
    @Column(name = "razorpay_order_id", length = 64)
    private String razorpayOrderId;
    
    @Column(name = "razorpay_payment_id", length = 64)
    private String razorpayPaymentId;
    
    // Discount details
    @Column(name = "promo_code", length = 50)
    private String promoCode;
    
    @Column(name = "promotion_name", length = 100)
    private String promotionName;
    
    // Billing information
    @Column(name = "billing_name", nullable = false, length = 200)
    private String billingName;
    
    @Column(name = "billing_email", nullable = false, length = 200)
    private String billingEmail;
    
    @Column(name = "billing_phone", length = 20)
    private String billingPhone;
    
    @Column(name = "billing_address", columnDefinition = "TEXT")
    private String billingAddress;
    
    // Timestamps
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    @Column(name = "paid_at")
    private Instant paidAt;
    
    @Version
    @Column(nullable = false)
    @Builder.Default
    private Long version = 0L;
    
    /**
     * Mark invoice as paid
     */
    public void markAsPaid(String paymentMethod, String razorpayPaymentId) {
        this.paymentStatus = PaymentStatus.PAID;
        this.paymentMethod = paymentMethod;
        this.razorpayPaymentId = razorpayPaymentId;
        this.paidAt = Instant.now();
    }
    
    /**
     * Mark invoice as failed
     */
    public void markAsFailed() {
        this.paymentStatus = PaymentStatus.FAILED;
    }
}
