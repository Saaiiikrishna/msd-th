package com.mysillydreams.payment.service;

import com.mysillydreams.payment.domain.UpiAutoPayMandate;
import com.mysillydreams.payment.domain.MandateStatus;
import com.mysillydreams.payment.repository.UpiAutoPayMandateRepository;
import com.razorpay.RazorpayClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Service for managing UPI AutoPay mandates
 * Handles recurring UPI payments for treasure hunt subscriptions
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UpiAutoPayService {

    private final UpiAutoPayMandateRepository mandateRepository;
    private final RazorpayClient razorpayClient;
    private final OutboxEventService outboxEventService;

    /**
     * Create UPI AutoPay mandate
     */
    @Transactional
    public UpiAutoPayMandate createMandate(UUID userId, String customerName, String customerEmail,
                                          String customerPhone, BigDecimal maxAmount, 
                                          String frequency, LocalDateTime startDate, LocalDateTime endDate) {
        try {
            // Create customer
            JSONObject customerRequest = new JSONObject();
            customerRequest.put("name", customerName);
            customerRequest.put("email", customerEmail);
            customerRequest.put("contact", customerPhone);

            com.razorpay.Customer razorpayCustomer = razorpayClient.customers.create(customerRequest);

            // Create token for UPI AutoPay
            JSONObject tokenRequest = new JSONObject();
            tokenRequest.put("customer_id", razorpayCustomer.get("id"));
            tokenRequest.put("method", "upi");
            tokenRequest.put("max_amount", maxAmount.multiply(BigDecimal.valueOf(100)).intValue());
            tokenRequest.put("expire_at", endDate.toEpochSecond(ZoneOffset.UTC));

            // UPI specific details
            JSONObject upi = new JSONObject();
            upi.put("flow", "collect"); // or "intent"
            upi.put("vpa", ""); // Will be filled by customer during mandate creation
            tokenRequest.put("upi", upi);

            // Bank account details (optional for UPI)
            JSONObject bankAccount = new JSONObject();
            bankAccount.put("beneficiary_name", customerName);
            bankAccount.put("account_number", ""); // Not required for UPI
            bankAccount.put("ifsc", ""); // Not required for UPI
            tokenRequest.put("bank_account", bankAccount);

            // Mandate details
            JSONObject mandate = new JSONObject();
            mandate.put("max_amount", maxAmount.multiply(BigDecimal.valueOf(100)).intValue());
            mandate.put("start_at", startDate.toEpochSecond(ZoneOffset.UTC));
            mandate.put("end_at", endDate.toEpochSecond(ZoneOffset.UTC));
            mandate.put("frequency", frequency); // "daily", "weekly", "monthly", "yearly"
            tokenRequest.put("mandate", mandate);

            // Notes
            JSONObject notes = new JSONObject();
            notes.put("user_id", userId.toString());
            notes.put("purpose", "treasure_hunt_subscription");
            tokenRequest.put("notes", notes);

            // Create token via Razorpay API
            com.razorpay.Token razorpayToken = razorpayClient.customers.createToken(razorpayCustomer.get("id"), tokenRequest);

            // Save to database
            UpiAutoPayMandate mandate_entity = UpiAutoPayMandate.builder()
                    .id(UUID.randomUUID())
                    .userId(userId)
                    .razorpayTokenId(razorpayToken.get("id"))
                    .razorpayCustomerId(razorpayCustomer.get("id"))
                    .maxAmount(maxAmount)
                    .frequency(frequency)
                    .status(MandateStatus.CREATED)
                    .startDate(startDate)
                    .endDate(endDate)
                    .build();

            UpiAutoPayMandate savedMandate = mandateRepository.save(mandate_entity);

            // Publish event
            outboxEventService.publishEvent(
                    "UpiAutoPayMandate",
                    savedMandate.getId().toString(),
                    "upi.autopay.mandate.created",
                    savedMandate
            );

            log.info("Created UPI AutoPay mandate {} for user {} with max amount {}", 
                    savedMandate.getId(), userId, maxAmount);

            return savedMandate;

        } catch (Exception e) {
            log.error("Error creating UPI AutoPay mandate for user {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to create UPI AutoPay mandate: " + e.getMessage(), e);
        }
    }

    /**
     * Execute payment using UPI AutoPay mandate
     */
    @Transactional
    public String executeAutoPayment(UUID mandateId, BigDecimal amount, String description) {
        UpiAutoPayMandate mandate = mandateRepository.findById(mandateId)
                .orElseThrow(() -> new IllegalArgumentException("UPI AutoPay mandate not found: " + mandateId));

        if (mandate.getStatus() != MandateStatus.ACTIVE) {
            throw new IllegalStateException("UPI AutoPay mandate is not active: " + mandateId);
        }

        if (amount.compareTo(mandate.getMaxAmount()) > 0) {
            throw new IllegalArgumentException("Payment amount exceeds mandate limit");
        }

        try {
            // Create payment using token
            JSONObject paymentRequest = new JSONObject();
            paymentRequest.put("amount", amount.multiply(BigDecimal.valueOf(100)).intValue());
            paymentRequest.put("currency", "INR");
            paymentRequest.put("customer_id", mandate.getRazorpayCustomerId());
            paymentRequest.put("token", mandate.getRazorpayTokenId());
            paymentRequest.put("description", description);

            // Notes
            JSONObject notes = new JSONObject();
            notes.put("mandate_id", mandate.getId().toString());
            notes.put("user_id", mandate.getUserId().toString());
            notes.put("autopay", "true");
            paymentRequest.put("notes", notes);

            // Create payment via Razorpay API
            com.razorpay.Payment razorpayPayment = razorpayClient.payments.create(paymentRequest);

            // Update mandate usage
            mandate.setLastUsedAt(LocalDateTime.now());
            mandate.setUsageCount(mandate.getUsageCount() + 1);
            mandateRepository.save(mandate);

            // Publish event
            outboxEventService.publishEvent(
                    "UpiAutoPayMandate",
                    mandate.getId().toString(),
                    "upi.autopay.payment.executed",
                    Map.of(
                        "mandate_id", mandate.getId().toString(),
                        "payment_id", razorpayPayment.get("id"),
                        "amount", amount
                    )
            );

            log.info("Executed UPI AutoPay payment {} for mandate {} with amount {}", 
                    razorpayPayment.get("id"), mandateId, amount);

            return razorpayPayment.get("id");

        } catch (Exception e) {
            log.error("Error executing UPI AutoPay payment for mandate {}: {}", mandateId, e.getMessage(), e);
            throw new RuntimeException("Failed to execute UPI AutoPay payment: " + e.getMessage(), e);
        }
    }

    /**
     * Cancel UPI AutoPay mandate
     */
    @Transactional
    public void cancelMandate(UUID mandateId) {
        UpiAutoPayMandate mandate = mandateRepository.findById(mandateId)
                .orElseThrow(() -> new IllegalArgumentException("UPI AutoPay mandate not found: " + mandateId));

        try {
            // Cancel token in Razorpay
            com.razorpay.Token razorpayToken = razorpayClient.customers.fetchToken(
                    mandate.getRazorpayCustomerId(), mandate.getRazorpayTokenId());
            razorpayToken.delete();

            // Update status
            mandate.setStatus(MandateStatus.CANCELLED);
            mandate.setCancelledAt(LocalDateTime.now());
            mandateRepository.save(mandate);

            // Publish event
            outboxEventService.publishEvent(
                    "UpiAutoPayMandate",
                    mandate.getId().toString(),
                    "upi.autopay.mandate.cancelled",
                    mandate
            );

            log.info("Cancelled UPI AutoPay mandate {}", mandateId);

        } catch (Exception e) {
            log.error("Error cancelling UPI AutoPay mandate {}: {}", mandateId, e.getMessage(), e);
            throw new RuntimeException("Failed to cancel UPI AutoPay mandate: " + e.getMessage(), e);
        }
    }

    /**
     * Pause UPI AutoPay mandate
     */
    @Transactional
    public void pauseMandate(UUID mandateId) {
        UpiAutoPayMandate mandate = mandateRepository.findById(mandateId)
                .orElseThrow(() -> new IllegalArgumentException("UPI AutoPay mandate not found: " + mandateId));

        mandate.setStatus(MandateStatus.PAUSED);
        mandate.setPausedAt(LocalDateTime.now());
        mandateRepository.save(mandate);

        // Publish event
        outboxEventService.publishEvent(
                "UpiAutoPayMandate",
                mandate.getId().toString(),
                "upi.autopay.mandate.paused",
                mandate
        );

        log.info("Paused UPI AutoPay mandate {}", mandateId);
    }

    /**
     * Resume UPI AutoPay mandate
     */
    @Transactional
    public void resumeMandate(UUID mandateId) {
        UpiAutoPayMandate mandate = mandateRepository.findById(mandateId)
                .orElseThrow(() -> new IllegalArgumentException("UPI AutoPay mandate not found: " + mandateId));

        mandate.setStatus(MandateStatus.ACTIVE);
        mandate.setPausedAt(null);
        mandateRepository.save(mandate);

        // Publish event
        outboxEventService.publishEvent(
                "UpiAutoPayMandate",
                mandate.getId().toString(),
                "upi.autopay.mandate.resumed",
                mandate
        );

        log.info("Resumed UPI AutoPay mandate {}", mandateId);
    }
}
