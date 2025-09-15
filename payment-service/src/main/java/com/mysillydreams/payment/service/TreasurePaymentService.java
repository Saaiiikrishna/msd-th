package com.mysillydreams.payment.service;

import com.mysillydreams.payment.domain.EnrollmentType;
import com.mysillydreams.payment.domain.Invoice;
import com.mysillydreams.payment.domain.PaymentTransaction;
import com.mysillydreams.payment.dto.EnrollmentDetails;
import com.mysillydreams.payment.dto.PricingDetails;
import com.mysillydreams.payment.dto.TreasureEnrollmentEvent;
import com.mysillydreams.payment.repository.InvoiceRepository;
import com.mysillydreams.payment.repository.PaymentRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Enhanced payment service for Treasure Hunt business integration
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TreasurePaymentService {
    
    private final InvoiceService invoiceService;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final RazorpayClient razorpayClient;
    private final OutboxEventService outboxEventService;
    private final VendorPayoutService vendorPayoutService;
    
    /**
     * Process treasure hunt enrollment payment
     */
    @Transactional
    public TreasurePaymentResult processEnrollmentPayment(TreasureEnrollmentEvent enrollmentEvent) {
        log.info("Processing treasure hunt enrollment payment for enrollment ID: {}, registration ID: {}", 
                enrollmentEvent.enrollmentId(), enrollmentEvent.registrationId());
        
        try {
            // 1. Generate invoice
            EnrollmentDetails enrollmentDetails = mapToEnrollmentDetails(enrollmentEvent);
            PricingDetails pricingDetails = mapToPricingDetails(enrollmentEvent);
            
            Invoice invoice = invoiceService.generateInvoice(enrollmentDetails, pricingDetails);
            
            // 2. Create Razorpay order (INR only)
            Order razorpayOrder = createRazorpayOrder(invoice);

            // 3. Create payment transaction with Razorpay order ID
            PaymentTransaction transaction = createPaymentTransaction(invoice, razorpayOrder.get("id"), enrollmentEvent.vendorId());
            PaymentTransaction savedTransaction = paymentRepository.save(transaction);

            // 4. Update invoice with payment transaction details
            invoice = invoiceService.updateWithPaymentTransaction(
                    invoice.getId(),
                    savedTransaction.getId(),
                    razorpayOrder.get("id")
            );
            
            log.info("Created Razorpay order {} for invoice {} with amount {} INR", 
                    razorpayOrder.get("id"), invoice.getInvoiceNumber(), invoice.getTotalAmount());
            
            return TreasurePaymentResult.success(
                    invoice,
                    savedTransaction,
                    razorpayOrder.get("id"),
                    createPaymentLink(razorpayOrder)
            );
            
        } catch (Exception e) {
            log.error("Failed to process enrollment payment for enrollment ID: {}", 
                    enrollmentEvent.enrollmentId(), e);
            return TreasurePaymentResult.failure(e.getMessage());
        }
    }
    
    /**
     * Handle successful payment
     */
    @Transactional
    public void handlePaymentSuccess(String razorpayOrderId, String razorpayPaymentId, String paymentMethod) {
        log.info("Handling payment success for Razorpay order: {}, payment: {}", razorpayOrderId, razorpayPaymentId);
        
        // Find invoice by Razorpay order ID
        Invoice invoice = invoiceService.findByRazorpayOrderId(razorpayOrderId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found for Razorpay order: " + razorpayOrderId));
        
        // Mark invoice as paid
        invoice = invoiceService.markAsPaid(invoice.getId(), paymentMethod, razorpayPaymentId);
        
        // Update payment transaction
        PaymentTransaction transaction = paymentRepository.findById(invoice.getPaymentTransactionId())
                .orElseThrow(() -> new IllegalArgumentException("Payment transaction not found"));
        
        transaction.setStatus("CAPTURED");
        transaction.setRazorpayPaymentId(razorpayPaymentId);
        paymentRepository.save(transaction);
        
        // Trigger vendor payout if vendor is assigned
        if (transaction.getVendorId() != null) {
            log.info("Initiating vendor payout for transaction {} to vendor {}",
                    transaction.getId(), transaction.getVendorId());

            try {
                vendorPayoutService.initiatePayout(
                        transaction.getId(),
                        transaction.getVendorId(),
                        transaction.getAmount(),
                        transaction.getCurrency()
                );
                log.info("Vendor payout initiated successfully for transaction {}", transaction.getId());
            } catch (Exception e) {
                log.error("Failed to initiate vendor payout for transaction {}: {}",
                        transaction.getId(), e.getMessage(), e);
                // Don't fail the payment processing if payout fails
            }
        } else {
            log.info("No vendor assigned for transaction {}, skipping payout", transaction.getId());
        }

        // Publish payment success event
        outboxEventService.publish(
                "TreasurePayment",
                invoice.getId().toString(),
                "treasure.payment.succeeded",
                java.util.Map.of(
                        "enrollmentId", invoice.getEnrollmentId().toString(),
                        "registrationId", invoice.getRegistrationId(),
                        "invoiceId", invoice.getId().toString(),
                        "totalAmount", invoice.getTotalAmount().toString(),
                        "currency", invoice.getCurrency(),
                        "paymentMethod", paymentMethod,
                        "razorpayPaymentId", razorpayPaymentId,
                        "vendorId", transaction.getVendorId() != null ? transaction.getVendorId().toString() : ""
                )
        );

        log.info("Successfully processed payment for invoice {} with total amount {} INR",
                invoice.getInvoiceNumber(), invoice.getTotalAmount());
    }
    
    /**
     * Handle failed payment
     */
    @Transactional
    public void handlePaymentFailure(String razorpayOrderId, String errorMessage) {
        log.warn("Handling payment failure for Razorpay order: {}, error: {}", razorpayOrderId, errorMessage);
        
        Invoice invoice = invoiceService.findByRazorpayOrderId(razorpayOrderId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found for Razorpay order: " + razorpayOrderId));
        
        // Mark invoice as failed
        invoice = invoiceService.markAsFailed(invoice.getId());
        
        // Update payment transaction
        if (invoice.getPaymentTransactionId() != null) {
            PaymentTransaction transaction = paymentRepository.findById(invoice.getPaymentTransactionId())
                    .orElse(null);
            if (transaction != null) {
                transaction.setStatus("FAILED");
                transaction.setErrorMessage(errorMessage);
                paymentRepository.save(transaction);
            }
        }
        
        // Publish payment failure event
        outboxEventService.publish(
                "TreasurePayment",
                invoice.getId().toString(),
                "treasure.payment.failed",
                java.util.Map.of(
                        "enrollmentId", invoice.getEnrollmentId().toString(),
                        "registrationId", invoice.getRegistrationId(),
                        "invoiceId", invoice.getId().toString(),
                        "errorMessage", errorMessage
                )
        );
    }
    
    /**
     * Create Razorpay order for INR transactions
     */
    private Order createRazorpayOrder(Invoice invoice) throws RazorpayException {
        // Convert to paise (smallest unit for INR)
        long amountInPaise = invoice.getTotalAmount().multiply(BigDecimal.valueOf(100)).longValue();
        
        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", amountInPaise);
        orderRequest.put("currency", "INR"); // Fixed to INR for v1
        orderRequest.put("receipt", invoice.getInvoiceNumber());
        orderRequest.put("payment_capture", 1); // Auto-capture
        
        // Add notes with enrollment details
        JSONObject notes = new JSONObject();
        notes.put("enrollment_id", invoice.getEnrollmentId().toString());
        notes.put("registration_id", invoice.getRegistrationId());
        notes.put("plan_id", invoice.getPlanId().toString());
        notes.put("enrollment_type", invoice.getEnrollmentType().name());
        if (invoice.getTeamName() != null) {
            notes.put("team_name", invoice.getTeamName());
        }
        orderRequest.put("notes", notes);
        
        return razorpayClient.orders.create(orderRequest);
    }
    
    /**
     * Create payment transaction from invoice with Razorpay order ID
     */
    private PaymentTransaction createPaymentTransaction(Invoice invoice, String razorpayOrderId, UUID vendorId) {
        return new PaymentTransaction(
                invoice.getId(),
                invoice.getEnrollmentId(),
                invoice.getTotalAmount(),
                invoice.getCurrency(),
                "PENDING",
                razorpayOrderId, // Razorpay order ID from created order
                null,  // Razorpay payment ID will be set later
                vendorId // Vendor ID for payout tracking
        );
    }
    
    /**
     * Create payment link for frontend
     */
    private String createPaymentLink(Order razorpayOrder) {
        // In a real implementation, this would generate a proper payment link
        // For now, return the order ID that can be used with Razorpay checkout
        return "https://checkout.razorpay.com/v1/checkout.js?order_id=" + razorpayOrder.get("id");
    }
    
    /**
     * Map enrollment event to enrollment details
     */
    private EnrollmentDetails mapToEnrollmentDetails(TreasureEnrollmentEvent event) {
        return new EnrollmentDetails(
                event.enrollmentId(),
                event.registrationId(),
                event.userId(),
                event.planId(),
                event.planTitle(),
                EnrollmentType.valueOf(event.enrollmentType().name()),
                event.teamName(),
                event.teamSize(),
                event.billingName(),
                event.billingEmail(),
                event.billingPhone(),
                event.billingAddress()
        );
    }
    
    /**
     * Map enrollment event to pricing details
     */
    private PricingDetails mapToPricingDetails(TreasureEnrollmentEvent event) {
        return new PricingDetails(
                event.baseAmount(),
                event.discountAmount(),
                event.taxAmount(),
                event.convenienceFee(),
                event.platformFee(),
                event.totalAmount(),
                event.promoCode(),
                event.promotionName()
        );
    }

    /**
     * Get user's order history
     */
    public List<Invoice> getUserOrderHistory(UUID userId) {
        log.info("Fetching order history for user: {}", userId);
        return invoiceRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * Get user's payment status for a specific registration
     */
    public Optional<Invoice> getUserPaymentStatus(UUID userId, String registrationId) {
        log.info("Fetching payment status for user: {} and registration: {}", userId, registrationId);

        Optional<Invoice> invoice = invoiceRepository.findByRegistrationId(registrationId);

        // Verify the invoice belongs to the user
        if (invoice.isPresent() && !invoice.get().getUserId().equals(userId)) {
            log.warn("Invoice {} does not belong to user {}", registrationId, userId);
            return Optional.empty();
        }

        return invoice;
    }

    /**
     * Result of treasure payment processing
     */
    public record TreasurePaymentResult(
            boolean success,
            String errorMessage,
            Invoice invoice,
            PaymentTransaction transaction,
            String razorpayOrderId,
            String paymentLink
    ) {
        public static TreasurePaymentResult success(Invoice invoice, PaymentTransaction transaction, 
                                                  String razorpayOrderId, String paymentLink) {
            return new TreasurePaymentResult(true, null, invoice, transaction, razorpayOrderId, paymentLink);
        }
        
        public static TreasurePaymentResult failure(String errorMessage) {
            return new TreasurePaymentResult(false, errorMessage, null, null, null, null);
        }
    }
}
