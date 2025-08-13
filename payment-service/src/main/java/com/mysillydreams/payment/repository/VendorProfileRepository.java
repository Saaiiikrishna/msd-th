package com.mysillydreams.payment.repository;

import com.mysillydreams.payment.domain.VendorProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VendorProfileRepository extends JpaRepository<VendorProfile, UUID> {
    
    /**
     * Find vendor profile by vendor ID
     */
    Optional<VendorProfile> findByVendorId(UUID vendorId);
    
    /**
     * Find vendor profile by Razorpay fund account ID
     */
    Optional<VendorProfile> findByRazorpayFundAccountId(String razorpayFundAccountId);
    
    /**
     * Find active vendors
     */
    List<VendorProfile> findByIsActiveTrueOrderByCreatedAtDesc();
    
    /**
     * Find verified vendors
     */
    List<VendorProfile> findByIsVerifiedTrueOrderByCreatedAtDesc();
    
    /**
     * Find vendors with commission rate in range
     */
    @Query("SELECT vp FROM VendorProfile vp WHERE vp.isActive = true " +
           "AND vp.commissionRate BETWEEN :minRate AND :maxRate " +
           "ORDER BY vp.commissionRate ASC")
    List<VendorProfile> findByCommissionRateRange(@Param("minRate") BigDecimal minRate,
                                                 @Param("maxRate") BigDecimal maxRate);
    
    /**
     * Find vendors with recent payouts
     */
    @Query("SELECT vp FROM VendorProfile vp WHERE vp.isActive = true " +
           "AND vp.lastPayoutDate >= :cutoffDate " +
           "ORDER BY vp.lastPayoutDate DESC")
    List<VendorProfile> findVendorsWithRecentPayouts(@Param("cutoffDate") Instant cutoffDate);
    
    /**
     * Find top vendors by total payout amount
     */
    @Query("SELECT vp FROM VendorProfile vp WHERE vp.isActive = true " +
           "ORDER BY vp.totalPayoutsAmount DESC")
    List<VendorProfile> findTopVendorsByPayoutAmount();
    
    /**
     * Calculate total commission earned across all vendors
     */
    @Query("SELECT COALESCE(SUM(vp.totalCommissionEarned), 0) FROM VendorProfile vp WHERE vp.isActive = true")
    BigDecimal calculateTotalCommissionEarned();
    
    /**
     * Count active vendors
     */
    Long countByIsActiveTrue();
    
    /**
     * Count verified vendors
     */
    Long countByIsVerifiedTrue();
    
    /**
     * Find vendors needing verification
     */
    @Query("SELECT vp FROM VendorProfile vp WHERE vp.isActive = true " +
           "AND vp.isVerified = false " +
           "ORDER BY vp.createdAt ASC")
    List<VendorProfile> findVendorsNeedingVerification();
}
