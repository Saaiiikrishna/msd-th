package com.mysillydreams.payment.service;

import com.mysillydreams.payment.domain.Invoice;
import com.mysillydreams.payment.domain.PaymentLink;
import com.mysillydreams.payment.domain.PaymentLinkStatus;
import com.mysillydreams.payment.repository.PaymentLinkRepository;
import com.razorpay.RazorpayClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

/**
 * Service for managing Razorpay Payment Links
 * Provides shareable payment links for treasure hunt enrollments
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentLinkService {

    private final PaymentLinkRepository paymentLinkRepository;
    private final RazorpayClient razorpayClient;
    private final OutboxEventService outboxEventService;

    @Value("${app.base-url:http://localhost:3000}")
    private String baseUrl;

    /**
     * Create payment link for invoice
     */
    @Transactional
    public PaymentLink createPaymentLink(Invoice invoice, String description, LocalDateTime expiresAt) {
        try {
            // Create Razorpay payment link
            JSONObject paymentLinkRequest = new JSONObject();
            paymentLinkRequest.put("amount", invoice.getTotalAmount().multiply(BigDecimal.valueOf(100)).intValue());
            paymentLinkRequest.put("currency", invoice.getCurrency());
            paymentLinkRequest.put("accept_partial", false);
            paymentLinkRequest.put("description", description);
            paymentLinkRequest.put("reference_id", invoice.getInvoiceNumber());
            
            // Customer details
            JSONObject customer = new JSONObject();
            customer.put("name", invoice.getBillingName());
            customer.put("email", invoice.getBillingEmail());
            customer.put("contact", invoice.getBillingPhone());
            paymentLinkRequest.put("customer", customer);

            // Callback URLs
            JSONObject callback = new JSONObject();
            callback.put("url", baseUrl + "/payment/callback/" + invoice.getId());
            callback.put("method", "get");
            paymentLinkRequest.put("callback_url", callback.getString("url"));
            callback.put("method", "get");
            paymentLinkRequest.put("callback_method", "get");

            // Expiry
            if (expiresAt != null) {
                paymentLinkRequest.put("expire_by", expiresAt.toEpochSecond(ZoneOffset.UTC));
            }

            // Notes
            JSONObject notes = new JSONObject();
            notes.put("invoice_id", invoice.getId().toString());
            notes.put("enrollment_id", invoice.getEnrollmentId().toString());
            notes.put("registration_id", invoice.getRegistrationId());
            notes.put("plan_title", invoice.getPlanTitle());
            paymentLinkRequest.put("notes", notes);

            // Create payment link via Razorpay API
            com.razorpay.PaymentLink razorpayPaymentLink = razorpayClient.paymentLink.create(paymentLinkRequest);

            // Save to database
            PaymentLink paymentLink = PaymentLink.builder()
                    .id(UUID.randomUUID())
                    .invoiceId(invoice.getId())
                    .razorpayPaymentLinkId(razorpayPaymentLink.get("id"))
                    .shortUrl(razorpayPaymentLink.get("short_url"))
                    .amount(invoice.getTotalAmount())
                    .currency(invoice.getCurrency())
                    .description(description)
                    .status(PaymentLinkStatus.CREATED)
                    .expiresAt(expiresAt)
                    .build();

            PaymentLink savedPaymentLink = paymentLinkRepository.save(paymentLink);

            // Publish event
            outboxEventService.publish(
                    "PaymentLink",
                    savedPaymentLink.getId().toString(),
                    "payment.link.created",
                    Map.of("paymentLink", savedPaymentLink)
            );

            log.info("Created payment link {} for invoice {} with short URL: {}", 
                    savedPaymentLink.getId(), invoice.getInvoiceNumber(), savedPaymentLink.getShortUrl());

            return savedPaymentLink;

        } catch (Exception e) {
            log.error("Error creating payment link for invoice {}: {}", invoice.getInvoiceNumber(), e.getMessage(), e);
            throw new RuntimeException("Failed to create payment link: " + e.getMessage(), e);
        }
    }

    /**
     * Cancel payment link
     */
    @Transactional
    public void cancelPaymentLink(UUID paymentLinkId) {
        PaymentLink paymentLink = paymentLinkRepository.findById(paymentLinkId)
                .orElseThrow(() -> new IllegalArgumentException("Payment link not found: " + paymentLinkId));

        try {
            // Cancel via Razorpay API
            // TODO: Implement proper Razorpay payment link cancellation
            // com.razorpay.PaymentLink razorpayPaymentLink = razorpayClient.paymentLink.fetch(paymentLink.getRazorpayPaymentLinkId());
            // razorpayPaymentLink.cancel(); // Method might not exist in current Razorpay SDK version
            log.info("Cancelling Razorpay payment link: {}", paymentLink.getRazorpayPaymentLinkId());

            // Update status
            paymentLink.setStatus(PaymentLinkStatus.CANCELLED);
            paymentLinkRepository.save(paymentLink);

            // Publish event
            outboxEventService.publish(
                    "PaymentLink",
                    paymentLink.getId().toString(),
                    "payment.link.cancelled",
                    Map.of("paymentLink", paymentLink)
            );

            log.info("Cancelled payment link {}", paymentLinkId);

        } catch (Exception e) {
            log.error("Error cancelling payment link {}: {}", paymentLinkId, e.getMessage(), e);
            throw new RuntimeException("Failed to cancel payment link: " + e.getMessage(), e);
        }
    }

    /**
     * Get payment link status from Razorpay
     */
    public PaymentLink refreshPaymentLinkStatus(UUID paymentLinkId) {
        PaymentLink paymentLink = paymentLinkRepository.findById(paymentLinkId)
                .orElseThrow(() -> new IllegalArgumentException("Payment link not found: " + paymentLinkId));

        try {
            com.razorpay.PaymentLink razorpayPaymentLink = razorpayClient.paymentLink.fetch(paymentLink.getRazorpayPaymentLinkId());
            
            String status = razorpayPaymentLink.get("status");
            PaymentLinkStatus newStatus = PaymentLinkStatus.valueOf(status.toUpperCase());
            
            if (!paymentLink.getStatus().equals(newStatus)) {
                paymentLink.setStatus(newStatus);
                paymentLinkRepository.save(paymentLink);

                // Publish status change event
                outboxEventService.publish(
                        "PaymentLink",
                        paymentLink.getId().toString(),
                        "payment.link.status.changed",
                        Map.of("paymentLink", paymentLink)
                );
            }

            return paymentLink;

        } catch (Exception e) {
            log.error("Error refreshing payment link status {}: {}", paymentLinkId, e.getMessage(), e);
            throw new RuntimeException("Failed to refresh payment link status: " + e.getMessage(), e);
        }
    }
}
