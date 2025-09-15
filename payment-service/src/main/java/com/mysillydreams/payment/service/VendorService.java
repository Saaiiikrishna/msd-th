package com.mysillydreams.payment.service;

import com.mysillydreams.payment.domain.VendorProfile;
import com.mysillydreams.payment.dto.UserServiceVendorInfo;
import com.mysillydreams.payment.repository.VendorProfileRepository;
import com.razorpay.FundAccount;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing vendor information and fund accounts
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VendorService {
    
    private final VendorProfileRepository vendorProfileRepository;
    private final RazorpayClient razorpayClient;
    private final RestTemplate restTemplate;
    
    // This would be configured via properties
    private final String userServiceBaseUrl = "http://user-service/api/users/v1";
    
    /**
     * Get or create vendor profile with fund account
     */
    @Transactional
    public VendorProfile getOrCreateVendorProfile(UUID vendorId) {
        Optional<VendorProfile> existingProfile = vendorProfileRepository.findByVendorId(vendorId);
        
        if (existingProfile.isPresent()) {
            return existingProfile.get();
        }
        
        // Fetch vendor information from User service
        UserServiceVendorInfo vendorInfo = fetchVendorInfoFromUserService(vendorId);
        
        // Create Razorpay fund account
        String fundAccountId = createRazorpayFundAccount(vendorInfo);
        
        // Create vendor profile
        VendorProfile vendorProfile = VendorProfile.builder()
                .vendorId(vendorId)
                .vendorName(vendorInfo.name())
                .vendorEmail(vendorInfo.email())
                .vendorPhone(vendorInfo.phone())
                .bankAccountNumber(vendorInfo.bankAccountNumber())
                .bankIfscCode(vendorInfo.bankIfscCode())
                .bankAccountHolderName(vendorInfo.bankAccountHolderName())
                .razorpayFundAccountId(fundAccountId)
                .commissionRate(vendorInfo.commissionRate())
                .isActive(true)
                .build();
        
        VendorProfile savedProfile = vendorProfileRepository.save(vendorProfile);
        
        log.info("Created vendor profile for vendor {} with fund account {}", 
                vendorId, fundAccountId);
        
        return savedProfile;
    }
    
    /**
     * Get vendor fund account ID
     */
    @Transactional(readOnly = true)
    public String getVendorFundAccountId(UUID vendorId) {
        return vendorProfileRepository.findByVendorId(vendorId)
                .map(VendorProfile::getRazorpayFundAccountId)
                .orElse(null);
    }
    
    /**
     * Update vendor commission rate
     */
    @Transactional
    public VendorProfile updateCommissionRate(UUID vendorId, BigDecimal newCommissionRate) {
        VendorProfile vendorProfile = vendorProfileRepository.findByVendorId(vendorId)
                .orElseThrow(() -> new IllegalArgumentException("Vendor profile not found: " + vendorId));
        
        vendorProfile.setCommissionRate(newCommissionRate);
        
        return vendorProfileRepository.save(vendorProfile);
    }
    
    /**
     * Deactivate vendor
     */
    @Transactional
    public void deactivateVendor(UUID vendorId) {
        VendorProfile vendorProfile = vendorProfileRepository.findByVendorId(vendorId)
                .orElseThrow(() -> new IllegalArgumentException("Vendor profile not found: " + vendorId));
        
        vendorProfile.setIsActive(false);
        vendorProfileRepository.save(vendorProfile);
        
        log.info("Deactivated vendor profile for vendor {}", vendorId);
    }
    
    /**
     * Get all active vendors
     */
    @Transactional(readOnly = true)
    public List<VendorProfile> getActiveVendors() {
        return vendorProfileRepository.findByIsActiveTrueOrderByCreatedAtDesc();
    }
    
    /**
     * Calculate vendor payout amount
     */
    public VendorPayoutCalculation calculateVendorPayout(UUID vendorId, BigDecimal grossAmount) {
        VendorProfile vendorProfile = vendorProfileRepository.findByVendorId(vendorId)
                .orElseThrow(() -> new IllegalArgumentException("Vendor profile not found: " + vendorId));
        
        BigDecimal commissionRate = vendorProfile.getCommissionRate();
        BigDecimal commissionAmount = grossAmount.multiply(commissionRate)
                .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
        BigDecimal netAmount = grossAmount.subtract(commissionAmount);
        
        return new VendorPayoutCalculation(
                vendorId,
                grossAmount,
                commissionRate,
                commissionAmount,
                netAmount,
                null // No fund account ID needed - using bank account details directly
        );
    }
    
    /**
     * Fetch vendor information from User service
     */
    private UserServiceVendorInfo fetchVendorInfoFromUserService(UUID vendorId) {
        try {
            String url = userServiceBaseUrl + "/vendors/" + vendorId;
            UserServiceVendorInfo vendorInfo = restTemplate.getForObject(url, UserServiceVendorInfo.class);
            
            if (vendorInfo == null) {
                throw new IllegalArgumentException("Vendor not found in User service: " + vendorId);
            }
            
            return vendorInfo;
            
        } catch (Exception e) {
            log.error("Failed to fetch vendor info from User service for vendor {}", vendorId, e);
            throw new RuntimeException("Failed to fetch vendor information: " + e.getMessage(), e);
        }
    }
    
    /**
     * Create Razorpay fund account for vendor
     */
    private String createRazorpayFundAccount(UserServiceVendorInfo vendorInfo) throws RuntimeException {
            // Create contact first
            JSONObject contactRequest = new JSONObject();
            contactRequest.put("name", vendorInfo.name());
            contactRequest.put("email", vendorInfo.email());
            contactRequest.put("contact", vendorInfo.phone());
            contactRequest.put("type", "vendor");
            
            JSONObject contactNotes = new JSONObject();
            contactNotes.put("vendor_id", vendorInfo.vendorId().toString());
            contactRequest.put("notes", contactNotes);
            
            // This would use Razorpay Contacts API
            // For now, we'll simulate the contact creation
            String contactId = "cont_" + UUID.randomUUID().toString().replace("-", "").substring(0, 14);
            
            // No need to create fund account - we'll use bank account details directly in payouts
            log.info("Vendor profile will use bank account details directly for payouts: {} - {}",
                    vendorInfo.bankAccountNumber(), vendorInfo.bankIfscCode());

            // Return null since we don't need fund account IDs anymore
            return null;
    }
    
    /**
     * Create vendor profile from vendor info
     */
    @Transactional
    public VendorProfile createVendorProfile(VendorInfo vendorInfo) {
        // Check if vendor profile already exists
        Optional<VendorProfile> existingProfile = vendorProfileRepository.findByVendorId(vendorInfo.vendorId());
        if (existingProfile.isPresent()) {
            throw new IllegalArgumentException("Vendor profile already exists for vendor: " + vendorInfo.vendorId());
        }

        // No need for fund account - using bank account details directly
        log.info("Creating vendor profile with bank account details for direct payouts: {} - {}",
                vendorInfo.bankAccountNumber(), vendorInfo.bankIfscCode());

        VendorProfile vendorProfile = VendorProfile.builder()
                .vendorId(vendorInfo.vendorId())
                .vendorName(vendorInfo.vendorName())
                .vendorEmail(vendorInfo.vendorEmail())
                .vendorPhone(vendorInfo.vendorPhone())
                .bankAccountNumber(vendorInfo.bankAccountNumber())
                .bankIfscCode(vendorInfo.bankIfscCode())
                .bankAccountHolderName(vendorInfo.bankAccountHolderName())
                .razorpayFundAccountId(null) // Not needed - using bank account details directly
                .commissionRate(vendorInfo.commissionRate())
                .isActive(true)
                .isVerified(false)
                .build();

        VendorProfile savedProfile = vendorProfileRepository.save(vendorProfile);
        log.info("Created vendor profile for vendor {}", vendorInfo.vendorId());

        return savedProfile;
    }

    /**
     * Get vendor profile by vendor ID
     */
    @Transactional(readOnly = true)
    public VendorProfile getVendorProfile(UUID vendorId) {
        return vendorProfileRepository.findByVendorId(vendorId)
                .orElseThrow(() -> new IllegalArgumentException("Vendor profile not found: " + vendorId));
    }

    /**
     * Vendor information for profile creation
     */
    public record VendorInfo(
            UUID vendorId,
            String vendorName,
            String vendorEmail,
            String vendorPhone,
            String bankAccountNumber,
            String bankIfscCode,
            String bankAccountHolderName,
            BigDecimal commissionRate
    ) {}

    /**
     * Vendor payout calculation result
     */
    public record VendorPayoutCalculation(
            UUID vendorId,
            BigDecimal grossAmount,
            BigDecimal commissionRate,
            BigDecimal commissionAmount,
            BigDecimal netAmount,
            String fundAccountId
    ) {}
}
