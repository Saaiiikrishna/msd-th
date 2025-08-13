package com.mysillydreams.treasure.domain.repository;

import com.mysillydreams.treasure.domain.model.PromoCodeUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface PromoCodeUsageRepository extends JpaRepository<PromoCodeUsage, UUID> {
    
    /**
     * Count usage by user and promo code
     */
    @Query("SELECT COUNT(pcu) FROM PromoCodeUsage pcu WHERE pcu.userId = :userId " +
           "AND pcu.promoCode.id = :promoCodeId")
    Long countByUserIdAndPromoCodeId(@Param("userId") UUID userId, 
                                    @Param("promoCodeId") UUID promoCodeId);
    
    /**
     * Find usage history for a user
     */
    List<PromoCodeUsage> findByUserIdOrderByUsedAtDesc(UUID userId);
    
    /**
     * Find usage history for a promo code
     */
    List<PromoCodeUsage> findByPromoCodeIdOrderByUsedAtDesc(UUID promoCodeId);
    
    /**
     * Find usage within a date range
     */
    @Query("SELECT pcu FROM PromoCodeUsage pcu WHERE pcu.usedAt BETWEEN :startDate AND :endDate " +
           "ORDER BY pcu.usedAt DESC")
    List<PromoCodeUsage> findByUsedAtBetween(@Param("startDate") OffsetDateTime startDate,
                                           @Param("endDate") OffsetDateTime endDate);
    
    /**
     * Calculate total discount given for a promo code
     */
    @Query("SELECT COALESCE(SUM(pcu.discountAmount), 0) FROM PromoCodeUsage pcu " +
           "WHERE pcu.promoCode.id = :promoCodeId")
    Double getTotalDiscountForPromoCode(@Param("promoCodeId") UUID promoCodeId);
    
    /**
     * Check if user has used any promo code before (for first-time user checks)
     */
    @Query("SELECT COUNT(pcu) > 0 FROM PromoCodeUsage pcu WHERE pcu.userId = :userId")
    Boolean hasUserUsedAnyPromoCode(@Param("userId") UUID userId);
}
