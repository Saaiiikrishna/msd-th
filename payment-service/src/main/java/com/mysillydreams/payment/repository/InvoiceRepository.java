package com.mysillydreams.payment.repository;

import com.mysillydreams.payment.domain.Invoice;
import com.mysillydreams.payment.domain.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {
    
    /**
     * Find invoice by enrollment ID
     */
    Optional<Invoice> findByEnrollmentId(UUID enrollmentId);
    
    /**
     * Find invoice by registration ID
     */
    Optional<Invoice> findByRegistrationId(String registrationId);
    
    /**
     * Find invoices by user ID
     */
    List<Invoice> findByUserIdOrderByCreatedAtDesc(UUID userId);
    
    /**
     * Find invoice by Razorpay order ID
     */
    Optional<Invoice> findByRazorpayOrderId(String razorpayOrderId);
    
    /**
     * Find invoice by Razorpay payment ID
     */
    Optional<Invoice> findByRazorpayPaymentId(String razorpayPaymentId);
    
    /**
     * Count invoices by payment status
     */
    Long countByPaymentStatus(PaymentStatus paymentStatus);
    
    /**
     * Find invoices by payment status
     */
    List<Invoice> findByPaymentStatusOrderByCreatedAtDesc(PaymentStatus paymentStatus);
    
    /**
     * Calculate total revenue for paid invoices
     */
    @Query("SELECT COALESCE(SUM(i.totalAmount), 0) FROM Invoice i WHERE i.paymentStatus = 'PAID' " +
           "AND (:startDate IS NULL OR i.paidAt >= :startDate) " +
           "AND (:endDate IS NULL OR i.paidAt <= :endDate)")
    BigDecimal calculateTotalRevenue(@Param("startDate") Instant startDate, 
                                   @Param("endDate") Instant endDate);
    

    
    /**
     * Find invoices created within date range
     */
    @Query("SELECT i FROM Invoice i WHERE i.createdAt BETWEEN :startDate AND :endDate " +
           "ORDER BY i.createdAt DESC")
    List<Invoice> findByCreatedAtBetween(@Param("startDate") Instant startDate,
                                       @Param("endDate") Instant endDate);
    
    /**
     * Find invoices by plan ID
     */
    List<Invoice> findByPlanIdOrderByCreatedAtDesc(UUID planId);
    
    /**
     * Calculate total discount given
     */
    @Query("SELECT COALESCE(SUM(i.discountAmount), 0) FROM Invoice i WHERE i.paymentStatus = 'PAID' " +
           "AND (:startDate IS NULL OR i.paidAt >= :startDate) " +
           "AND (:endDate IS NULL OR i.paidAt <= :endDate)")
    BigDecimal calculateTotalDiscounts(@Param("startDate") Instant startDate,
                                     @Param("endDate") Instant endDate);
    
    /**
     * Find invoices with promo codes
     */
    @Query("SELECT i FROM Invoice i WHERE i.promoCode IS NOT NULL " +
           "ORDER BY i.createdAt DESC")
    List<Invoice> findInvoicesWithPromoCodes();
    
    /**
     * Calculate average order value
     */
    @Query("SELECT AVG(i.totalAmount) FROM Invoice i WHERE i.paymentStatus = 'PAID' " +
           "AND (:startDate IS NULL OR i.paidAt >= :startDate) " +
           "AND (:endDate IS NULL OR i.paidAt <= :endDate)")
    BigDecimal calculateAverageOrderValue(@Param("startDate") Instant startDate,
                                        @Param("endDate") Instant endDate);
}
