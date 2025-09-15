package com.mysillydreams.treasure.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Tracks promo code usage by users
 */
@Entity 
@Table(name = "promo_code_usage",
       indexes = {
           @Index(name = "idx_promo_usage_user", columnList = "user_id"),
           @Index(name = "idx_promo_usage_code", columnList = "promo_code_id"),
           @Index(name = "idx_promo_usage_enrollment", columnList = "enrollment_id")
       })
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class PromoCodeUsage {
    
    @Id @GeneratedValue 
    private UUID id;
    
    @ManyToOne(optional = false)
    @JoinColumn(name = "promo_code_id")
    private PromoCode promoCode;
    
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    
    @Column(name = "enrollment_id", nullable = false)
    private UUID enrollmentId;
    
    @Column(name = "order_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal orderAmount;
    
    @Column(name = "discount_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountAmount;
    
    @Column(name = "final_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal finalAmount;
    
    @Column(nullable = false)
    @Builder.Default
    private OffsetDateTime usedAt = OffsetDateTime.now();
}
