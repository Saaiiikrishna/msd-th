package com.mysillydreams.payment.repository;

import com.mysillydreams.payment.domain.PaymentLink;
import com.mysillydreams.payment.domain.PaymentLinkStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Payment Link entities
 */
@Repository
public interface PaymentLinkRepository extends JpaRepository<PaymentLink, UUID> {

    /**
     * Find payment link by Razorpay payment link ID
     */
    Optional<PaymentLink> findByRazorpayPaymentLinkId(String razorpayPaymentLinkId);

    /**
     * Find payment links by invoice ID
     */
    List<PaymentLink> findByInvoiceIdOrderByCreatedAtDesc(UUID invoiceId);

    /**
     * Find active payment links (not expired, not cancelled, not paid)
     */
    @Query("SELECT pl FROM PaymentLink pl WHERE pl.status = :status AND " +
           "(pl.expiresAt IS NULL OR pl.expiresAt > :now)")
    List<PaymentLink> findActivePaymentLinks(@Param("status") PaymentLinkStatus status, 
                                           @Param("now") LocalDateTime now);

    /**
     * Find expired payment links that need status update
     */
    @Query("SELECT pl FROM PaymentLink pl WHERE pl.status = 'CREATED' AND " +
           "pl.expiresAt IS NOT NULL AND pl.expiresAt < :now")
    List<PaymentLink> findExpiredPaymentLinks(@Param("now") LocalDateTime now);

    /**
     * Count payment links by status
     */
    long countByStatus(PaymentLinkStatus status);

    /**
     * Find payment links created within date range
     */
    @Query("SELECT pl FROM PaymentLink pl WHERE pl.createdAt BETWEEN :startDate AND :endDate " +
           "ORDER BY pl.createdAt DESC")
    List<PaymentLink> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate,
                                           @Param("endDate") LocalDateTime endDate);
}
