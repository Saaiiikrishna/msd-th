package com.mysillydreams.payment.repository;

import com.mysillydreams.payment.domain.PaymentMethodType;
import com.mysillydreams.payment.domain.SavedPaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SavedPaymentMethodRepository extends JpaRepository<SavedPaymentMethod, UUID> {
    
    /**
     * Find active payment methods for a user
     */
    List<SavedPaymentMethod> findByUserIdAndIsActiveTrueOrderByIsDefaultDescCreatedAtDesc(UUID userId);
    
    /**
     * Find user's default payment method
     */
    Optional<SavedPaymentMethod> findByUserIdAndIsDefaultTrueAndIsActiveTrue(UUID userId);
    
    /**
     * Find payment method by ID and user ID
     */
    Optional<SavedPaymentMethod> findByIdAndUserId(UUID id, UUID userId);
    
    /**
     * Find first payment method with Razorpay customer ID
     */
    Optional<SavedPaymentMethod> findFirstByUserIdAndRazorpayCustomerIdIsNotNull(UUID userId);
    
    /**
     * Find by Razorpay token
     */
    Optional<SavedPaymentMethod> findByRazorpayToken(String razorpayToken);
    
    /**
     * Remove default status from all user's payment methods
     */
    @Modifying
    @Query("UPDATE SavedPaymentMethod spm SET spm.isDefault = false WHERE spm.userId = :userId")
    void removeDefaultForUser(@Param("userId") UUID userId);
    
    /**
     * Find payment methods by type
     */
    List<SavedPaymentMethod> findByUserIdAndPaymentTypeAndIsActiveTrueOrderByCreatedAtDesc(
            UUID userId, PaymentMethodType paymentType);
    
    /**
     * Find expired card payment methods
     */
    @Query("SELECT spm FROM SavedPaymentMethod spm WHERE spm.paymentType = 'CARD' " +
           "AND spm.isActive = true " +
           "AND (spm.cardExpiryYear < :currentYear OR " +
           "(spm.cardExpiryYear = :currentYear AND spm.cardExpiryMonth < :currentMonth))")
    List<SavedPaymentMethod> findExpiredCards(@Param("currentYear") Integer currentYear,
                                            @Param("currentMonth") Integer currentMonth);
    
    /**
     * Find unused payment methods
     */
    @Query("SELECT spm FROM SavedPaymentMethod spm WHERE spm.isActive = true " +
           "AND (spm.lastUsedAt IS NULL OR spm.lastUsedAt < :cutoffDate)")
    List<SavedPaymentMethod> findUnusedPaymentMethods(@Param("cutoffDate") Instant cutoffDate);
    
    /**
     * Count active payment methods for user
     */
    Long countByUserIdAndIsActiveTrue(UUID userId);
    
    /**
     * Find most used payment methods
     */
    @Query("SELECT spm FROM SavedPaymentMethod spm WHERE spm.isActive = true " +
           "AND spm.lastUsedAt IS NOT NULL ORDER BY spm.lastUsedAt DESC")
    List<SavedPaymentMethod> findMostRecentlyUsed();
}
