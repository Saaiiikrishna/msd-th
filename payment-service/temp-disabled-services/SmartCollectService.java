package com.mysillydreams.payment.service;

import com.mysillydreams.payment.domain.VirtualAccount;
import com.mysillydreams.payment.domain.VirtualAccountStatus;
import com.mysillydreams.payment.repository.VirtualAccountRepository;
import com.razorpay.RazorpayClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Service for managing Razorpay Smart Collect (Virtual Accounts)
 * Handles bulk payments and custom payment collection
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SmartCollectService {

    private final VirtualAccountRepository virtualAccountRepository;
    private final RazorpayClient razorpayClient;
    private final OutboxEventService outboxEventService;

    /**
     * Create virtual account for bulk payments
     */
    @Transactional
    public VirtualAccount createVirtualAccount(String customerName, String customerEmail, 
                                             String description, BigDecimal expectedAmount,
                                             LocalDateTime expiresAt) {
        try {
            // Create virtual account request
            JSONObject vaRequest = new JSONObject();
            
            // Receivers - what payment methods to accept
            JSONArray receivers = new JSONArray();
            
            // Bank account receiver
            JSONObject bankReceiver = new JSONObject();
            bankReceiver.put("types", new JSONArray().put("bank_account"));
            receivers.put(bankReceiver);
            
            // UPI receiver
            JSONObject upiReceiver = new JSONObject();
            upiReceiver.put("types", new JSONArray().put("vpa"));
            receivers.put(upiReceiver);
            
            vaRequest.put("receivers", receivers);

            // Customer details
            JSONObject customer = new JSONObject();
            customer.put("name", customerName);
            customer.put("email", customerEmail);
            vaRequest.put("customer", customer);

            // Description and notes
            vaRequest.put("description", description);
            JSONObject notes = new JSONObject();
            notes.put("purpose", "treasure_hunt_bulk_payment");
            notes.put("customer_email", customerEmail);
            if (expectedAmount != null) {
                notes.put("expected_amount", expectedAmount.toString());
            }
            vaRequest.put("notes", notes);

            // Expiry
            if (expiresAt != null) {
                vaRequest.put("close_by", expiresAt.toEpochSecond(ZoneOffset.UTC));
            }

            // Create virtual account via Razorpay API
            com.razorpay.VirtualAccount razorpayVA = razorpayClient.virtualAccounts.create(vaRequest);

            // Extract bank account and UPI details
            JSONArray receiversArray = razorpayVA.toJson().getJSONArray("receivers");
            String bankAccountNumber = null;
            String ifscCode = null;
            String upiId = null;

            for (int i = 0; i < receiversArray.length(); i++) {
                JSONObject receiver = receiversArray.getJSONObject(i);
                if (receiver.has("bank_account")) {
                    JSONObject bankAccount = receiver.getJSONObject("bank_account");
                    bankAccountNumber = bankAccount.optString("account_number");
                    ifscCode = bankAccount.optString("ifsc");
                } else if (receiver.has("vpa")) {
                    JSONObject vpa = receiver.getJSONObject("vpa");
                    upiId = vpa.optString("address");
                }
            }

            // Save to database
            VirtualAccount virtualAccount = VirtualAccount.builder()
                    .id(UUID.randomUUID())
                    .razorpayVirtualAccountId(razorpayVA.get("id"))
                    .customerName(customerName)
                    .customerEmail(customerEmail)
                    .description(description)
                    .expectedAmount(expectedAmount)
                    .bankAccountNumber(bankAccountNumber)
                    .ifscCode(ifscCode)
                    .upiId(upiId)
                    .status(VirtualAccountStatus.ACTIVE)
                    .expiresAt(expiresAt)
                    .build();

            VirtualAccount savedVA = virtualAccountRepository.save(virtualAccount);

            // Publish event
            outboxEventService.publishEvent(
                    "VirtualAccount",
                    savedVA.getId().toString(),
                    "virtual.account.created",
                    savedVA
            );

            log.info("Created virtual account {} with bank account {} and UPI {}", 
                    savedVA.getId(), bankAccountNumber, upiId);

            return savedVA;

        } catch (Exception e) {
            log.error("Error creating virtual account for {}: {}", customerEmail, e.getMessage(), e);
            throw new RuntimeException("Failed to create virtual account: " + e.getMessage(), e);
        }
    }

    /**
     * Create virtual account for specific treasure hunt event
     */
    @Transactional
    public VirtualAccount createEventVirtualAccount(UUID eventId, String eventName, 
                                                   BigDecimal registrationFee, LocalDateTime eventDate) {
        String description = String.format("Payment collection for %s (Event ID: %s)", eventName, eventId);
        String customerName = "Treasure Hunt Event Collection";
        String customerEmail = "events@mysillydreams.com";

        return createVirtualAccount(customerName, customerEmail, description, registrationFee, eventDate);
    }

    /**
     * Close virtual account
     */
    @Transactional
    public void closeVirtualAccount(UUID virtualAccountId) {
        VirtualAccount virtualAccount = virtualAccountRepository.findById(virtualAccountId)
                .orElseThrow(() -> new IllegalArgumentException("Virtual account not found: " + virtualAccountId));

        try {
            // Close via Razorpay API
            com.razorpay.VirtualAccount razorpayVA = razorpayClient.virtualAccounts
                    .fetch(virtualAccount.getRazorpayVirtualAccountId());
            razorpayVA.close();

            // Update status
            virtualAccount.setStatus(VirtualAccountStatus.CLOSED);
            virtualAccount.setClosedAt(LocalDateTime.now());
            virtualAccountRepository.save(virtualAccount);

            // Publish event
            outboxEventService.publishEvent(
                    "VirtualAccount",
                    virtualAccount.getId().toString(),
                    "virtual.account.closed",
                    virtualAccount
            );

            log.info("Closed virtual account {}", virtualAccountId);

        } catch (Exception e) {
            log.error("Error closing virtual account {}: {}", virtualAccountId, e.getMessage(), e);
            throw new RuntimeException("Failed to close virtual account: " + e.getMessage(), e);
        }
    }

    /**
     * Handle payment received on virtual account
     */
    @Transactional
    public void handleVirtualAccountPayment(String razorpayVirtualAccountId, String razorpayPaymentId, 
                                          BigDecimal amount, String method) {
        VirtualAccount virtualAccount = virtualAccountRepository
                .findByRazorpayVirtualAccountId(razorpayVirtualAccountId)
                .orElseThrow(() -> new IllegalArgumentException("Virtual account not found: " + razorpayVirtualAccountId));

        try {
            // Update received amount
            BigDecimal currentReceived = virtualAccount.getAmountReceived() != null ? 
                    virtualAccount.getAmountReceived() : BigDecimal.ZERO;
            virtualAccount.setAmountReceived(currentReceived.add(amount));
            virtualAccount.setLastPaymentAt(LocalDateTime.now());

            // Check if expected amount is reached
            if (virtualAccount.getExpectedAmount() != null && 
                virtualAccount.getAmountReceived().compareTo(virtualAccount.getExpectedAmount()) >= 0) {
                virtualAccount.setStatus(VirtualAccountStatus.PAID);
            }

            virtualAccountRepository.save(virtualAccount);

            // Publish payment received event
            JSONObject paymentData = new JSONObject();
            paymentData.put("virtual_account_id", virtualAccount.getId().toString());
            paymentData.put("razorpay_payment_id", razorpayPaymentId);
            paymentData.put("amount", amount);
            paymentData.put("method", method);

            outboxEventService.publishEvent(
                    "VirtualAccount",
                    virtualAccount.getId().toString(),
                    "virtual.account.payment.received",
                    paymentData.toString()
            );

            log.info("Received payment {} on virtual account {} via {}", 
                    amount, virtualAccount.getId(), method);

        } catch (Exception e) {
            log.error("Error handling virtual account payment {}: {}", razorpayVirtualAccountId, e.getMessage(), e);
            throw new RuntimeException("Failed to handle virtual account payment: " + e.getMessage(), e);
        }
    }

    /**
     * Get virtual account status from Razorpay
     */
    public VirtualAccount refreshVirtualAccountStatus(UUID virtualAccountId) {
        VirtualAccount virtualAccount = virtualAccountRepository.findById(virtualAccountId)
                .orElseThrow(() -> new IllegalArgumentException("Virtual account not found: " + virtualAccountId));

        try {
            com.razorpay.VirtualAccount razorpayVA = razorpayClient.virtualAccounts
                    .fetch(virtualAccount.getRazorpayVirtualAccountId());
            
            String status = razorpayVA.get("status");
            VirtualAccountStatus newStatus = VirtualAccountStatus.valueOf(status.toUpperCase());
            
            if (!virtualAccount.getStatus().equals(newStatus)) {
                virtualAccount.setStatus(newStatus);
                if (newStatus == VirtualAccountStatus.CLOSED) {
                    virtualAccount.setClosedAt(LocalDateTime.now());
                }
                virtualAccountRepository.save(virtualAccount);

                // Publish status change event
                outboxEventService.publishEvent(
                        "VirtualAccount",
                        virtualAccount.getId().toString(),
                        "virtual.account.status.changed",
                        virtualAccount
                );
            }

            return virtualAccount;

        } catch (Exception e) {
            log.error("Error refreshing virtual account status {}: {}", virtualAccountId, e.getMessage(), e);
            throw new RuntimeException("Failed to refresh virtual account status: " + e.getMessage(), e);
        }
    }
}
