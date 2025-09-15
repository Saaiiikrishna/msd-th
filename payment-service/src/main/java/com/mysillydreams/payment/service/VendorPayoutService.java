package com.mysillydreams.payment.service;

import com.mysillydreams.payment.config.CommissionProperties;
import com.mysillydreams.payment.domain.PaymentTransaction; // Assuming this is needed for context
import com.mysillydreams.payment.domain.PayoutStatus;
import com.mysillydreams.payment.domain.PayoutTransaction;
import com.mysillydreams.payment.dto.VendorPayoutFailedEvent;
import com.mysillydreams.payment.dto.VendorPayoutInitiatedEvent;
import com.mysillydreams.payment.dto.VendorPayoutSucceededEvent;
import com.mysillydreams.payment.repository.PaymentRepository; // To fetch PaymentTransaction
import com.mysillydreams.payment.repository.PayoutTransactionRepository;
import com.mysillydreams.payment.repository.VendorProfileRepository;
import com.mysillydreams.payment.domain.VendorProfile;
import com.mysillydreams.payment.domain.RazorpayPayout;
import com.mysillydreams.payment.service.OutboxEventService;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
// @RequiredArgsConstructor // Cannot use with manual constructor for metrics
@Slf4j
public class VendorPayoutService {

    private final PayoutTransactionRepository payoutTransactionRepository;
    private final PaymentRepository paymentRepository;
    private final VendorProfileRepository vendorProfileRepository;
    private final RazorpayClient razorpayClient;
    private final OutboxEventService outboxEventService;
    private final CommissionProperties commissionProperties;
    private final MeterRegistry meterRegistry;
    private final RestTemplate restTemplate;

    // Metrics
    private final Counter payoutSuccessTotal;    // Programmatic
    private final Counter payoutFailureTotal;    // Programmatic
    // @Counted for payment.service.payouts.attempts.total will be on initiatePayout method
    // @Timed for Razorpay Payouts.create will be on helper method callRazorpayCreatePayout

    @Value("${kafka.topics.vendorPayoutInitiated:vendor.payout.initiated}")
    private String vendorPayoutInitiatedTopic;
    @Value("${kafka.topics.vendorPayoutSucceeded:vendor.payout.succeeded}")
    private String vendorPayoutSucceededTopic;
    @Value("${kafka.topics.vendorPayoutFailed:vendor.payout.failed}")
    private String vendorPayoutFailedTopic;

    @Value("${payment.razorpay.payout.account-id}")
    private String razorpayXAccountId;

    @Value("${payment.razorpay.key-id}")
    private String razorpayKeyId;

    @Value("${payment.razorpay.key-secret}")
    private String razorpayKeySecret;

    public VendorPayoutService(PayoutTransactionRepository payoutTransactionRepository,
                               PaymentRepository paymentRepository,
                               VendorProfileRepository vendorProfileRepository,
                               RazorpayClient razorpayClient,
                               OutboxEventService outboxEventService,
                               CommissionProperties commissionProperties,
                               MeterRegistry meterRegistry,
                               RestTemplate restTemplate) {
        this.payoutTransactionRepository = payoutTransactionRepository;
        this.paymentRepository = paymentRepository;
        this.vendorProfileRepository = vendorProfileRepository;
        this.razorpayClient = razorpayClient;
        this.outboxEventService = outboxEventService;
        this.commissionProperties = commissionProperties;
        this.restTemplate = restTemplate;
        this.meterRegistry = meterRegistry; // Keep for programmatic counters

        // Initialize programmatic counters
        this.payoutSuccessTotal = Counter.builder("payment.service.payouts.success.total") // Renamed for consistency
                .description("Total number of successful vendor payouts (confirmed by API/webhook)")
                .register(meterRegistry);
        this.payoutFailureTotal = Counter.builder("payment.service.payouts.failure.total") // Renamed for consistency
                .description("Total number of failed vendor payouts (confirmed by API/webhook)")
                .register(meterRegistry);
    }

