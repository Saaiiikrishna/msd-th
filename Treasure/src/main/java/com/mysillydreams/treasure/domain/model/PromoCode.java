package com.mysillydreams.treasure.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Promo codes for discounts on treasure hunt plan registrations
 */
@Entity 
@Table(name = "promo_code",
       indexes = {
           @Index(name = "idx_promo_code_code", columnList = "code", unique = true),
           @Index(name = "idx_promo_code_active", columnList = "is_active, valid_from, valid_until")
       })
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class PromoCode {
    
    @Id @GeneratedValue 
    private UUID id;
    
    @Column(nullable = false, unique = true, length = 50)
    private String code;
    
    @Column(nullable = false, length = 200)
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false)
    private DiscountType discountType;
    
    @Column(name = "discount_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountValue;
    
    @Column(name = "max_discount_amount", precision = 10, scale = 2)
    private BigDecimal maxDiscountAmount; // For percentage discounts
    
    @Column(name = "min_order_amount", precision = 10, scale = 2)
    private BigDecimal minOrderAmount; // Minimum order value to apply discount
    
    @Column(name = "usage_limit")
    private Integer usageLimit; // null = unlimited
    
    @Column(name = "usage_count", nullable = false)
    @Builder.Default
    private Integer usageCount = 0;
    
    @Column(name = "usage_limit_per_user")
    private Integer usageLimitPerUser; // null = unlimited per user
    
    @Column(name = "valid_from", nullable = false)
    private OffsetDateTime validFrom;
    
    @Column(name = "valid_until", nullable = false)
    private OffsetDateTime validUntil;
    
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
    
    // Applicability filters
    @Enumerated(EnumType.STRING)
    @Column(name = "applicable_enrollment_type")
    private EnrollmentType applicableEnrollmentType; // null = all types
    
    @Enumerated(EnumType.STRING)
    @Column(name = "applicable_difficulty")
    private Difficulty applicableDifficulty; // null = all difficulties
    
    @Column(name = "applicable_plan_ids", columnDefinition = "TEXT")
    private String applicablePlanIds; // JSON array of plan UUIDs, null = all plans
    
    @Column(name = "first_time_users_only", nullable = false)
    @Builder.Default
    private Boolean firstTimeUsersOnly = false;
    
    @Column(name = "created_by")
    private UUID createdBy;
    
    @Column(nullable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();
    
    @Column(nullable = false)
    @Builder.Default
    private OffsetDateTime updatedAt = OffsetDateTime.now();
    
    @PreUpdate 
    void touch() { 
        this.updatedAt = OffsetDateTime.now(); 
    }
    
    /**
     * Check if promo code is currently valid
     */
    public boolean isCurrentlyValid() {
        OffsetDateTime now = OffsetDateTime.now();
        return isActive && 
               now.isAfter(validFrom) && 
               now.isBefore(validUntil) &&
               (usageLimit == null || usageCount < usageLimit);
    }
    
    /**
     * Check if promo code can be used by a specific user
     */
    public boolean canBeUsedBy(UUID userId, int userUsageCount) {
        return isCurrentlyValid() && 
               (usageLimitPerUser == null || userUsageCount < usageLimitPerUser);
    }
    
    /**
     * Calculate discount amount for a given order value
     */
    public BigDecimal calculateDiscount(BigDecimal orderAmount) {
        if (!isCurrentlyValid() || 
            (minOrderAmount != null && orderAmount.compareTo(minOrderAmount) < 0)) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal discount = switch (discountType) {
            case PERCENTAGE -> {
                BigDecimal percentageDiscount = orderAmount
                    .multiply(discountValue)
                    .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
                
                // Apply max discount limit if set
                if (maxDiscountAmount != null && percentageDiscount.compareTo(maxDiscountAmount) > 0) {
                    yield maxDiscountAmount;
                }
                yield percentageDiscount;
            }
            case FIXED_AMOUNT -> {
                // Don't give more discount than the order amount
                yield orderAmount.min(discountValue);
            }
        };
        
        return discount;
    }
}
