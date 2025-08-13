package com.mysillydreams.treasure.domain.service;

import com.mysillydreams.treasure.domain.model.*;
import com.mysillydreams.treasure.domain.repository.PromoCodeRepository;
import com.mysillydreams.treasure.domain.repository.PromoCodeUsageRepository;
import com.mysillydreams.treasure.domain.repository.PromotionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiscountServiceTest {

    @Mock
    private PromoCodeRepository promoCodeRepository;

    @Mock
    private PromoCodeUsageRepository promoCodeUsageRepository;

    @Mock
    private PromotionRepository promotionRepository;

    @InjectMocks
    private DiscountService discountService;

    private UUID userId;
    private UUID planId;
    private PromoCode validPromoCode;
    private Promotion activePromotion;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        planId = UUID.randomUUID();

        validPromoCode = PromoCode.builder()
                .id(UUID.randomUUID())
                .code("SAVE20")
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(BigDecimal.valueOf(20))
                .validFrom(OffsetDateTime.now().minusDays(1))
                .validUntil(OffsetDateTime.now().plusDays(30))
                .isActive(true)
                .usageLimit(100)
                .usageCount(10)
                .build();

        activePromotion = Promotion.builder()
                .id(UUID.randomUUID())
                .name("Summer Sale")
                .discountType(DiscountType.FIXED_AMOUNT)
                .discountValue(BigDecimal.valueOf(100))
                .startDate(OffsetDateTime.now().minusDays(1))
                .endDate(OffsetDateTime.now().plusDays(30))
                .isActive(true)
                .canStackWithPromoCodes(true)
                .priority(1)
                .build();
    }

    @Test
    void calculateDiscount_ShouldApplyPromotionOnly() {
        // Given
        DiscountService.DiscountRequest request = new DiscountService.DiscountRequest(
                userId, planId, BigDecimal.valueOf(1000), 
                EnrollmentType.INDIVIDUAL, Difficulty.BEGINNER, null);

        when(promotionRepository.findApplicablePromotions(any(), eq(EnrollmentType.INDIVIDUAL), eq(Difficulty.BEGINNER)))
                .thenReturn(Arrays.asList(activePromotion));
        when(promoCodeUsageRepository.hasUserUsedAnyPromoCode(userId))
                .thenReturn(false);

        // When
        DiscountService.DiscountCalculation result = discountService.calculateDiscount(request);

        // Then
        assertThat(result.originalAmount()).isEqualTo(BigDecimal.valueOf(1000));
        assertThat(result.promotionDiscount()).isEqualTo(BigDecimal.valueOf(100));
        assertThat(result.promoCodeDiscount()).isEqualTo(BigDecimal.ZERO);
        assertThat(result.totalDiscount()).isEqualTo(BigDecimal.valueOf(100));
        assertThat(result.finalAmount()).isEqualTo(BigDecimal.valueOf(900));
        assertThat(result.appliedPromotion()).isEqualTo(activePromotion);
    }

    @Test
    void calculateDiscount_ShouldApplyPromoCodeOnly() {
        // Given
        DiscountService.DiscountRequest request = new DiscountService.DiscountRequest(
                userId, planId, BigDecimal.valueOf(1000), 
                EnrollmentType.INDIVIDUAL, Difficulty.BEGINNER, "SAVE20");

        when(promotionRepository.findApplicablePromotions(any(), eq(EnrollmentType.INDIVIDUAL), eq(Difficulty.BEGINNER)))
                .thenReturn(Arrays.asList());
        when(promoCodeRepository.findByCodeIgnoreCase("SAVE20"))
                .thenReturn(Optional.of(validPromoCode));
        when(promoCodeUsageRepository.countByUserIdAndPromoCodeId(userId, validPromoCode.getId()))
                .thenReturn(0L);
        when(promoCodeUsageRepository.hasUserUsedAnyPromoCode(userId))
                .thenReturn(false);

        // When
        DiscountService.DiscountCalculation result = discountService.calculateDiscount(request);

        // Then
        assertThat(result.originalAmount()).isEqualTo(BigDecimal.valueOf(1000));
        assertThat(result.promotionDiscount()).isEqualTo(BigDecimal.ZERO);
        assertThat(result.promoCodeDiscount()).isEqualTo(BigDecimal.valueOf(200)); // 20% of 1000
        assertThat(result.totalDiscount()).isEqualTo(BigDecimal.valueOf(200));
        assertThat(result.finalAmount()).isEqualTo(BigDecimal.valueOf(800));
        assertThat(result.appliedPromoCode()).isEqualTo(validPromoCode);
    }

    @Test
    void calculateDiscount_ShouldStackPromotionAndPromoCode() {
        // Given
        DiscountService.DiscountRequest request = new DiscountService.DiscountRequest(
                userId, planId, BigDecimal.valueOf(1000), 
                EnrollmentType.INDIVIDUAL, Difficulty.BEGINNER, "SAVE20");

        when(promotionRepository.findApplicablePromotions(any(), eq(EnrollmentType.INDIVIDUAL), eq(Difficulty.BEGINNER)))
                .thenReturn(Arrays.asList(activePromotion));
        when(promoCodeRepository.findByCodeIgnoreCase("SAVE20"))
                .thenReturn(Optional.of(validPromoCode));
        when(promoCodeUsageRepository.countByUserIdAndPromoCodeId(userId, validPromoCode.getId()))
                .thenReturn(0L);
        when(promoCodeUsageRepository.hasUserUsedAnyPromoCode(userId))
                .thenReturn(false);

        // When
        DiscountService.DiscountCalculation result = discountService.calculateDiscount(request);

        // Then
        assertThat(result.originalAmount()).isEqualTo(BigDecimal.valueOf(1000));
        assertThat(result.promotionDiscount()).isEqualTo(BigDecimal.valueOf(100));
        assertThat(result.promoCodeDiscount()).isEqualTo(BigDecimal.valueOf(180)); // 20% of 900
        assertThat(result.totalDiscount()).isEqualTo(BigDecimal.valueOf(280));
        assertThat(result.finalAmount()).isEqualTo(BigDecimal.valueOf(720));
    }

    @Test
    void calculateDiscount_ShouldRejectInvalidPromoCode() {
        // Given
        DiscountService.DiscountRequest request = new DiscountService.DiscountRequest(
                userId, planId, BigDecimal.valueOf(1000), 
                EnrollmentType.INDIVIDUAL, Difficulty.BEGINNER, "INVALID");

        when(promotionRepository.findApplicablePromotions(any(), eq(EnrollmentType.INDIVIDUAL), eq(Difficulty.BEGINNER)))
                .thenReturn(Arrays.asList());
        when(promoCodeRepository.findByCodeIgnoreCase("INVALID"))
                .thenReturn(Optional.empty());

        // When
        DiscountService.DiscountCalculation result = discountService.calculateDiscount(request);

        // Then
        assertThat(result.promoCodeDiscount()).isEqualTo(BigDecimal.ZERO);
        assertThat(result.promoCodeError()).isEqualTo("Invalid or expired promo code");
        assertThat(result.appliedPromoCode()).isNull();
    }

    @Test
    void calculateDiscount_ShouldRejectExpiredPromoCode() {
        // Given
        PromoCode expiredPromoCode = PromoCode.builder()
                .code("EXPIRED")
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(BigDecimal.valueOf(10))
                .validFrom(OffsetDateTime.now().minusDays(30))
                .validUntil(OffsetDateTime.now().minusDays(1))
                .isActive(true)
                .build();

        DiscountService.DiscountRequest request = new DiscountService.DiscountRequest(
                userId, planId, BigDecimal.valueOf(1000), 
                EnrollmentType.INDIVIDUAL, Difficulty.BEGINNER, "EXPIRED");

        when(promotionRepository.findApplicablePromotions(any(), eq(EnrollmentType.INDIVIDUAL), eq(Difficulty.BEGINNER)))
                .thenReturn(Arrays.asList());
        when(promoCodeRepository.findByCodeIgnoreCase("EXPIRED"))
                .thenReturn(Optional.of(expiredPromoCode));

        // When
        DiscountService.DiscountCalculation result = discountService.calculateDiscount(request);

        // Then
        assertThat(result.promoCodeDiscount()).isEqualTo(BigDecimal.ZERO);
        assertThat(result.promoCodeError()).isEqualTo("Invalid or expired promo code");
    }

    @Test
    void validateAndGetPromoCode_ShouldReturnValidCode() {
        // Given
        when(promoCodeRepository.findByCodeIgnoreCase("SAVE20"))
                .thenReturn(Optional.of(validPromoCode));
        when(promoCodeUsageRepository.countByUserIdAndPromoCodeId(userId, validPromoCode.getId()))
                .thenReturn(0L);
        when(promoCodeUsageRepository.hasUserUsedAnyPromoCode(userId))
                .thenReturn(false);

        // When
        Optional<PromoCode> result = discountService.validateAndGetPromoCode("SAVE20", userId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(validPromoCode);
    }

    @Test
    void validateAndGetPromoCode_ShouldRejectUsageLimitExceeded() {
        // Given
        PromoCode limitedPromoCode = PromoCode.builder()
                .code("LIMITED")
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(BigDecimal.valueOf(10))
                .validFrom(OffsetDateTime.now().minusDays(1))
                .validUntil(OffsetDateTime.now().plusDays(30))
                .isActive(true)
                .usageLimit(5)
                .usageCount(5) // Already at limit
                .build();

        when(promoCodeRepository.findByCodeIgnoreCase("LIMITED"))
                .thenReturn(Optional.of(limitedPromoCode));

        // When
        Optional<PromoCode> result = discountService.validateAndGetPromoCode("LIMITED", userId);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void applyDiscount_ShouldRecordPromoCodeUsage() {
        // Given
        UUID enrollmentId = UUID.randomUUID();
        DiscountService.DiscountCalculation calculation = DiscountService.DiscountCalculation.builder()
                .userId(userId)
                .originalAmount(BigDecimal.valueOf(1000))
                .promoCodeDiscount(BigDecimal.valueOf(200))
                .promotionDiscount(BigDecimal.ZERO)
                .totalDiscount(BigDecimal.valueOf(200))
                .finalAmount(BigDecimal.valueOf(800))
                .appliedPromoCode(validPromoCode)
                .build();

        when(promoCodeUsageRepository.save(any(PromoCodeUsage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(promoCodeRepository.incrementUsageCount(validPromoCode.getId()))
                .thenReturn(1);

        // When
        DiscountService.DiscountApplication result = discountService.applyDiscount(calculation, enrollmentId);

        // Then
        assertThat(result.enrollmentId()).isEqualTo(enrollmentId);
        assertThat(result.totalDiscount()).isEqualTo(BigDecimal.valueOf(200));
        assertThat(result.finalAmount()).isEqualTo(BigDecimal.valueOf(800));
        
        verify(promoCodeUsageRepository).save(argThat(usage -> 
                usage.getPromoCode().equals(validPromoCode) &&
                usage.getUserId().equals(userId) &&
                usage.getEnrollmentId().equals(enrollmentId) &&
                usage.getDiscountAmount().equals(BigDecimal.valueOf(200))
        ));
        verify(promoCodeRepository).incrementUsageCount(validPromoCode.getId());
    }

    @Test
    void getAvailablePromoCodes_ShouldFilterByUserEligibility() {
        // Given
        List<PromoCode> applicableCodes = Arrays.asList(validPromoCode);
        
        when(promoCodeRepository.findApplicablePromoCodes(
                any(OffsetDateTime.class), eq(EnrollmentType.INDIVIDUAL), eq(Difficulty.BEGINNER)))
                .thenReturn(applicableCodes);
        when(promoCodeUsageRepository.countByUserIdAndPromoCodeId(userId, validPromoCode.getId()))
                .thenReturn(0L);
        when(promoCodeUsageRepository.hasUserUsedAnyPromoCode(userId))
                .thenReturn(false);

        // When
        List<PromoCode> result = discountService.getAvailablePromoCodes(
                userId, EnrollmentType.INDIVIDUAL, Difficulty.BEGINNER);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(validPromoCode);
    }
}
