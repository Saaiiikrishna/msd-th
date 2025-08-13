package com.mysillydreams.treasure.domain.service;

import com.mysillydreams.treasure.domain.model.*;
import com.mysillydreams.treasure.domain.repository.PromoCodeRepository;
import com.mysillydreams.treasure.domain.repository.PromoCodeUsageRepository;
import com.mysillydreams.treasure.domain.repository.PromotionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for handling discounts, promo codes, and promotions
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DiscountService {
    
    private final PromoCodeRepository promoCodeRepository;
    private final PromoCodeUsageRepository promoCodeUsageRepository;
    private final PromotionRepository promotionRepository;
    
    /**
     * Calculate total discount for an enrollment
     */
    @Transactional(readOnly = true)
    public DiscountCalculation calculateDiscount(DiscountRequest request) {
        BigDecimal originalAmount = request.originalAmount();
        BigDecimal totalDiscount = BigDecimal.ZERO;
        
        DiscountCalculation.Builder calculationBuilder = DiscountCalculation.builder()
                .originalAmount(originalAmount)
                .userId(request.userId())
                .planId(request.planId())
                .enrollmentType(request.enrollmentType())
                .difficulty(request.difficulty());
        
        // Apply automatic promotions first
        List<Promotion> applicablePromotions = promotionRepository.findApplicablePromotions(
                OffsetDateTime.now(), request.enrollmentType(), request.difficulty());
        
        BigDecimal promotionDiscount = BigDecimal.ZERO;
        for (Promotion promotion : applicablePromotions) {
            if (isPromotionApplicable(promotion, request)) {
                BigDecimal discount = promotion.calculateDiscount(originalAmount.subtract(totalDiscount));
                promotionDiscount = promotionDiscount.add(discount);
                totalDiscount = totalDiscount.add(discount);
                
                calculationBuilder.appliedPromotion(promotion);
                log.info("Applied promotion {} with discount {}", promotion.getName(), discount);
            }
        }
        
        // Apply promo code if provided
        BigDecimal promoCodeDiscount = BigDecimal.ZERO;
        if (request.promoCode() != null && !request.promoCode().trim().isEmpty()) {
            Optional<PromoCode> promoCodeOpt = validateAndGetPromoCode(request.promoCode(), request.userId());
            
            if (promoCodeOpt.isPresent()) {
                PromoCode promoCode = promoCodeOpt.get();
                
                // Check if promo code can stack with applied promotions
                boolean canStack = applicablePromotions.isEmpty() || 
                                 applicablePromotions.stream().allMatch(Promotion::getCanStackWithPromoCodes);
                
                if (canStack) {
                    BigDecimal remainingAmount = originalAmount.subtract(promotionDiscount);
                    promoCodeDiscount = promoCode.calculateDiscount(remainingAmount);
                    totalDiscount = totalDiscount.add(promoCodeDiscount);
                    
                    calculationBuilder.appliedPromoCode(promoCode);
                    log.info("Applied promo code {} with discount {}", promoCode.getCode(), promoCodeDiscount);
                } else {
                    calculationBuilder.promoCodeError("Promo code cannot be combined with current promotions");
                }
            } else {
                calculationBuilder.promoCodeError("Invalid or expired promo code");
            }
        }
        
        BigDecimal finalAmount = originalAmount.subtract(totalDiscount);
        
        return calculationBuilder
                .promotionDiscount(promotionDiscount)
                .promoCodeDiscount(promoCodeDiscount)
                .totalDiscount(totalDiscount)
                .finalAmount(finalAmount)
                .build();
    }
    
    /**
     * Apply discount and record usage
     */
    @Transactional
    public DiscountApplication applyDiscount(DiscountCalculation calculation, UUID enrollmentId) {
        BigDecimal totalSavings = BigDecimal.ZERO;
        
        // Record promo code usage if applied
        if (calculation.appliedPromoCode() != null) {
            PromoCodeUsage usage = PromoCodeUsage.builder()
                    .promoCode(calculation.appliedPromoCode())
                    .userId(calculation.userId())
                    .enrollmentId(enrollmentId)
                    .orderAmount(calculation.originalAmount())
                    .discountAmount(calculation.promoCodeDiscount())
                    .finalAmount(calculation.finalAmount())
                    .build();
            
            promoCodeUsageRepository.save(usage);
            
            // Increment promo code usage count
            promoCodeRepository.incrementUsageCount(calculation.appliedPromoCode().getId());
            
            totalSavings = totalSavings.add(calculation.promoCodeDiscount());
            
            log.info("Recorded promo code usage for user {} with discount {}", 
                    calculation.userId(), calculation.promoCodeDiscount());
        }
        
        totalSavings = totalSavings.add(calculation.promotionDiscount());
        
        return DiscountApplication.builder()
                .enrollmentId(enrollmentId)
                .userId(calculation.userId())
                .originalAmount(calculation.originalAmount())
                .totalDiscount(calculation.totalDiscount())
                .finalAmount(calculation.finalAmount())
                .totalSavings(totalSavings)
                .appliedAt(OffsetDateTime.now())
                .build();
    }
    
    /**
     * Validate promo code and check user eligibility
     */
    @Transactional(readOnly = true)
    public Optional<PromoCode> validateAndGetPromoCode(String code, UUID userId) {
        Optional<PromoCode> promoCodeOpt = promoCodeRepository.findByCodeIgnoreCase(code);
        
        if (promoCodeOpt.isEmpty()) {
            return Optional.empty();
        }
        
        PromoCode promoCode = promoCodeOpt.get();
        
        // Check if promo code is currently valid
        if (!promoCode.isCurrentlyValid()) {
            return Optional.empty();
        }
        
        // Check user-specific usage limits
        Long userUsageCount = promoCodeUsageRepository.countByUserIdAndPromoCodeId(userId, promoCode.getId());
        if (!promoCode.canBeUsedBy(userId, userUsageCount.intValue())) {
            return Optional.empty();
        }
        
        // Check first-time user restriction
        if (promoCode.getFirstTimeUsersOnly()) {
            Boolean hasUsedBefore = promoCodeUsageRepository.hasUserUsedAnyPromoCode(userId);
            if (hasUsedBefore) {
                return Optional.empty();
            }
        }
        
        return Optional.of(promoCode);
    }
    
    /**
     * Get available promo codes for a user
     */
    @Transactional(readOnly = true)
    public List<PromoCode> getAvailablePromoCodes(UUID userId, EnrollmentType enrollmentType, Difficulty difficulty) {
        List<PromoCode> applicableCodes = promoCodeRepository.findApplicablePromoCodes(
                OffsetDateTime.now(), enrollmentType, difficulty);
        
        return applicableCodes.stream()
                .filter(promoCode -> {
                    Long userUsageCount = promoCodeUsageRepository.countByUserIdAndPromoCodeId(userId, promoCode.getId());
                    return promoCode.canBeUsedBy(userId, userUsageCount.intValue());
                })
                .filter(promoCode -> {
                    if (promoCode.getFirstTimeUsersOnly()) {
                        Boolean hasUsedBefore = promoCodeUsageRepository.hasUserUsedAnyPromoCode(userId);
                        return !hasUsedBefore;
                    }
                    return true;
                })
                .toList();
    }
    
    /**
     * Get active promotions
     */
    @Transactional(readOnly = true)
    public List<Promotion> getActivePromotions(EnrollmentType enrollmentType, Difficulty difficulty) {
        return promotionRepository.findApplicablePromotions(OffsetDateTime.now(), enrollmentType, difficulty);
    }
    
    /**
     * Check if promotion is applicable to the request
     */
    private boolean isPromotionApplicable(Promotion promotion, DiscountRequest request) {
        // Check first-time user restriction
        if (promotion.getFirstTimeUsersOnly()) {
            Boolean hasUsedBefore = promoCodeUsageRepository.hasUserUsedAnyPromoCode(request.userId());
            if (hasUsedBefore) {
                return false;
            }
        }
        
        // Additional plan-specific checks could be added here
        // For now, the repository query handles most filtering
        
        return true;
    }
    
    /**
     * Request object for discount calculation
     */
    public record DiscountRequest(
            UUID userId,
            UUID planId,
            BigDecimal originalAmount,
            EnrollmentType enrollmentType,
            Difficulty difficulty,
            String promoCode
    ) {}
    
    /**
     * Result of discount calculation
     */
    public record DiscountCalculation(
            UUID userId,
            UUID planId,
            BigDecimal originalAmount,
            BigDecimal promotionDiscount,
            BigDecimal promoCodeDiscount,
            BigDecimal totalDiscount,
            BigDecimal finalAmount,
            EnrollmentType enrollmentType,
            Difficulty difficulty,
            Promotion appliedPromotion,
            PromoCode appliedPromoCode,
            String promoCodeError
    ) {
        public static Builder builder() {
            return new Builder();
        }
        
        public static class Builder {
            private UUID userId;
            private UUID planId;
            private BigDecimal originalAmount;
            private BigDecimal promotionDiscount = BigDecimal.ZERO;
            private BigDecimal promoCodeDiscount = BigDecimal.ZERO;
            private BigDecimal totalDiscount = BigDecimal.ZERO;
            private BigDecimal finalAmount;
            private EnrollmentType enrollmentType;
            private Difficulty difficulty;
            private Promotion appliedPromotion;
            private PromoCode appliedPromoCode;
            private String promoCodeError;
            
            public Builder userId(UUID userId) { this.userId = userId; return this; }
            public Builder planId(UUID planId) { this.planId = planId; return this; }
            public Builder originalAmount(BigDecimal originalAmount) { this.originalAmount = originalAmount; return this; }
            public Builder promotionDiscount(BigDecimal promotionDiscount) { this.promotionDiscount = promotionDiscount; return this; }
            public Builder promoCodeDiscount(BigDecimal promoCodeDiscount) { this.promoCodeDiscount = promoCodeDiscount; return this; }
            public Builder totalDiscount(BigDecimal totalDiscount) { this.totalDiscount = totalDiscount; return this; }
            public Builder finalAmount(BigDecimal finalAmount) { this.finalAmount = finalAmount; return this; }
            public Builder enrollmentType(EnrollmentType enrollmentType) { this.enrollmentType = enrollmentType; return this; }
            public Builder difficulty(Difficulty difficulty) { this.difficulty = difficulty; return this; }
            public Builder appliedPromotion(Promotion appliedPromotion) { this.appliedPromotion = appliedPromotion; return this; }
            public Builder appliedPromoCode(PromoCode appliedPromoCode) { this.appliedPromoCode = appliedPromoCode; return this; }
            public Builder promoCodeError(String promoCodeError) { this.promoCodeError = promoCodeError; return this; }
            
            public DiscountCalculation build() {
                return new DiscountCalculation(userId, planId, originalAmount, promotionDiscount, 
                    promoCodeDiscount, totalDiscount, finalAmount, enrollmentType, difficulty, 
                    appliedPromotion, appliedPromoCode, promoCodeError);
            }
        }
    }
    
    /**
     * Result of applying discount
     */
    public record DiscountApplication(
            UUID enrollmentId,
            UUID userId,
            BigDecimal originalAmount,
            BigDecimal totalDiscount,
            BigDecimal finalAmount,
            BigDecimal totalSavings,
            OffsetDateTime appliedAt
    ) {
        public static Builder builder() {
            return new Builder();
        }
        
        public static class Builder {
            private UUID enrollmentId;
            private UUID userId;
            private BigDecimal originalAmount;
            private BigDecimal totalDiscount;
            private BigDecimal finalAmount;
            private BigDecimal totalSavings;
            private OffsetDateTime appliedAt;
            
            public Builder enrollmentId(UUID enrollmentId) { this.enrollmentId = enrollmentId; return this; }
            public Builder userId(UUID userId) { this.userId = userId; return this; }
            public Builder originalAmount(BigDecimal originalAmount) { this.originalAmount = originalAmount; return this; }
            public Builder totalDiscount(BigDecimal totalDiscount) { this.totalDiscount = totalDiscount; return this; }
            public Builder finalAmount(BigDecimal finalAmount) { this.finalAmount = finalAmount; return this; }
            public Builder totalSavings(BigDecimal totalSavings) { this.totalSavings = totalSavings; return this; }
            public Builder appliedAt(OffsetDateTime appliedAt) { this.appliedAt = appliedAt; return this; }
            
            public DiscountApplication build() {
                return new DiscountApplication(enrollmentId, userId, originalAmount, 
                    totalDiscount, finalAmount, totalSavings, appliedAt);
            }
        }
    }
}
