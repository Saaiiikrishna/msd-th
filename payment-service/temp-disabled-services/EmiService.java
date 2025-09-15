package com.mysillydreams.payment.service;

import com.mysillydreams.payment.domain.EmiPlan;
import com.mysillydreams.payment.domain.EmiTransaction;
import com.mysillydreams.payment.domain.EmiStatus;
import com.mysillydreams.payment.repository.EmiPlanRepository;
import com.mysillydreams.payment.repository.EmiTransactionRepository;
import com.razorpay.RazorpayClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for managing EMI (Equated Monthly Installments)
 * Handles EMI options for high-value treasure hunt packages
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmiService {

    private final EmiPlanRepository emiPlanRepository;
    private final EmiTransactionRepository emiTransactionRepository;
    private final RazorpayClient razorpayClient;
    private final OutboxEventService outboxEventService;

    /**
     * Get available EMI options for amount
     */
    public List<Map<String, Object>> getEmiOptions(BigDecimal amount) {
        List<Map<String, Object>> emiOptions = new ArrayList<>();

        // EMI is typically available for amounts above ₹1000
        if (amount.compareTo(BigDecimal.valueOf(1000)) < 0) {
            return emiOptions;
        }

        try {
            // Get EMI options from Razorpay
            JSONObject emiRequest = new JSONObject();
            emiRequest.put("amount", amount.multiply(BigDecimal.valueOf(100)).intValue());
            emiRequest.put("currency", "INR");

            // This would call Razorpay EMI API to get available options
            // For now, we'll provide standard EMI options
            
            // 3 months EMI
            if (amount.compareTo(BigDecimal.valueOf(3000)) >= 0) {
                emiOptions.add(createEmiOption(amount, 3, 12.0));
            }

            // 6 months EMI
            if (amount.compareTo(BigDecimal.valueOf(5000)) >= 0) {
                emiOptions.add(createEmiOption(amount, 6, 13.5));
            }

            // 9 months EMI
            if (amount.compareTo(BigDecimal.valueOf(8000)) >= 0) {
                emiOptions.add(createEmiOption(amount, 9, 14.5));
            }

            // 12 months EMI
            if (amount.compareTo(BigDecimal.valueOf(10000)) >= 0) {
                emiOptions.add(createEmiOption(amount, 12, 15.0));
            }

            // 18 months EMI
            if (amount.compareTo(BigDecimal.valueOf(20000)) >= 0) {
                emiOptions.add(createEmiOption(amount, 18, 16.0));
            }

            // 24 months EMI
            if (amount.compareTo(BigDecimal.valueOf(30000)) >= 0) {
                emiOptions.add(createEmiOption(amount, 24, 17.0));
            }

            log.info("Generated {} EMI options for amount {}", emiOptions.size(), amount);

        } catch (Exception e) {
            log.error("Error getting EMI options for amount {}: {}", amount, e.getMessage(), e);
        }

        return emiOptions;
    }

    /**
     * Create EMI option
     */
    private Map<String, Object> createEmiOption(BigDecimal amount, int tenure, double interestRate) {
        // Calculate EMI using standard formula: P * r * (1+r)^n / ((1+r)^n - 1)
        double principal = amount.doubleValue();
        double monthlyRate = interestRate / 100 / 12;
        double emi = principal * monthlyRate * Math.pow(1 + monthlyRate, tenure) / 
                    (Math.pow(1 + monthlyRate, tenure) - 1);
        
        BigDecimal emiAmount = BigDecimal.valueOf(emi).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = emiAmount.multiply(BigDecimal.valueOf(tenure));
        BigDecimal totalInterest = totalAmount.subtract(amount);

        return Map.of(
            "tenure", tenure,
            "interest_rate", interestRate,
            "emi_amount", emiAmount,
            "total_amount", totalAmount,
            "total_interest", totalInterest,
            "processing_fee", BigDecimal.valueOf(99), // Standard processing fee
            "description", String.format("%d months @ %.1f%% p.a.", tenure, interestRate)
        );
    }

    /**
     * Create EMI plan
     */
    @Transactional
    public EmiPlan createEmiPlan(UUID userId, UUID enrollmentId, BigDecimal principalAmount,
                                int tenure, double interestRate, String cardToken) {
        try {
            // Calculate EMI details
            Map<String, Object> emiOption = createEmiOption(principalAmount, tenure, interestRate);
            BigDecimal emiAmount = (BigDecimal) emiOption.get("emi_amount");
            BigDecimal totalAmount = (BigDecimal) emiOption.get("total_amount");

            // Create EMI plan in Razorpay (if supported)
            JSONObject emiRequest = new JSONObject();
            emiRequest.put("amount", principalAmount.multiply(BigDecimal.valueOf(100)).intValue());
            emiRequest.put("currency", "INR");
            emiRequest.put("tenure", tenure);
            emiRequest.put("interest_rate", interestRate);
            emiRequest.put("token", cardToken);

            // Notes
            JSONObject notes = new JSONObject();
            notes.put("user_id", userId.toString());
            notes.put("enrollment_id", enrollmentId.toString());
            notes.put("purpose", "treasure_hunt_emi");
            emiRequest.put("notes", notes);

            // For now, we'll create our own EMI plan
            // In production, you might use Razorpay's EMI API if available

            // Save to database
            EmiPlan emiPlan = EmiPlan.builder()
                    .id(UUID.randomUUID())
                    .userId(userId)
                    .enrollmentId(enrollmentId)
                    .principalAmount(principalAmount)
                    .emiAmount(emiAmount)
                    .totalAmount(totalAmount)
                    .tenure(tenure)
                    .interestRate(BigDecimal.valueOf(interestRate))
                    .status(EmiStatus.ACTIVE)
                    .cardToken(cardToken)
                    .remainingTenure(tenure)
                    .build();

            EmiPlan savedPlan = emiPlanRepository.save(emiPlan);

            // Publish event
            outboxEventService.publishEvent(
                    "EmiPlan",
                    savedPlan.getId().toString(),
                    "emi.plan.created",
                    savedPlan
            );

            log.info("Created EMI plan {} for user {} with tenure {} months", 
                    savedPlan.getId(), userId, tenure);

            return savedPlan;

        } catch (Exception e) {
            log.error("Error creating EMI plan for user {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to create EMI plan: " + e.getMessage(), e);
        }
    }

    /**
     * Process EMI payment
     */
    @Transactional
    public EmiTransaction processEmiPayment(UUID emiPlanId) {
        EmiPlan emiPlan = emiPlanRepository.findById(emiPlanId)
                .orElseThrow(() -> new IllegalArgumentException("EMI plan not found: " + emiPlanId));

        if (emiPlan.getStatus() != EmiStatus.ACTIVE) {
            throw new IllegalStateException("EMI plan is not active: " + emiPlanId);
        }

        if (emiPlan.getRemainingTenure() <= 0) {
            throw new IllegalStateException("EMI plan is already completed: " + emiPlanId);
        }

        try {
            // Create payment using saved card token
            JSONObject paymentRequest = new JSONObject();
            paymentRequest.put("amount", emiPlan.getEmiAmount().multiply(BigDecimal.valueOf(100)).intValue());
            paymentRequest.put("currency", "INR");
            paymentRequest.put("token", emiPlan.getCardToken());
            paymentRequest.put("description", String.format("EMI payment %d of %d", 
                    emiPlan.getTenure() - emiPlan.getRemainingTenure() + 1, emiPlan.getTenure()));

            // Notes
            JSONObject notes = new JSONObject();
            notes.put("emi_plan_id", emiPlan.getId().toString());
            notes.put("user_id", emiPlan.getUserId().toString());
            notes.put("installment_number", emiPlan.getTenure() - emiPlan.getRemainingTenure() + 1);
            paymentRequest.put("notes", notes);

            // Create payment via Razorpay API
            com.razorpay.Payment razorpayPayment = razorpayClient.payments.create(paymentRequest);

            // Save EMI transaction
            EmiTransaction emiTransaction = EmiTransaction.builder()
                    .id(UUID.randomUUID())
                    .emiPlanId(emiPlan.getId())
                    .razorpayPaymentId(razorpayPayment.get("id"))
                    .installmentNumber(emiPlan.getTenure() - emiPlan.getRemainingTenure() + 1)
                    .amount(emiPlan.getEmiAmount())
                    .status(EmiStatus.PENDING)
                    .build();

            EmiTransaction savedTransaction = emiTransactionRepository.save(emiTransaction);

            // Update EMI plan
            emiPlan.setRemainingTenure(emiPlan.getRemainingTenure() - 1);
            if (emiPlan.getRemainingTenure() == 0) {
                emiPlan.setStatus(EmiStatus.COMPLETED);
            }
            emiPlanRepository.save(emiPlan);

            // Publish event
            outboxEventService.publishEvent(
                    "EmiTransaction",
                    savedTransaction.getId().toString(),
                    "emi.payment.processed",
                    savedTransaction
            );

            log.info("Processed EMI payment {} for plan {} (installment {}/{})", 
                    savedTransaction.getId(), emiPlanId, 
                    savedTransaction.getInstallmentNumber(), emiPlan.getTenure());

            return savedTransaction;

        } catch (Exception e) {
            log.error("Error processing EMI payment for plan {}: {}", emiPlanId, e.getMessage(), e);
            throw new RuntimeException("Failed to process EMI payment: " + e.getMessage(), e);
        }
    }

    /**
     * Cancel EMI plan
     */
    @Transactional
    public void cancelEmiPlan(UUID emiPlanId, String reason) {
        EmiPlan emiPlan = emiPlanRepository.findById(emiPlanId)
                .orElseThrow(() -> new IllegalArgumentException("EMI plan not found: " + emiPlanId));

        emiPlan.setStatus(EmiStatus.CANCELLED);
        emiPlan.setCancellationReason(reason);
        emiPlanRepository.save(emiPlan);

        // Publish event
        outboxEventService.publishEvent(
                "EmiPlan",
                emiPlan.getId().toString(),
                "emi.plan.cancelled",
                emiPlan
        );

        log.info("Cancelled EMI plan {} with reason: {}", emiPlanId, reason);
    }

    /**
     * Get EMI plan details
     */
    public Map<String, Object> getEmiPlanDetails(UUID emiPlanId) {
        EmiPlan emiPlan = emiPlanRepository.findById(emiPlanId)
                .orElseThrow(() -> new IllegalArgumentException("EMI plan not found: " + emiPlanId));

        List<EmiTransaction> transactions = emiTransactionRepository.findByEmiPlanIdOrderByInstallmentNumber(emiPlanId);

        return Map.of(
            "emi_plan", emiPlan,
            "transactions", transactions,
            "paid_installments", transactions.size(),
            "remaining_installments", emiPlan.getRemainingTenure(),
            "next_due_date", calculateNextDueDate(emiPlan),
            "total_paid", transactions.stream()
                    .map(EmiTransaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
        );
    }

    /**
     * Calculate next due date for EMI
     */
    private String calculateNextDueDate(EmiPlan emiPlan) {
        // This would calculate based on creation date + installment number
        // For simplicity, returning a placeholder
        return "Next month 15th";
    }
}
