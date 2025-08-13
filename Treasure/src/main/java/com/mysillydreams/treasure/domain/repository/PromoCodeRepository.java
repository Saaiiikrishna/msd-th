package com.mysillydreams.treasure.domain.repository;

import com.mysillydreams.treasure.domain.model.Difficulty;
import com.mysillydreams.treasure.domain.model.EnrollmentType;
import com.mysillydreams.treasure.domain.model.PromoCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PromoCodeRepository extends JpaRepository<PromoCode, UUID> {
    
    /**
     * Find promo code by code string
     */
    Optional<PromoCode> findByCodeIgnoreCase(String code);
    
    /**
     * Find active promo codes
     */
    @Query("SELECT pc FROM PromoCode pc WHERE pc.isActive = true " +
           "AND pc.validFrom <= :now AND pc.validUntil > :now " +
           "AND (pc.usageLimit IS NULL OR pc.usageCount < pc.usageLimit)")
    List<PromoCode> findActivePromoCodes(@Param("now") OffsetDateTime now);
    
    /**
     * Find applicable promo codes for specific criteria
     */
    @Query("SELECT pc FROM PromoCode pc WHERE pc.isActive = true " +
           "AND pc.validFrom <= :now AND pc.validUntil > :now " +
           "AND (pc.usageLimit IS NULL OR pc.usageCount < pc.usageLimit) " +
           "AND (pc.applicableEnrollmentType IS NULL OR pc.applicableEnrollmentType = :enrollmentType) " +
           "AND (pc.applicableDifficulty IS NULL OR pc.applicableDifficulty = :difficulty)")
    List<PromoCode> findApplicablePromoCodes(@Param("now") OffsetDateTime now,
                                           @Param("enrollmentType") EnrollmentType enrollmentType,
                                           @Param("difficulty") Difficulty difficulty);
    
    /**
     * Increment usage count atomically
     */
    @Modifying
    @Query("UPDATE PromoCode pc SET pc.usageCount = pc.usageCount + 1, " +
           "pc.updatedAt = CURRENT_TIMESTAMP WHERE pc.id = :id")
    int incrementUsageCount(@Param("id") UUID id);
    
    /**
     * Find promo codes created by a specific user
     */
    List<PromoCode> findByCreatedByOrderByCreatedAtDesc(UUID createdBy);
    
    /**
     * Find promo codes expiring soon
     */
    @Query("SELECT pc FROM PromoCode pc WHERE pc.isActive = true " +
           "AND pc.validUntil BETWEEN :now AND :expiryThreshold")
    List<PromoCode> findExpiringPromoCodes(@Param("now") OffsetDateTime now,
                                         @Param("expiryThreshold") OffsetDateTime expiryThreshold);
    
    /**
     * Find most used promo codes
     */
    @Query("SELECT pc FROM PromoCode pc WHERE pc.usageCount > 0 " +
           "ORDER BY pc.usageCount DESC")
    List<PromoCode> findMostUsedPromoCodes();
}
