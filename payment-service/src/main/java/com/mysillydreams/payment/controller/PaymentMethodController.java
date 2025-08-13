package com.mysillydreams.payment.controller;

import com.mysillydreams.payment.domain.SavedPaymentMethod;
import com.mysillydreams.payment.dto.SavePaymentMethodRequest;
import com.mysillydreams.payment.service.PaymentMethodService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * REST API for managing saved payment methods
 */
@RestController
@RequestMapping("/api/payments/v1/methods")
@RequiredArgsConstructor
@Tag(name = "Payment Methods", description = "Saved payment methods management APIs")
@Slf4j
public class PaymentMethodController {

    private final PaymentMethodService paymentMethodService;

    /**
     * Save a new payment method
     */
    @PostMapping
    @Operation(summary = "Save payment method", 
               description = "Save a new payment method with tokenization for future use")
    public ResponseEntity<Map<String, Object>> savePaymentMethod(
            @Valid @RequestBody SavePaymentMethodRequest request) {
        
        log.info("Saving payment method for user: {}, type: {}", request.userId(), request.paymentType());
        
        try {
            SavedPaymentMethod savedMethod = paymentMethodService.savePaymentMethod(request);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "success", true,
                    "message", "Payment method saved successfully",
                    "paymentMethod", Map.of(
                            "id", savedMethod.getId(),
                            "displayName", savedMethod.getDisplayName(),
                            "paymentType", savedMethod.getPaymentType(),
                            "isDefault", savedMethod.getIsDefault(),
                            "maskedDetails", getMaskedDetails(savedMethod),
                            "createdAt", savedMethod.getCreatedAt()
                    )
            ));
            
        } catch (Exception e) {
            log.error("Failed to save payment method for user: {}", request.userId(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "success", false,
                    "message", "Failed to save payment method: " + e.getMessage()
            ));
        }
    }

    /**
     * Get user's saved payment methods
     */
    @GetMapping("/user/{userId}")
    @Operation(summary = "Get user payment methods", 
               description = "Retrieve all saved payment methods for a user")
    public ResponseEntity<Map<String, Object>> getUserPaymentMethods(
            @Parameter(description = "User ID") @PathVariable UUID userId) {
        
        log.info("Fetching payment methods for user: {}", userId);
        
        try {
            List<SavedPaymentMethod> paymentMethods = paymentMethodService.getUserPaymentMethods(userId);
            
            List<Map<String, Object>> methodsData = paymentMethods.stream()
                    .map(this::mapPaymentMethodToResponse)
                    .toList();
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "paymentMethods", methodsData,
                    "count", methodsData.size()
            ));
            
        } catch (Exception e) {
            log.error("Failed to fetch payment methods for user: {}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", "Failed to fetch payment methods: " + e.getMessage()
            ));
        }
    }

    /**
     * Get user's default payment method
     */
    @GetMapping("/user/{userId}/default")
    @Operation(summary = "Get default payment method", 
               description = "Get the default payment method for a user")
    public ResponseEntity<Map<String, Object>> getDefaultPaymentMethod(
            @Parameter(description = "User ID") @PathVariable UUID userId) {
        
        log.info("Fetching default payment method for user: {}", userId);
        
        try {
            Optional<SavedPaymentMethod> defaultMethod = paymentMethodService.getDefaultPaymentMethod(userId);
            
            if (defaultMethod.isPresent()) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "paymentMethod", mapPaymentMethodToResponse(defaultMethod.get())
                ));
            } else {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "paymentMethod", null,
                        "message", "No default payment method found"
                ));
            }
            
        } catch (Exception e) {
            log.error("Failed to fetch default payment method for user: {}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", "Failed to fetch default payment method: " + e.getMessage()
            ));
        }
    }

    /**
     * Set payment method as default
     */
    @PutMapping("/{paymentMethodId}/default")
    @Operation(summary = "Set as default payment method", 
               description = "Set a payment method as the default for a user")
    public ResponseEntity<Map<String, Object>> setAsDefault(
            @Parameter(description = "Payment method ID") @PathVariable UUID paymentMethodId,
            @Parameter(description = "User ID") @RequestParam UUID userId) {
        
        log.info("Setting payment method {} as default for user: {}", paymentMethodId, userId);
        
        try {
            SavedPaymentMethod updatedMethod = paymentMethodService.setAsDefault(paymentMethodId, userId);
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Payment method set as default successfully",
                    "paymentMethod", mapPaymentMethodToResponse(updatedMethod)
            ));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Failed to set payment method {} as default for user: {}", paymentMethodId, userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", "Failed to set as default: " + e.getMessage()
            ));
        }
    }

    /**
     * Delete a saved payment method
     */
    @DeleteMapping("/{paymentMethodId}")
    @Operation(summary = "Delete payment method", 
               description = "Delete a saved payment method")
    public ResponseEntity<Map<String, Object>> deletePaymentMethod(
            @Parameter(description = "Payment method ID") @PathVariable UUID paymentMethodId,
            @Parameter(description = "User ID") @RequestParam UUID userId) {
        
        log.info("Deleting payment method {} for user: {}", paymentMethodId, userId);
        
        try {
            paymentMethodService.deletePaymentMethod(paymentMethodId, userId);
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Payment method deleted successfully"
            ));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Failed to delete payment method {} for user: {}", paymentMethodId, userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", "Failed to delete payment method: " + e.getMessage()
            ));
        }
    }

    /**
     * Create payment with saved method
     */
    @PostMapping("/{paymentMethodId}/pay")
    @Operation(summary = "Pay with saved method", 
               description = "Create payment using a saved payment method (OTP-less)")
    public ResponseEntity<Map<String, Object>> payWithSavedMethod(
            @Parameter(description = "Payment method ID") @PathVariable UUID paymentMethodId,
            @Parameter(description = "User ID") @RequestParam UUID userId,
            @Parameter(description = "Amount in paise") @RequestParam long amount,
            @Parameter(description = "Currency") @RequestParam(defaultValue = "INR") String currency) {
        
        log.info("Creating payment with saved method {} for user: {}, amount: {}", 
                paymentMethodId, userId, amount);
        
        try {
            String paymentToken = paymentMethodService.createPaymentWithSavedMethod(
                    paymentMethodId, userId, amount, currency);
            
            // Mark payment method as used
            paymentMethodService.markAsUsed(paymentMethodId);
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Payment initiated successfully",
                    "paymentToken", paymentToken,
                    "amount", amount,
                    "currency", currency
            ));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Failed to create payment with saved method {} for user: {}", 
                    paymentMethodId, userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", "Failed to create payment: " + e.getMessage()
            ));
        }
    }

    /**
     * Get payment method details
     */
    @GetMapping("/{paymentMethodId}")
    @Operation(summary = "Get payment method details", 
               description = "Get details of a specific payment method")
    public ResponseEntity<Map<String, Object>> getPaymentMethodDetails(
            @Parameter(description = "Payment method ID") @PathVariable UUID paymentMethodId,
            @Parameter(description = "User ID") @RequestParam UUID userId) {
        
        log.info("Fetching payment method {} details for user: {}", paymentMethodId, userId);
        
        try {
            List<SavedPaymentMethod> userMethods = paymentMethodService.getUserPaymentMethods(userId);
            Optional<SavedPaymentMethod> method = userMethods.stream()
                    .filter(pm -> pm.getId().equals(paymentMethodId))
                    .findFirst();
            
            if (method.isPresent()) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "paymentMethod", mapPaymentMethodToResponse(method.get())
                ));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                        "success", false,
                        "message", "Payment method not found"
                ));
            }
            
        } catch (Exception e) {
            log.error("Failed to fetch payment method {} for user: {}", paymentMethodId, userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", "Failed to fetch payment method: " + e.getMessage()
            ));
        }
    }

    /**
     * Map payment method to response format
     */
    private Map<String, Object> mapPaymentMethodToResponse(SavedPaymentMethod method) {
        return Map.of(
                "id", method.getId(),
                "displayName", method.getDisplayName(),
                "paymentType", method.getPaymentType(),
                "isDefault", method.getIsDefault(),
                "isActive", method.getIsActive(),
                "maskedDetails", getMaskedDetails(method),
                "lastUsedAt", method.getLastUsedAt(),
                "createdAt", method.getCreatedAt(),
                "isExpired", method.isCardExpired()
        );
    }

    /**
     * Get masked details for display
     */
    private Map<String, Object> getMaskedDetails(SavedPaymentMethod method) {
        return switch (method.getPaymentType()) {
            case CARD -> Map.of(
                    "maskedNumber", method.getMaskedCardNumber(),
                    "brand", method.getCardBrand(),
                    "type", method.getCardType(),
                    "expiryMonth", method.getCardExpiryMonth(),
                    "expiryYear", method.getCardExpiryYear(),
                    "issuer", method.getCardIssuer()
            );
            case UPI -> Map.of(
                    "maskedVpa", method.getMaskedUpiVpa()
            );
            case WALLET -> Map.of(
                    "provider", method.getWalletProvider(),
                    "maskedPhone", method.getWalletPhone()
            );
            case NET_BANKING -> Map.of(
                    "bankName", method.getDisplayName()
            );
        };
    }
}
