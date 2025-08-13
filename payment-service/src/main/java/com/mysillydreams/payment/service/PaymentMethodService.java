package com.mysillydreams.payment.service;

import com.mysillydreams.payment.domain.PaymentMethodType;
import com.mysillydreams.payment.domain.SavedPaymentMethod;
import com.mysillydreams.payment.dto.SavePaymentMethodRequest;
import com.mysillydreams.payment.repository.SavedPaymentMethodRepository;
import com.razorpay.Customer;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Token;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing saved payment methods with Razorpay tokenization
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentMethodService {
    
    private final SavedPaymentMethodRepository paymentMethodRepository;
    private final RazorpayClient razorpayClient;
    
    /**
     * Save payment method with tokenization
     */
    @Transactional
    public SavedPaymentMethod savePaymentMethod(SavePaymentMethodRequest request) {
        log.info("Saving payment method for user: {}, type: {}", request.userId(), request.paymentType());
        
        try {
            // Create or get Razorpay customer
            String razorpayCustomerId = getOrCreateRazorpayCustomer(request.userId(), request.customerEmail());
            
            // Tokenize payment method with Razorpay
            String razorpayToken = tokenizePaymentMethod(razorpayCustomerId, request);
            
            // Remove default status from other payment methods if this is set as default
            if (request.isDefault()) {
                paymentMethodRepository.removeDefaultForUser(request.userId());
            }
            
            // Create saved payment method
            SavedPaymentMethod paymentMethod = SavedPaymentMethod.builder()
                    .userId(request.userId())
                    .paymentType(request.paymentType())
                    .razorpayToken(razorpayToken)
                    .razorpayCustomerId(razorpayCustomerId)
                    .displayName(request.displayName())
                    .isDefault(request.isDefault())
                    .build();
            
            // Set type-specific details
            switch (request.paymentType()) {
                case CARD -> setCardDetails(paymentMethod, request);
                case UPI -> setUpiDetails(paymentMethod, request);
                case WALLET -> setWalletDetails(paymentMethod, request);
                case NET_BANKING -> setNetBankingDetails(paymentMethod, request);
            }
            
            SavedPaymentMethod saved = paymentMethodRepository.save(paymentMethod);
            
            log.info("Successfully saved payment method {} for user {}", saved.getId(), request.userId());
            return saved;
            
        } catch (Exception e) {
            log.error("Failed to save payment method for user {}", request.userId(), e);
            throw new RuntimeException("Failed to save payment method: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get user's saved payment methods
     */
    @Transactional(readOnly = true)
    public List<SavedPaymentMethod> getUserPaymentMethods(UUID userId) {
        return paymentMethodRepository.findByUserIdAndIsActiveTrueOrderByIsDefaultDescCreatedAtDesc(userId);
    }
    
    /**
     * Get user's default payment method
     */
    @Transactional(readOnly = true)
    public Optional<SavedPaymentMethod> getDefaultPaymentMethod(UUID userId) {
        return paymentMethodRepository.findByUserIdAndIsDefaultTrueAndIsActiveTrue(userId);
    }
    
    /**
     * Set payment method as default
     */
    @Transactional
    public SavedPaymentMethod setAsDefault(UUID paymentMethodId, UUID userId) {
        SavedPaymentMethod paymentMethod = paymentMethodRepository.findByIdAndUserId(paymentMethodId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Payment method not found"));
        
        // Remove default from other methods
        paymentMethodRepository.removeDefaultForUser(userId);
        
        // Set this as default
        paymentMethod.setAsDefault();
        
        return paymentMethodRepository.save(paymentMethod);
    }
    
    /**
     * Delete saved payment method
     */
    @Transactional
    public void deletePaymentMethod(UUID paymentMethodId, UUID userId) {
        SavedPaymentMethod paymentMethod = paymentMethodRepository.findByIdAndUserId(paymentMethodId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Payment method not found"));
        
        try {
            // Delete token from Razorpay
            if (paymentMethod.getRazorpayToken() != null) {
                deleteRazorpayToken(paymentMethod.getRazorpayToken());
            }
            
            // Soft delete (mark as inactive)
            paymentMethod.setIsActive(false);
            paymentMethodRepository.save(paymentMethod);
            
            log.info("Deleted payment method {} for user {}", paymentMethodId, userId);
            
        } catch (Exception e) {
            log.error("Failed to delete payment method {} for user {}", paymentMethodId, userId, e);
            throw new RuntimeException("Failed to delete payment method: " + e.getMessage(), e);
        }
    }
    
    /**
     * Mark payment method as used
     */
    @Transactional
    public void markAsUsed(UUID paymentMethodId) {
        SavedPaymentMethod paymentMethod = paymentMethodRepository.findById(paymentMethodId)
                .orElseThrow(() -> new IllegalArgumentException("Payment method not found"));
        
        paymentMethod.markAsUsed();
        paymentMethodRepository.save(paymentMethod);
    }
    
    /**
     * Create payment with saved method (OTP-less)
     */
    public String createPaymentWithSavedMethod(UUID paymentMethodId, UUID userId, long amountInPaise, String currency) {
        SavedPaymentMethod paymentMethod = paymentMethodRepository.findByIdAndUserId(paymentMethodId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Payment method not found"));
        
        if (paymentMethod.isCardExpired()) {
            throw new IllegalArgumentException("Payment method has expired");
        }
        
        try {
            // Create payment with saved token
            JSONObject paymentRequest = new JSONObject();
            paymentRequest.put("amount", amountInPaise);
            paymentRequest.put("currency", currency);
            paymentRequest.put("token", paymentMethod.getRazorpayToken());
            paymentRequest.put("customer_id", paymentMethod.getRazorpayCustomerId());
            
            // This would use Razorpay's recurring payment API
            // For now, return the token for frontend processing
            return paymentMethod.getRazorpayToken();
            
        } catch (Exception e) {
            log.error("Failed to create payment with saved method {} for user {}", paymentMethodId, userId, e);
            throw new RuntimeException("Failed to create payment: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get or create Razorpay customer
     */
    private String getOrCreateRazorpayCustomer(UUID userId, String email) throws RazorpayException {
        // Check if customer already exists
        Optional<SavedPaymentMethod> existingMethod = paymentMethodRepository.findFirstByUserIdAndRazorpayCustomerIdIsNotNull(userId);
        
        if (existingMethod.isPresent()) {
            return existingMethod.get().getRazorpayCustomerId();
        }
        
        // Create new Razorpay customer
        JSONObject customerRequest = new JSONObject();
        customerRequest.put("name", "User " + userId.toString().substring(0, 8));
        customerRequest.put("email", email);
        customerRequest.put("contact", ""); // Would be populated from user profile
        
        JSONObject notes = new JSONObject();
        notes.put("user_id", userId.toString());
        customerRequest.put("notes", notes);
        
        Customer customer = razorpayClient.customers.create(customerRequest);
        return customer.get("id");
    }
    
    /**
     * Tokenize payment method with Razorpay
     */
    private String tokenizePaymentMethod(String customerId, SavePaymentMethodRequest request) throws RazorpayException {
        JSONObject tokenRequest = new JSONObject();
        tokenRequest.put("customer_id", customerId);
        tokenRequest.put("method", getMethodForRazorpay(request.paymentType()));
        
        // Add method-specific details
        switch (request.paymentType()) {
            case CARD -> {
                JSONObject card = new JSONObject();
                card.put("number", request.cardNumber());
                card.put("name", request.cardHolderName());
                card.put("expiry_month", request.cardExpiryMonth());
                card.put("expiry_year", request.cardExpiryYear());
                card.put("cvv", request.cardCvv());
                tokenRequest.put("card", card);
            }
            case UPI -> {
                JSONObject upi = new JSONObject();
                upi.put("vpa", request.upiVpa());
                tokenRequest.put("upi", upi);
            }
            // Add other payment method types as needed
        }
        
        // TODO: Implement token creation with correct Razorpay SDK API
        // Token token = razorpayClient.tokens.create(tokenRequest);
        // return token.get("id");
        throw new UnsupportedOperationException("Token creation not yet implemented - requires correct Razorpay SDK API");
    }
    
    /**
     * Delete Razorpay token
     */
    private void deleteRazorpayToken(String tokenId) throws RazorpayException {
        // TODO: Implement token deletion with correct Razorpay SDK API
        // razorpayClient.tokens.delete(tokenId);
        log.info("Token deletion not yet implemented for tokenId: {}", tokenId);
    }
    
    /**
     * Get Razorpay method name
     */
    private String getMethodForRazorpay(PaymentMethodType type) {
        return switch (type) {
            case CARD -> "card";
            case UPI -> "upi";
            case WALLET -> "wallet";
            case NET_BANKING -> "netbanking";
        };
    }
    
    /**
     * Set card-specific details
     */
    private void setCardDetails(SavedPaymentMethod paymentMethod, SavePaymentMethodRequest request) {
        paymentMethod.setCardLastFour(request.cardNumber().substring(request.cardNumber().length() - 4));
        paymentMethod.setCardBrand(request.cardBrand());
        paymentMethod.setCardType(request.cardType());
        paymentMethod.setCardExpiryMonth(request.cardExpiryMonth());
        paymentMethod.setCardExpiryYear(request.cardExpiryYear());
        paymentMethod.setCardIssuer(request.cardIssuer());
    }
    
    /**
     * Set UPI-specific details
     */
    private void setUpiDetails(SavedPaymentMethod paymentMethod, SavePaymentMethodRequest request) {
        paymentMethod.setUpiVpa(maskUpiVpa(request.upiVpa()));
    }
    
    /**
     * Set wallet-specific details
     */
    private void setWalletDetails(SavedPaymentMethod paymentMethod, SavePaymentMethodRequest request) {
        paymentMethod.setWalletProvider(request.walletProvider());
        paymentMethod.setWalletPhone(maskPhoneNumber(request.walletPhone()));
    }
    
    /**
     * Set net banking details
     */
    private void setNetBankingDetails(SavedPaymentMethod paymentMethod, SavePaymentMethodRequest request) {
        // Net banking typically doesn't store sensitive details
        // Just the bank name in display name
    }
    
    /**
     * Mask UPI VPA for storage
     */
    private String maskUpiVpa(String vpa) {
        if (vpa == null || !vpa.contains("@")) {
            return vpa;
        }
        
        String[] parts = vpa.split("@");
        String username = parts[0];
        String provider = parts[1];
        
        if (username.length() <= 3) {
            return vpa;
        }
        
        String masked = username.substring(0, 2) + "***" + username.substring(username.length() - 1);
        return masked + "@" + provider;
    }
    
    /**
     * Mask phone number for storage
     */
    private String maskPhoneNumber(String phone) {
        if (phone == null || phone.length() < 6) {
            return phone;
        }
        
        return phone.substring(0, 2) + "****" + phone.substring(phone.length() - 2);
    }
}
