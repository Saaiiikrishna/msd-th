package com.mysillydreams.treasure.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Promotional campaigns and sales for treasure hunt plans
 */
@Entity 
@Table(name = "promotion",
       indexes = {
           @Index(name = "idx_promotion_active", columnList = "is_active, start_date, end_date"),
           @Index(name = "idx_promotion_type", columnList = "promotion_type")
       })
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Promotion {
    
    @Id @GeneratedValue 
    private UUID id;
    
    @Column(nullable = false, length = 100)
    private String name;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "promotion_type", nullable = false)
    private PromotionType promotionType;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false)
    private DiscountType discountType;
    
    @Column(name = "discount_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountValue;
    
    @Column(name = "max_discount_amount", precision = 10, scale = 2)
    private BigDecimal maxDiscountAmount;
    
    @Column(name = "min_order_amount", precision = 10, scale = 2)
    private BigDecimal minOrderAmount;
    
    @Column(name = "start_date", nullable = false)
    private OffsetDateTime startDate;
    
    @Column(name = "end_date", nullable = false)
    private OffsetDateTime endDate;
    
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
    
    // Applicability filters
    @Enumerated(EnumType.STRING)
    @Column(name = "applicable_enrollment_type")
    private EnrollmentType applicableEnrollmentType;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "applicable_difficulty")
    private Difficulty applicableDifficulty;
    
    @Column(name = "applicable_plan_ids", columnDefinition = "TEXT")
    private String applicablePlanIds; // JSON array of plan UUIDs
    
    @Column(name = "first_time_users_only", nullable = false)
    @Builder.Default
    private Boolean firstTimeUsersOnly = false;
    
    // Priority for stacking promotions
    @Column(name = "priority", nullable = false)
    @Builder.Default
    private Integer priority = 0; // Higher number = higher priority
    
    @Column(name = "can_stack_with_promo_codes", nullable = false)
    @Builder.Default
    private Boolean canStackWithPromoCodes = true;
    
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
     * Check if promotion is currently active
     */
    public boolean isCurrentlyActive() {
        OffsetDateTime now = OffsetDateTime.now();
        return isActive && 
               now.isAfter(startDate) && 
               now.isBefore(endDate);
    }
    
    /**
     * Calculate discount amount for a given order value
     */
    public BigDecimal calculateDiscount(BigDecimal orderAmount) {
        if (!isCurrentlyActive() || 
            (minOrderAmount != null && orderAmount.compareTo(minOrderAmount) < 0)) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal discount = switch (discountType) {
            case PERCENTAGE -> {
                BigDecimal percentageDiscount = orderAmount
                    .multiply(discountValue)
                    .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
                
                if (maxDiscountAmount != null && percentageDiscount.compareTo(maxDiscountAmount) > 0) {
                    yield maxDiscountAmount;
                }
                yield percentageDiscount;
            }
            case FIXED_AMOUNT -> orderAmount.min(discountValue);
        };
        
        return discount;
    }
}