    @Transactional // Main transaction for creating PayoutTransaction and initiating event
    // User sketch: @Timed(value = "vendor.payout.time", ...) @Counted(value = "vendor.payout.count", ...)
    // My current @Counted is "payment.service.payouts.attempts.total". I'll align with user sketch.
    @Timed(value = "vendor.payout.time", description = "Time taken to initiate vendor payout")
    @Counted(value = "vendor.payout.count", description = "Number of vendor payouts initiated")
    public UUID initiatePayout(UUID paymentTransactionId, UUID vendorId, BigDecimal grossAmount, String currency) {
        log.info("Initiating payout for PaymentTransaction ID: {}, Vendor ID: {}, Amount: {} {}",
                paymentTransactionId, vendorId, grossAmount, currency);

        PaymentTransaction paymentTx = paymentRepository.findById(paymentTransactionId)
                .orElseThrow(() -> new IllegalArgumentException("PaymentTransaction not found with ID: " + paymentTransactionId));

        BigDecimal commissionRate = commissionProperties.getPercent().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        BigDecimal commissionAmount = grossAmount.multiply(commissionRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal netAmount = grossAmount.subtract(commissionAmount);

        if (netAmount.compareTo(BigDecimal.ZERO) <= 0) {
            log.error("Net amount for payout is zero or negative for PaymentTransaction ID: {}. Gross: {}, Commission: {}",
                    paymentTransactionId, grossAmount, commissionAmount);
            // This scenario should ideally not happen or be handled by creating a FAILED PayoutTransaction directly.
            // For now, throwing an exception to prevent INIT PayoutTransaction with no payout amount.
            throw new IllegalStateException("Net payout amount is not positive.");
        }

        PayoutTransaction payoutTx = new PayoutTransaction(
                paymentTx, vendorId, grossAmount, commissionAmount, netAmount, currency, PayoutStatus.INIT);
        payoutTransactionRepository.save(payoutTx);
        log.info("Saved PayoutTransaction ID: {} in INIT state.", payoutTx.getId());

        // Publish VendorPayoutInitiatedEvent via Outbox
        VendorPayoutInitiatedEvent initiatedEvent = new VendorPayoutInitiatedEvent(
                payoutTx.getId(),
                vendorId,
                netAmount,
                currency,
                null // razorpayPayoutId will be set later when payout is created
        );
        outboxEventService.publish("VendorPayout", payoutTx.getId().toString(), vendorPayoutInitiatedTopic,
                // Convert Avro SpecificRecord to Map for outbox payload if needed, or ensure KafkaTemplate handles SpecificRecord
                // For consistency with existing outbox (Map<String,Object>), convert. ObjectMapper can do this.
                // Or, if KafkaTemplate is <String, SpecificRecord>, then pass directly.
                // Current template is <String, Object>, so SpecificRecord should work if Avro serdes are setup.
                // Let's assume SpecificRecord is fine for KafkaTemplate<String, Object> with Avro Serializer.
                // The OutboxPoller sends `event.getPayload()` which is Map.
                // So, for consistency, the payload for outbox should be a Map.
                 Map.of(
                     "payoutId", initiatedEvent.payoutId().toString(),
                     "vendorId", initiatedEvent.vendorId().toString(),
                     "netAmount", initiatedEvent.amount(),
                     "currency", initiatedEvent.currency(),
                     "razorpayPayoutId", initiatedEvent.razorpayPayoutId() != null ? initiatedEvent.razorpayPayoutId() : "",
                     "initiatedAt", System.currentTimeMillis()
                 )
        );
        log.info("Published VendorPayoutInitiatedEvent for Payout ID: {} to outbox.", payoutTx.getId());

        // Trigger asynchronous processing of Razorpay Payout API call
        processRazorpayPayoutAsync(payoutTx); // Call the async method

        return payoutTx.getId();
    }

    @Async // Execute in a separate thread, outside the initial transaction
    @Transactional(propagation = Propagation.REQUIRES_NEW) // New transaction for this async operation
    public void processRazorpayPayoutAsync(PayoutTransaction payoutTransaction) {
        log.info("Async processing Razorpay payout for PayoutTransaction ID: {}", payoutTransaction.getId());
        PayoutTransaction pt = payoutTransactionRepository.findById(payoutTransaction.getId()).orElse(null);
        if (pt == null || pt.getStatus() != PayoutStatus.INIT) {
             log.warn("PayoutTransaction ID {} no longer in INIT state or not found. Current status: {}. Aborting Razorpay call.",
                payoutTransaction.getId(), pt != null ? pt.getStatus() : "NOT_FOUND");
            return;
        }

        try {
            // Get vendor bank account details from vendor profile
            VendorProfile vendorProfile = vendorProfileRepository.findByVendorId(pt.getVendorId())
                    .orElseThrow(() -> new IllegalStateException("Vendor profile not found for vendor: " + pt.getVendorId()));

            if (vendorProfile.getBankAccountNumber() == null || vendorProfile.getBankIfscCode() == null) {
                throw new IllegalStateException("Bank account details not found for vendor: " + pt.getVendorId());
            }

            JSONObject payoutRequest = new JSONObject();
            payoutRequest.put("account_number", razorpayXAccountId); // Your RazorpayX account number: 2323230097650454

            // Create fund account inline with bank account details
            JSONObject fundAccount = new JSONObject();
            fundAccount.put("account_type", "bank_account");

            JSONObject bankAccount = new JSONObject();
            bankAccount.put("name", vendorProfile.getBankAccountHolderName());
            bankAccount.put("ifsc", vendorProfile.getBankIfscCode());
            bankAccount.put("account_number", vendorProfile.getBankAccountNumber());

            fundAccount.put("bank_account", bankAccount);

            // Add contact details for fund account
            JSONObject contact = new JSONObject();
            contact.put("name", vendorProfile.getVendorName());
            contact.put("email", vendorProfile.getVendorEmail());
            if (vendorProfile.getVendorPhone() != null) {
                contact.put("contact", vendorProfile.getVendorPhone());
            }
            contact.put("type", "vendor");

            fundAccount.put("contact", contact);
            payoutRequest.put("fund_account", fundAccount); // Use bank account details directly

            payoutRequest.put("amount", pt.getNetAmount().multiply(BigDecimal.valueOf(100)).intValueExact()); // Amount in paise
            payoutRequest.put("currency", pt.getCurrency());
            payoutRequest.put("mode", "IMPS"); // Or NEFT, RTGS, UPI
            payoutRequest.put("purpose", "vendor_payout"); // e.g., "vendor_payout", "refund", "cashback"
            payoutRequest.put("queue_if_low_balance", true); // Or false based on policy
            payoutRequest.put("reference_id", "PAYOUT_" + pt.getId().toString()); // Your internal reference
            payoutRequest.put("narration", "Vendor payout for " + vendorProfile.getVendorName());

            // Add notes with vendor details
            JSONObject notes = new JSONObject();
            notes.put("vendor_id", pt.getVendorId().toString());
            notes.put("vendor_name", vendorProfile.getVendorName());
            notes.put("payment_id", pt.getPaymentTransaction().getId().toString());
            payoutRequest.put("notes", notes);

            // Call resilience-enabled helper method
            RazorpayPayout razorpayPayout = callRazorpayCreatePayout(payoutRequest);

            String rzpPayoutId = razorpayPayout.getId();
            String rzpStatus = razorpayPayout.getStatus();

            log.info("Razorpay Payout API call successful for Payout ID: {}. Razorpay Payout ID: {}, Razorpay Status: {}, Bank Account: {}",
                    pt.getId(), rzpPayoutId, rzpStatus, vendorProfile.getBankAccountNumber());

            pt.setRazorpayPayoutId(rzpPayoutId);
            pt.setStatus(PayoutStatus.PENDING);
            payoutTransactionRepository.save(pt);
            log.info("PayoutTransaction ID {} updated to PENDING with Razorpay Payout ID {}.", pt.getId(), rzpPayoutId);

        } catch (RazorpayException e) {
            log.error("RazorpayException during payout for Payout ID {}: {}",
                    pt.getId(), e.getMessage(), e);
            pt.setStatus(PayoutStatus.FAILED);
            pt.setErrorCode(e.getClass().getSimpleName());
            pt.setErrorMessage(e.getMessage());
            payoutTransactionRepository.save(pt);
            payoutFailureTotal.increment(); // Increment failure counter for API call

            VendorPayoutFailedEvent failedEvent = new VendorPayoutFailedEvent(
                    pt.getId(),
                    pt.getVendorId(),
                    pt.getNetAmount(),
                    pt.getCurrency(),
                    e.getMessage()
            );
            outboxEventService.publish("VendorPayout", pt.getId().toString(), vendorPayoutFailedTopic,
                    Map.of(
                         "payoutId", failedEvent.payoutId().toString(),
                         "vendorId", failedEvent.vendorId().toString(),
                         "amount", failedEvent.amount(),
                         "currency", failedEvent.currency(),
                         "errorMessage", failedEvent.errorMessage(),
                         "failedAt", System.currentTimeMillis()
                     )
            );
            log.warn("PayoutTransaction ID {} marked FAILED due to Razorpay API error. Published failure event to outbox.", pt.getId());
        } catch (Exception e) { // Catch other unexpected errors
            log.error("Unexpected exception during async Razorpay payout for Payout ID {}: {}", pt.getId(), e.getMessage(), e);
            pt.setStatus(PayoutStatus.FAILED);
            pt.setErrorCode(e.getClass().getSimpleName());
            pt.setErrorMessage("Unexpected error: " + e.getMessage());
            payoutTransactionRepository.save(pt);
            payoutFailureTotal.increment(); // Increment failure counter for unexpected error during API call phase
            // Publish failure event (similar to above)
             VendorPayoutFailedEvent failedEvent = new VendorPayoutFailedEvent(
                    pt.getId(),
                    pt.getVendorId(),
                    pt.getNetAmount(),
                    pt.getCurrency(),
                    "InternalError: " + e.getMessage()
            );
            outboxEventService.publish("VendorPayout", pt.getId().toString(), vendorPayoutFailedTopic,
                 Map.of(
                         "payoutId", failedEvent.payoutId().toString(),
                         "vendorId", failedEvent.vendorId().toString(),
                         "amount", failedEvent.amount(),
                         "currency", failedEvent.currency(),
                         "errorMessage", failedEvent.errorMessage(),
                         "failedAt", System.currentTimeMillis()
                     )
            );
        }
    }

    @Transactional // For updating PayoutTransaction from webhook
    public void handlePayoutSuccess(String razorpayPayoutId, Instant processedAt) {
        log.info("Handling successful payout webhook for Razorpay Payout ID: {}", razorpayPayoutId);
        PayoutTransaction pt = payoutTransactionRepository.findByRazorpayPayoutId(razorpayPayoutId)
                .orElseThrow(() -> {
                    log.warn("PayoutTransaction not found for Razorpay Payout ID: {} from webhook.", razorpayPayoutId);
                    // Depending on policy, might ignore or log as an issue.
                    return new IllegalArgumentException("PayoutTransaction not found for razorpayPayoutId: " + razorpayPayoutId);
                });

        if (pt.getStatus() == PayoutStatus.SUCCESS) {
            log.info("Payout ID {} already marked as SUCCESS. Ignoring webhook.", pt.getId());
            return;
        }
        if (pt.getStatus() == PayoutStatus.FAILED) {
            log.warn("Payout ID {} was FAILED but received a SUCCESS webhook for Razorpay ID {}. Manual investigation needed.", pt.getId(), razorpayPayoutId);
            // This indicates a potential inconsistency. For now, we might honor the success webhook.
        }


        pt.setStatus(PayoutStatus.SUCCESS);
        // pt.setUpdatedAt(processedAt); // Or let @UpdateTimestamp handle it
        payoutTransactionRepository.save(pt);
        payoutSuccessTotal.increment(); // Increment success counter from webhook
        log.info("PayoutTransaction ID {} marked SUCCESS.", pt.getId());

        VendorPayoutSucceededEvent succeededEvent = new VendorPayoutSucceededEvent(
                pt.getId(),
                pt.getVendorId(),
                pt.getNetAmount(),
                pt.getCurrency(),
                razorpayPayoutId
        );
        outboxEventService.publish("VendorPayout", pt.getId().toString(), vendorPayoutSucceededTopic,
                Map.of(
                    "payoutId", succeededEvent.payoutId().toString(),
                    "vendorId", succeededEvent.vendorId().toString(),
                    "razorpayPayoutId", succeededEvent.razorpayPayoutId(),
                    "amount", succeededEvent.amount(),
                    "currency", succeededEvent.currency(),
                    "processedAt", System.currentTimeMillis()
                )
        );
        log.info("Published VendorPayoutSucceededEvent for Payout ID: {} to outbox.", pt.getId());
    }

    @Transactional
    public void handlePayoutFailed(String razorpayPayoutId, String errorCode, String errorMessage, Instant failedAt) {
        log.info("Handling failed payout webhook for Razorpay Payout ID: {}, Error: {} - {}", razorpayPayoutId, errorCode, errorMessage);
         PayoutTransaction pt = payoutTransactionRepository.findByRazorpayPayoutId(razorpayPayoutId)
                .orElseThrow(() -> {
                    log.warn("PayoutTransaction not found for Razorpay Payout ID: {} from failed webhook.", razorpayPayoutId);
                    return new IllegalArgumentException("PayoutTransaction not found for razorpayPayoutId: " + razorpayPayoutId);
                });

        if (pt.getStatus() == PayoutStatus.FAILED && pt.getRazorpayPayoutId() != null && pt.getRazorpayPayoutId().equals(razorpayPayoutId)) {
            log.info("Payout ID {} already marked FAILED. Ignoring webhook.", pt.getId());
            return;
        }
         if (pt.getStatus() == PayoutStatus.SUCCESS) {
            log.warn("Payout ID {} was SUCCESS but received a FAILED webhook for Razorpay ID {}. Manual investigation needed.", pt.getId(), razorpayPayoutId);
            // This indicates a potential inconsistency.
        }

        pt.setStatus(PayoutStatus.FAILED);
        pt.setErrorCode(errorCode);
        pt.setErrorMessage(errorMessage);
        payoutTransactionRepository.save(pt);
        payoutFailureTotal.increment(); // Increment failure counter from webhook
        log.info("PayoutTransaction ID {} marked FAILED due to webhook.", pt.getId());

        VendorPayoutFailedEvent failedEvent = new VendorPayoutFailedEvent(
                pt.getId(),
                pt.getVendorId(),
                pt.getNetAmount(),
                pt.getCurrency(),
                errorCode + ": " + errorMessage
        );
         outboxEventService.publish("VendorPayout", pt.getId().toString(), vendorPayoutFailedTopic,
                Map.of(
                    "payoutId", failedEvent.payoutId().toString(),
                    "vendorId", failedEvent.vendorId().toString(),
                    "amount", failedEvent.amount(),
                    "currency", failedEvent.currency(),
                    "errorMessage", failedEvent.errorMessage(),
                    "failedAt", System.currentTimeMillis()
                )
        );
        log.info("Published VendorPayoutFailedEvent for Payout ID: {} to outbox due to webhook.", pt.getId());
    }




    // --- Resilience4j Helper Method & Fallback for Razorpay Payouts.create ---

    @Timed(value = "payment.service.razorpay.payouts.create.timer", description = "Timer for Razorpay Payouts create API calls", percentiles = {0.5, 0.95, 0.99})
    @CircuitBreaker(name = "razorpayPayoutsApi", fallbackMethod = "createPayoutFallback")
    @Retry(name = "razorpayApiRetry") // Using the same general retry policy
    protected RazorpayPayout callRazorpayCreatePayout(JSONObject payoutRequest) throws RazorpayException {
        log.debug("Calling Razorpay Payouts API: {}", payoutRequest.toString());

        try {
            // Use RestTemplate to call Razorpay Payouts API directly
            // Since the Java SDK doesn't have full payout support, we'll use HTTP client
            String url = "https://api.razorpay.com/v1/payouts";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            // Use injected Razorpay credentials
            headers.setBasicAuth(razorpayKeyId, razorpayKeySecret);

            HttpEntity<String> entity = new HttpEntity<>(payoutRequest.toString(), headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                JSONObject responseJson = new JSONObject(response.getBody());
                log.debug("Razorpay Payout API response: {}", responseJson.toString());
                return RazorpayPayout.fromJson(responseJson);
            } else {
                throw new RazorpayException("Payout API call failed with status: " + response.getStatusCode());
            }

        } catch (Exception e) {
            log.error("Error calling Razorpay Payout API: {}", e.getMessage(), e);
            if (e instanceof RazorpayException) {
                throw e;
            }
            throw new RazorpayException("Failed to create payout: " + e.getMessage());
        }
    }

    protected RazorpayPayout createPayoutFallback(JSONObject payoutRequest, Throwable t) throws RazorpayException {
        log.warn("Fallback for callRazorpayCreatePayout due to: {}. Request: {}", t.getMessage(), payoutRequest.toString());
        if (t instanceof RazorpayException) throw (RazorpayException) t;
        // Wrap other exceptions in RazorpayException to be handled by the calling method's catch block
        throw new RazorpayException("Razorpay Payouts.create call failed and fallback triggered: " + t.getMessage());
    }

    /**
     * Get payout transaction by ID
     */
    public PayoutTransaction getPayoutTransaction(UUID payoutId) {
        return payoutTransactionRepository.findById(payoutId)
                .orElseThrow(() -> new IllegalArgumentException("Payout transaction not found: " + payoutId));
    }

    /**
     * Get all payouts for a vendor
     */
    public List<PayoutTransaction> getPayoutsByVendor(UUID vendorId) {
        return payoutTransactionRepository.findByVendorIdOrderByCreatedAtDesc(vendorId);
    }
}
