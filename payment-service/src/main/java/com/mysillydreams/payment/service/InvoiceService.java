package com.mysillydreams.payment.service;

import com.mysillydreams.payment.domain.EnrollmentType;
import com.mysillydreams.payment.domain.Invoice;
import com.mysillydreams.payment.domain.PaymentStatus;
import com.mysillydreams.payment.dto.EnrollmentDetails;
import com.mysillydreams.payment.dto.PricingDetails;
import com.mysillydreams.payment.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Year;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing invoices for treasure hunt enrollments
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceService {
    
    private final InvoiceRepository invoiceRepository;
    
    /**
     * Generate invoice for enrollment
     */
    @Transactional
    public Invoice generateInvoice(EnrollmentDetails enrollmentDetails, PricingDetails pricingDetails) {
        // Use registration ID as the invoice number - no separate invoice number generation
        String registrationId = enrollmentDetails.registrationId();

        Invoice invoice = Invoice.builder()
                .invoiceNumber(registrationId) // Use registration ID as invoice number
                .enrollmentId(enrollmentDetails.enrollmentId())
                .registrationId(registrationId)
                .userId(enrollmentDetails.userId())
                .planId(enrollmentDetails.planId())
                .planTitle(enrollmentDetails.planTitle())
                .enrollmentType(EnrollmentType.valueOf(enrollmentDetails.enrollmentType().name()))
                .teamName(enrollmentDetails.teamName())
                .teamSize(enrollmentDetails.teamSize())
                .baseAmount(pricingDetails.baseAmount())
                .discountAmount(pricingDetails.discountAmount())
                .taxAmount(pricingDetails.taxAmount())
                .convenienceFee(pricingDetails.convenienceFee())
                .platformFee(pricingDetails.platformFee())
                .totalAmount(pricingDetails.totalAmount())
                .currency("INR") // Fixed to INR for v1
                .promoCode(pricingDetails.promoCode())
                .promotionName(pricingDetails.promotionName())
                .billingName(enrollmentDetails.billingName())
                .billingEmail(enrollmentDetails.billingEmail())
                .billingPhone(enrollmentDetails.billingPhone())
                .billingAddress(enrollmentDetails.billingAddress())
                .paymentStatus(PaymentStatus.PENDING)
                .build();

        Invoice savedInvoice = invoiceRepository.save(invoice);

        log.info("Generated invoice {} for enrollment {} with total amount {} INR",
                registrationId, enrollmentDetails.enrollmentId(), pricingDetails.totalAmount());

        return savedInvoice;
    }
    
    /**
     * Update invoice with payment transaction details
     */
    @Transactional
    public Invoice updateWithPaymentTransaction(UUID invoiceId, UUID paymentTransactionId, 
                                              String razorpayOrderId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + invoiceId));
        
        invoice.setPaymentTransactionId(paymentTransactionId);
        invoice.setRazorpayOrderId(razorpayOrderId);
        
        return invoiceRepository.save(invoice);
    }
    
    /**
     * Mark invoice as paid
     */
    @Transactional
    public Invoice markAsPaid(UUID invoiceId, String paymentMethod, String razorpayPaymentId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + invoiceId));
        
        invoice.markAsPaid(paymentMethod, razorpayPaymentId);
        
        Invoice savedInvoice = invoiceRepository.save(invoice);
        
        log.info("Marked invoice {} as paid with payment method {} and Razorpay payment ID {}", 
                invoice.getInvoiceNumber(), paymentMethod, razorpayPaymentId);
        
        return savedInvoice;
    }
    
    /**
     * Mark invoice as failed
     */
    @Transactional
    public Invoice markAsFailed(UUID invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + invoiceId));
        
        invoice.markAsFailed();
        
        return invoiceRepository.save(invoice);
    }
    
    /**
     * Find invoice by enrollment ID
     */
    @Transactional(readOnly = true)
    public Optional<Invoice> findByEnrollmentId(UUID enrollmentId) {
        return invoiceRepository.findByEnrollmentId(enrollmentId);
    }
    
    /**
     * Find invoice by registration ID
     */
    @Transactional(readOnly = true)
    public Optional<Invoice> findByRegistrationId(String registrationId) {
        return invoiceRepository.findByRegistrationId(registrationId);
    }
    
    /**
     * Find invoices by user ID
     */
    @Transactional(readOnly = true)
    public List<Invoice> findByUserId(UUID userId) {
        return invoiceRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
    
    /**
     * Find invoice by Razorpay order ID
     */
    @Transactional(readOnly = true)
    public Optional<Invoice> findByRazorpayOrderId(String razorpayOrderId) {
        return invoiceRepository.findByRazorpayOrderId(razorpayOrderId);
    }
    

    
    /**
     * Calculate total revenue for a period
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateTotalRevenue(java.time.Instant startDate, java.time.Instant endDate) {
        return invoiceRepository.calculateTotalRevenue(startDate, endDate);
    }
    
    /**
     * Get payment statistics
     */
    @Transactional(readOnly = true)
    public PaymentStatistics getPaymentStatistics() {
        Long totalInvoices = invoiceRepository.count();
        Long paidInvoices = invoiceRepository.countByPaymentStatus(PaymentStatus.PAID);
        Long pendingInvoices = invoiceRepository.countByPaymentStatus(PaymentStatus.PENDING);
        Long failedInvoices = invoiceRepository.countByPaymentStatus(PaymentStatus.FAILED);
        BigDecimal totalRevenue = invoiceRepository.calculateTotalRevenue(null, null);
        
        return new PaymentStatistics(totalInvoices, paidInvoices, pendingInvoices, 
                failedInvoices, totalRevenue);
    }
    
    /**
     * Payment statistics record
     */
    public record PaymentStatistics(
            Long totalInvoices,
            Long paidInvoices,
            Long pendingInvoices,
            Long failedInvoices,
            BigDecimal totalRevenue
    ) {}
}
