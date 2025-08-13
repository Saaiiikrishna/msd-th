package com.mysillydreams.treasure.domain.repository;

import com.mysillydreams.treasure.domain.model.Difficulty;
import com.mysillydreams.treasure.domain.model.EnrollmentType;
import com.mysillydreams.treasure.domain.model.Promotion;
import com.mysillydreams.treasure.domain.model.PromotionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface PromotionRepository extends JpaRepository<Promotion, UUID> {
    
    /**
     * Find active promotions
     */
    @Query("SELECT p FROM Promotion p WHERE p.isActive = true " +
           "AND p.startDate <= :now AND p.endDate > :now " +
           "ORDER BY p.priority DESC")
    List<Promotion> findActivePromotions(@Param("now") OffsetDateTime now);
    
    /**
     * Find applicable promotions for specific criteria
     */
    @Query("SELECT p FROM Promotion p WHERE p.isActive = true " +
           "AND p.startDate <= :now AND p.endDate > :now " +
           "AND (p.applicableEnrollmentType IS NULL OR p.applicableEnrollmentType = :enrollmentType) " +
           "AND (p.applicableDifficulty IS NULL OR p.applicableDifficulty = :difficulty) " +
           "ORDER BY p.priority DESC")
    List<Promotion> findApplicablePromotions(@Param("now") OffsetDateTime now,
                                           @Param("enrollmentType") EnrollmentType enrollmentType,
                                           @Param("difficulty") Difficulty difficulty);
    
    /**
     * Find promotions by type
     */
    List<Promotion> findByPromotionTypeAndIsActiveTrueOrderByPriorityDesc(PromotionType promotionType);
    
    /**
     * Find promotions created by a specific user
     */
    List<Promotion> findByCreatedByOrderByCreatedAtDesc(UUID createdBy);
    
    /**
     * Find promotions ending soon
     */
    @Query("SELECT p FROM Promotion p WHERE p.isActive = true " +
           "AND p.endDate BETWEEN :now AND :endThreshold")
    List<Promotion> findEndingSoonPromotions(@Param("now") OffsetDateTime now,
                                           @Param("endThreshold") OffsetDateTime endThreshold);
    
    /**
     * Find promotions that can stack with promo codes
     */
    @Query("SELECT p FROM Promotion p WHERE p.isActive = true " +
           "AND p.startDate <= :now AND p.endDate > :now " +
           "AND p.canStackWithPromoCodes = true " +
           "ORDER BY p.priority DESC")
    List<Promotion> findStackablePromotions(@Param("now") OffsetDateTime now);
}
