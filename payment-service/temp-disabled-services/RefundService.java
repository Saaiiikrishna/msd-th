package com.mysillydreams.payment.service;

import com.mysillydreams.payment.domain.PaymentTransaction;
import com.mysillydreams.payment.domain.Refund;
import com.mysillydreams.payment.domain.RefundStatus;
import com.mysillydreams.payment.repository.PaymentRepository;
import com.mysillydreams.payment.repository.RefundRepository;
import com.razorpay.RazorpayClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing Razorpay Refunds
 * Handles refund processing for treasure hunt cancellations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RefundService {

    private final RefundRepository refundRepository;
    private final PaymentRepository paymentRepository;
    private final RazorpayClient razorpayClient;
    private final OutboxEventService outboxEventService;

    /**
     * Create full refund for payment
     */
    @Transactional
    public Refund createFullRefund(UUID paymentId, String reason) {
        PaymentTransaction payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));

        return createRefund(payment, payment.getAmount(), reason);
    }

    /**
     * Create partial refund for payment
     */
    @Transactional
    public Refund createPartialRefund(UUID paymentId, BigDecimal refundAmount, String reason) {
        PaymentTransaction payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));

        if (refundAmount.compareTo(payment.getAmount()) > 0) {
            throw new IllegalArgumentException("Refund amount cannot exceed payment amount");
        }

        return createRefund(payment, refundAmount, reason);
    }

    /**
     * Create refund
     */
    private Refund createRefund(PaymentTransaction payment, BigDecimal refundAmount, String reason) {
        try {
            // Check if payment can be refunded
            if (payment.getRazorpayPaymentId() == null) {
                throw new IllegalStateException("Payment does not have Razorpay payment ID");
            }

            // Check existing refunds
            List<Refund> existingRefunds = refundRepository.findByPaymentIdAndStatusNot(
                    payment.getId(), RefundStatus.FAILED);
            BigDecimal totalRefunded = existingRefunds.stream()
                    .map(Refund::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (totalRefunded.add(refundAmount).compareTo(payment.getAmount()) > 0) {
                throw new IllegalArgumentException("Total refund amount would exceed payment amount");
            }

            // Create refund request
            JSONObject refundRequest = new JSONObject();
            refundRequest.put("amount", refundAmount.multiply(BigDecimal.valueOf(100)).intValue());
            refundRequest.put("speed", "normal"); // normal or optimum
            
            // Add notes
            JSONObject notes = new JSONObject();
            notes.put("reason", reason);
            notes.put("payment_id", payment.getId().toString());
            notes.put("enrollment_id", payment.getEnrollmentId().toString());
            refundRequest.put("notes", notes);

            // Create refund via Razorpay API
            com.razorpay.Payment razorpayPayment = razorpayClient.payments.fetch(payment.getRazorpayPaymentId());
            com.razorpay.Refund razorpayRefund = razorpayPayment.createRefund(refundRequest);

            // Save to database
            Refund refund = Refund.builder()
                    .id(UUID.randomUUID())
                    .paymentId(payment.getId())
                    .razorpayRefundId(razorpayRefund.get("id"))
                    .amount(refundAmount)
                    .currency(payment.getCurrency())
                    .reason(reason)
                    .status(RefundStatus.PENDING)
                    .build();

            Refund savedRefund = refundRepository.save(refund);

            // Publish event
            outboxEventService.publishEvent(
                    "Refund",
                    savedRefund.getId().toString(),
                    "refund.initiated",
                    savedRefund
            );

            log.info("Created refund {} for payment {} with amount {}", 
                    savedRefund.getId(), payment.getId(), refundAmount);

            return savedRefund;

        } catch (Exception e) {
            log.error("Error creating refund for payment {}: {}", payment.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to create refund: " + e.getMessage(), e);
        }
    }

    /**
     * Update refund status from webhook
     */
    @Transactional
    public void updateRefundStatus(String razorpayRefundId, RefundStatus status, String errorMessage) {
        Refund refund = refundRepository.findByRazorpayRefundId(razorpayRefundId)
                .orElseThrow(() -> new IllegalArgumentException("Refund not found: " + razorpayRefundId));

        RefundStatus oldStatus = refund.getStatus();
        refund.setStatus(status);
        refund.setErrorMessage(errorMessage);

        if (status == RefundStatus.PROCESSED) {
            refund.setProcessedAt(LocalDateTime.now());
        } else if (status == RefundStatus.FAILED) {
            refund.setFailedAt(LocalDateTime.now());
        }

        refundRepository.save(refund);

        // Publish status change event
        if (!oldStatus.equals(status)) {
            outboxEventService.publishEvent(
                    "Refund",
                    refund.getId().toString(),
                    "refund.status.changed",
                    refund
            );

            log.info("Updated refund {} status from {} to {}", 
                    refund.getId(), oldStatus, status);
        }
    }

    /**
     * Get refund status from Razorpay
     */
    public Refund refreshRefundStatus(UUID refundId) {
        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new IllegalArgumentException("Refund not found: " + refundId));

        try {
            com.razorpay.Refund razorpayRefund = razorpayClient.refunds.fetch(refund.getRazorpayRefundId());
            
            String status = razorpayRefund.get("status");
            RefundStatus newStatus = RefundStatus.valueOf(status.toUpperCase());
            
            if (!refund.getStatus().equals(newStatus)) {
                updateRefundStatus(refund.getRazorpayRefundId(), newStatus, null);
            }

            return refundRepository.findById(refundId).orElse(refund);

        } catch (Exception e) {
            log.error("Error refreshing refund status {}: {}", refundId, e.getMessage(), e);
            throw new RuntimeException("Failed to refresh refund status: " + e.getMessage(), e);
        }
    }

    /**
     * Get total refunded amount for payment
     */
    public BigDecimal getTotalRefundedAmount(UUID paymentId) {
        List<Refund> refunds = refundRepository.findByPaymentIdAndStatusNot(paymentId, RefundStatus.FAILED);
        return refunds.stream()
                .map(Refund::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Check if payment can be refunded
     */
    public boolean canRefund(UUID paymentId, BigDecimal amount) {
        PaymentTransaction payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));

        if (payment.getRazorpayPaymentId() == null) {
            return false;
        }

        BigDecimal totalRefunded = getTotalRefundedAmount(paymentId);
        return totalRefunded.add(amount).compareTo(payment.getAmount()) <= 0;
    }
}
