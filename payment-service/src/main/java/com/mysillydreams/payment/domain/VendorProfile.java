package com.mysillydreams.payment.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Vendor profile with payment and commission information
 */
@Entity
@Table(name = "vendor_profiles",
       indexes = {
           @Index(name = "idx_vendor_profile_vendor_id", columnList = "vendor_id", unique = true),
           @Index(name = "idx_vendor_profile_fund_account", columnList = "razorpay_fund_account_id")
       })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendorProfile {
    
    @Id
    @GeneratedValue
    private UUID id;
    
    @Column(name = "vendor_id", nullable = false, unique = true)
    private UUID vendorId;
    
    @Column(name = "vendor_name", nullable = false, length = 200)
    private String vendorName;
    
    @Column(name = "vendor_email", nullable = false, length = 200)
    private String vendorEmail;
    
    @Column(name = "vendor_phone", length = 20)
    private String vendorPhone;
    
    // Bank account details
    @Column(name = "bank_account_number", nullable = false, length = 50)
    private String bankAccountNumber;
    
    @Column(name = "bank_ifsc_code", nullable = false, length = 11)
    private String bankIfscCode;
    
    @Column(name = "bank_account_holder_name", nullable = false, length = 200)
    private String bankAccountHolderName;
    
    @Column(name = "bank_name", length = 200)
    private String bankName;
    
    // Razorpay integration
    @Column(name = "razorpay_contact_id", length = 50)
    private String razorpayContactId;
    
    @Column(name = "razorpay_fund_account_id", nullable = true, length = 50)
    private String razorpayFundAccountId; // Optional - not needed when using bank account details directly
    
    // Commission settings
    @Column(name = "commission_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal commissionRate; // Percentage (e.g., 2.50 for 2.5%)
    
    @Column(name = "minimum_payout_amount", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal minimumPayoutAmount = BigDecimal.valueOf(100); // Minimum ₹100
    
    @Column(name = "maximum_payout_amount", precision = 12, scale = 2)
    private BigDecimal maximumPayoutAmount; // null = no limit
    
    // Status and metadata
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
    
    @Column(name = "is_verified", nullable = false)
    @Builder.Default
    private Boolean isVerified = false;
    
    @Column(name = "verification_date")
    private Instant verificationDate;
    
    @Column(name = "last_payout_date")
    private Instant lastPayoutDate;
    
    @Column(name = "total_payouts_count", nullable = false)
    @Builder.Default
    private Long totalPayoutsCount = 0L;
    
    @Column(name = "total_payouts_amount", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalPayoutsAmount = BigDecimal.ZERO;
    
    @Column(name = "total_commission_earned", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalCommissionEarned = BigDecimal.ZERO;
    
    // Timestamps
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
     * Update payout statistics
     */
    public void updatePayoutStatistics(BigDecimal payoutAmount, BigDecimal commissionAmount) {
        this.totalPayoutsCount++;
        this.totalPayoutsAmount = this.totalPayoutsAmount.add(payoutAmount);
        this.totalCommissionEarned = this.totalCommissionEarned.add(commissionAmount);
        this.lastPayoutDate = Instant.now();
    }
    
    /**
     * Mark as verified
     */
    public void markAsVerified() {
        this.isVerified = true;
        this.verificationDate = Instant.now();
    }
    
    /**
     * Check if payout amount is within limits
     */
    public boolean isPayoutAmountValid(BigDecimal amount) {
        if (amount.compareTo(minimumPayoutAmount) < 0) {
            return false;
        }
        
        if (maximumPayoutAmount != null && amount.compareTo(maximumPayoutAmount) > 0) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Calculate commission for a given amount
     */
    public BigDecimal calculateCommission(BigDecimal grossAmount) {
        return grossAmount.multiply(commissionRate)
                .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
    }
    
    /**
     * Calculate net payout amount
     */
    public BigDecimal calculateNetAmount(BigDecimal grossAmount) {
        BigDecimal commission = calculateCommission(grossAmount);
        return grossAmount.subtract(commission);
    }
}
