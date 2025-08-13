package com.mysillydreams.payment.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Vendor information from User service
 */
public record UserServiceVendorInfo(
        UUID vendorId,
        String name,
        String email,
        String phone,
        String bankAccountNumber,
        String bankIfscCode,
        String bankAccountHolderName,
        String bankName,
        BigDecimal commissionRate,
        boolean isActive,
        boolean isVerified
) {}
