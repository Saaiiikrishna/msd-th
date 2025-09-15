package com.mysillydreams.payment.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Payment Link entity for Razorpay Payment Links
 * Represents shareable payment links for treasure hunt enrollments
 */
@Entity
@Table(name = "payment_links")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentLink {

    @Id
    private UUID id;

    @Column(name = "invoice_id", nullable = false)
    private UUID invoiceId;

    @Column(name = "razorpay_payment_link_id", nullable = false, unique = true, length = 64)
    private String razorpayPaymentLinkId;

    @Column(name = "short_url", nullable = false, length = 500)
    private String shortUrl;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "description", length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private PaymentLinkStatus status;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    // Helper methods
    public boolean isActive() {
        return status == PaymentLinkStatus.CREATED && 
               (expiresAt == null || expiresAt.isAfter(LocalDateTime.now()));
    }

    public boolean isPaid() {
        return status == PaymentLinkStatus.PAID;
    }

    public boolean isExpired() {
        return status == PaymentLinkStatus.EXPIRED || 
               (expiresAt != null && expiresAt.isBefore(LocalDateTime.now()));
    }
}
