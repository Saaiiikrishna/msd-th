package com.mysillydreams.payment.controller;

import com.mysillydreams.payment.service.TreasurePaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payments/v1/test")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Payment Testing", description = "Payment flow testing with different payment methods")
public class PaymentTestController {

    private final TreasurePaymentService treasurePaymentService;

    @Operation(
            summary = "Simulate Card Payment Success",
            description = "Simulates a successful card payment with test card details"
    )
    @PostMapping("/card/success")
    public ResponseEntity<Map<String, Object>> simulateCardPaymentSuccess(
            @Parameter(description = "Razorpay order ID", required = true)
            @RequestParam String razorpayOrderId,
            @Parameter(description = "Card type (VISA, MASTERCARD, AMEX)", required = false)
            @RequestParam(defaultValue = "VISA") String cardType) {
        
        log.info("Simulating card payment success for order: {}, card type: {}", razorpayOrderId, cardType);
        
        // Generate test payment ID based on card type
        String testPaymentId = generateTestPaymentId(cardType);
        String paymentMethod = "card";
        
        try {
            // Call the payment success handler
            treasurePaymentService.handlePaymentSuccess(razorpayOrderId, testPaymentId, paymentMethod);
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Card payment simulation successful",
                    "razorpayOrderId", razorpayOrderId,
                    "razorpayPaymentId", testPaymentId,
                    "paymentMethod", paymentMethod,
                    "cardType", cardType,
                    "simulationType", "CARD_SUCCESS"
            ));
            
        } catch (Exception e) {
            log.error("Card payment simulation failed", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage(),
                    "simulationType", "CARD_SUCCESS"
            ));
        }
    }

    @Operation(
            summary = "Simulate UPI Payment Success",
            description = "Simulates a successful UPI payment"
    )
    @PostMapping("/upi/success")
    public ResponseEntity<Map<String, Object>> simulateUpiPaymentSuccess(
            @Parameter(description = "Razorpay order ID", required = true)
            @RequestParam String razorpayOrderId,
            @Parameter(description = "UPI provider (GPAY, PHONEPE, PAYTM)", required = false)
            @RequestParam(defaultValue = "GPAY") String upiProvider) {
        
        log.info("Simulating UPI payment success for order: {}, provider: {}", razorpayOrderId, upiProvider);
        
        String testPaymentId = generateTestUpiPaymentId(upiProvider);
        String paymentMethod = "upi";
        
        try {
            treasurePaymentService.handlePaymentSuccess(razorpayOrderId, testPaymentId, paymentMethod);
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "UPI payment simulation successful",
                    "razorpayOrderId", razorpayOrderId,
                    "razorpayPaymentId", testPaymentId,
                    "paymentMethod", paymentMethod,
                    "upiProvider", upiProvider,
                    "simulationType", "UPI_SUCCESS"
            ));
            
        } catch (Exception e) {
            log.error("UPI payment simulation failed", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage(),
                    "simulationType", "UPI_SUCCESS"
            ));
        }
    }

    @Operation(
            summary = "Simulate Wallet Payment Success",
            description = "Simulates a successful wallet payment"
    )
    @PostMapping("/wallet/success")
    public ResponseEntity<Map<String, Object>> simulateWalletPaymentSuccess(
            @Parameter(description = "Razorpay order ID", required = true)
            @RequestParam String razorpayOrderId,
            @Parameter(description = "Wallet provider (PAYTM, PHONEPE, MOBIKWIK)", required = false)
            @RequestParam(defaultValue = "PAYTM") String walletProvider) {
        
        log.info("Simulating wallet payment success for order: {}, provider: {}", razorpayOrderId, walletProvider);
        
        String testPaymentId = generateTestWalletPaymentId(walletProvider);
        String paymentMethod = "wallet";
        
        try {
            treasurePaymentService.handlePaymentSuccess(razorpayOrderId, testPaymentId, paymentMethod);
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Wallet payment simulation successful",
                    "razorpayOrderId", razorpayOrderId,
                    "razorpayPaymentId", testPaymentId,
                    "paymentMethod", paymentMethod,
                    "walletProvider", walletProvider,
                    "simulationType", "WALLET_SUCCESS"
            ));
            
        } catch (Exception e) {
            log.error("Wallet payment simulation failed", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage(),
                    "simulationType", "WALLET_SUCCESS"
            ));
        }
    }

    @Operation(
            summary = "Simulate Net Banking Payment Success",
            description = "Simulates a successful net banking payment"
    )
    @PostMapping("/netbanking/success")
    public ResponseEntity<Map<String, Object>> simulateNetBankingPaymentSuccess(
            @Parameter(description = "Razorpay order ID", required = true)
            @RequestParam String razorpayOrderId,
            @Parameter(description = "Bank name (HDFC, ICICI, SBI)", required = false)
            @RequestParam(defaultValue = "HDFC") String bankName) {
        
        log.info("Simulating net banking payment success for order: {}, bank: {}", razorpayOrderId, bankName);
        
        String testPaymentId = generateTestNetBankingPaymentId(bankName);
        String paymentMethod = "netbanking";
        
        try {
            treasurePaymentService.handlePaymentSuccess(razorpayOrderId, testPaymentId, paymentMethod);
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Net banking payment simulation successful",
                    "razorpayOrderId", razorpayOrderId,
                    "razorpayPaymentId", testPaymentId,
                    "paymentMethod", paymentMethod,
                    "bankName", bankName,
                    "simulationType", "NETBANKING_SUCCESS"
            ));
            
        } catch (Exception e) {
            log.error("Net banking payment simulation failed", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage(),
                    "simulationType", "NETBANKING_SUCCESS"
            ));
        }
    }

    @Operation(
            summary = "Simulate Payment Failure",
            description = "Simulates a payment failure with different error scenarios"
    )
    @PostMapping("/failure")
    public ResponseEntity<Map<String, Object>> simulatePaymentFailure(
            @Parameter(description = "Razorpay order ID", required = true)
            @RequestParam String razorpayOrderId,
            @Parameter(description = "Failure reason", required = false)
            @RequestParam(defaultValue = "INSUFFICIENT_FUNDS") String failureReason) {
        
        log.info("Simulating payment failure for order: {}, reason: {}", razorpayOrderId, failureReason);
        
        String errorMessage = getErrorMessageForReason(failureReason);
        
        try {
            treasurePaymentService.handlePaymentFailure(razorpayOrderId, errorMessage);
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Payment failure simulation successful",
                    "razorpayOrderId", razorpayOrderId,
                    "failureReason", failureReason,
                    "errorMessage", errorMessage,
                    "simulationType", "PAYMENT_FAILURE"
            ));
            
        } catch (Exception e) {
            log.error("Payment failure simulation failed", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage(),
                    "simulationType", "PAYMENT_FAILURE"
            ));
        }
    }

    // Helper methods for generating test payment IDs
    private String generateTestPaymentId(String cardType) {
        String prefix = switch (cardType.toUpperCase()) {
            case "VISA" -> "pay_visa_test_";
            case "MASTERCARD" -> "pay_mc_test_";
            case "AMEX" -> "pay_amex_test_";
            default -> "pay_card_test_";
        };
        return prefix + System.currentTimeMillis();
    }

    private String generateTestUpiPaymentId(String provider) {
        String prefix = switch (provider.toUpperCase()) {
            case "GPAY" -> "pay_gpay_test_";
            case "PHONEPE" -> "pay_phonepe_test_";
            case "PAYTM" -> "pay_paytm_test_";
            default -> "pay_upi_test_";
        };
        return prefix + System.currentTimeMillis();
    }

    private String generateTestWalletPaymentId(String provider) {
        String prefix = switch (provider.toUpperCase()) {
            case "PAYTM" -> "pay_paytm_wallet_test_";
            case "PHONEPE" -> "pay_phonepe_wallet_test_";
            case "MOBIKWIK" -> "pay_mobikwik_test_";
            default -> "pay_wallet_test_";
        };
        return prefix + System.currentTimeMillis();
    }

    private String generateTestNetBankingPaymentId(String bankName) {
        String prefix = switch (bankName.toUpperCase()) {
            case "HDFC" -> "pay_hdfc_nb_test_";
            case "ICICI" -> "pay_icici_nb_test_";
            case "SBI" -> "pay_sbi_nb_test_";
            default -> "pay_nb_test_";
        };
        return prefix + System.currentTimeMillis();
    }

    private String getErrorMessageForReason(String reason) {
        return switch (reason.toUpperCase()) {
            case "INSUFFICIENT_FUNDS" -> "Payment failed due to insufficient funds in account";
            case "CARD_DECLINED" -> "Payment failed as card was declined by issuing bank";
            case "EXPIRED_CARD" -> "Payment failed due to expired card";
            case "INVALID_CVV" -> "Payment failed due to invalid CVV";
            case "NETWORK_ERROR" -> "Payment failed due to network connectivity issues";
            case "BANK_ERROR" -> "Payment failed due to bank server error";
            case "UPI_TIMEOUT" -> "UPI payment failed due to timeout";
            case "WALLET_INSUFFICIENT" -> "Wallet payment failed due to insufficient wallet balance";
            default -> "Payment failed due to unknown error: " + reason;
        };
    }
}
