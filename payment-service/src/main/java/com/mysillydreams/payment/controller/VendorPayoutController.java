package com.mysillydreams.payment.controller;

import com.mysillydreams.payment.domain.PayoutTransaction;
import com.mysillydreams.payment.domain.VendorProfile;
import com.mysillydreams.payment.service.VendorPayoutService;
import com.mysillydreams.payment.service.VendorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments/v1/vendor")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Vendor Payouts", description = "Vendor payout management and commission handling")
public class VendorPayoutController {

    private final VendorPayoutService vendorPayoutService;
    private final VendorService vendorService;

    @Operation(
            summary = "Create vendor profile",
            description = "Creates a new vendor profile with bank account and commission settings"
    )
    @PostMapping("/profiles")
    public ResponseEntity<VendorProfile> createVendorProfile(
            @Parameter(description = "Vendor profile creation request", required = true)
            @RequestBody CreateVendorProfileRequest request) {
        
        log.info("Creating vendor profile for vendor ID: {}", request.vendorId());
        
        VendorService.VendorInfo vendorInfo = new VendorService.VendorInfo(
                request.vendorId(),
                request.vendorName(),
                request.vendorEmail(),
                request.vendorPhone(),
                request.bankAccountNumber(),
                request.ifscCode(),
                request.accountHolderName(),
                request.commissionRate()
        );
        
        VendorProfile profile = vendorService.createVendorProfile(vendorInfo);
        return ResponseEntity.ok(profile);
    }

    @Operation(
            summary = "Get vendor profile",
            description = "Retrieves vendor profile by vendor ID"
    )
    @GetMapping("/profiles/{vendorId}")
    public ResponseEntity<VendorProfile> getVendorProfile(
            @Parameter(description = "Vendor ID", required = true)
            @PathVariable UUID vendorId) {
        
        VendorProfile profile = vendorService.getVendorProfile(vendorId);
        return ResponseEntity.ok(profile);
    }

    @Operation(
            summary = "Calculate vendor payout",
            description = "Calculates commission and net payout amount for a vendor"
    )
    @PostMapping("/calculate-payout")
    public ResponseEntity<VendorService.VendorPayoutCalculation> calculatePayout(
            @Parameter(description = "Payout calculation request", required = true)
            @RequestBody CalculatePayoutRequest request) {
        
        log.info("Calculating payout for vendor ID: {}, amount: {}", 
                request.vendorId(), request.grossAmount());
        
        VendorService.VendorPayoutCalculation calculation = 
                vendorService.calculateVendorPayout(request.vendorId(), request.grossAmount());
        
        return ResponseEntity.ok(calculation);
    }

    @Operation(
            summary = "Initiate vendor payout",
            description = "Initiates a payout to vendor after commission deduction"
    )
    @PostMapping("/payouts")
    public ResponseEntity<PayoutResponse> initiatePayout(
            @Parameter(description = "Payout initiation request", required = true)
            @RequestBody InitiatePayoutRequest request) {
        
        log.info("Initiating payout for payment transaction: {}, vendor: {}", 
                request.paymentTransactionId(), request.vendorId());
        
        UUID payoutId = vendorPayoutService.initiatePayout(
                request.paymentTransactionId(),
                request.vendorId(),
                request.grossAmount(),
                request.currency()
        );
        
        return ResponseEntity.ok(new PayoutResponse(
                payoutId,
                "Payout initiated successfully",
                true
        ));
    }

    @Operation(
            summary = "Get payout status",
            description = "Retrieves the status of a vendor payout"
    )
    @GetMapping("/payouts/{payoutId}")
    public ResponseEntity<PayoutTransaction> getPayoutStatus(
            @Parameter(description = "Payout ID", required = true)
            @PathVariable UUID payoutId) {
        
        PayoutTransaction payout = vendorPayoutService.getPayoutTransaction(payoutId);
        return ResponseEntity.ok(payout);
    }

    @Operation(
            summary = "Get vendor payouts",
            description = "Retrieves all payouts for a specific vendor"
    )
    @GetMapping("/payouts/vendor/{vendorId}")
    public ResponseEntity<List<PayoutTransaction>> getVendorPayouts(
            @Parameter(description = "Vendor ID", required = true)
            @PathVariable UUID vendorId) {

        List<PayoutTransaction> payouts = vendorPayoutService.getPayoutsByVendor(vendorId);
        return ResponseEntity.ok(payouts);
    }

    @Operation(
            summary = "Test vendor payout flow",
            description = "Creates a test vendor profile and simulates the complete payout flow"
    )
    @PostMapping("/test-vendor-flow")
    public ResponseEntity<Map<String, Object>> testVendorFlow() {
        log.info("Testing complete vendor payout flow");

        try {
            // Create test vendor
            UUID testVendorId = UUID.randomUUID();
            VendorService.VendorInfo vendorInfo = new VendorService.VendorInfo(
                    testVendorId,
                    "Test Vendor Ltd",
                    "vendor@test.com",
                    "+91-9876543210",
                    "1234567890123456",
                    "HDFC0000123",
                    "Test Vendor Account",
                    BigDecimal.valueOf(5.0) // 5% commission
            );

            VendorProfile vendorProfile = vendorService.createVendorProfile(vendorInfo);

            Map<String, Object> result = Map.of(
                    "success", true,
                    "message", "Test vendor created successfully",
                    "vendorId", testVendorId.toString(),
                    "vendorProfile", vendorProfile,
                    "instructions", "Use this vendor ID in plan creation to test automatic payouts"
            );

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Error in vendor flow test: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    // DTOs
    public record CreateVendorProfileRequest(
            UUID vendorId,
            String vendorName,
            String vendorEmail,
            String vendorPhone,
            String bankAccountNumber,
            String ifscCode,
            String accountHolderName,
            BigDecimal commissionRate
    ) {}

    public record CalculatePayoutRequest(
            UUID vendorId,
            BigDecimal grossAmount
    ) {}

    public record InitiatePayoutRequest(
            UUID paymentTransactionId,
            UUID vendorId,
            BigDecimal grossAmount,
            String currency
    ) {}

    public record PayoutResponse(
            UUID payoutId,
            String message,
            boolean success
    ) {}
}
