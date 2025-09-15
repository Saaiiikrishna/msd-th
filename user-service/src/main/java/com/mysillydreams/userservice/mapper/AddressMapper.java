package com.mysillydreams.userservice.mapper;

import com.mysillydreams.userservice.domain.AddressEntity;
import com.mysillydreams.userservice.dto.AddressDto;
import com.mysillydreams.userservice.encryption.PiiMapper;
import com.mysillydreams.userservice.security.RoleHierarchyConfig;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * MapStruct mapper for Address entities and DTOs with PII encryption support.
 * Handles field-level encryption for address data and role-based masking.
 */
@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public abstract class AddressMapper {

    @Autowired
    protected PiiMapper piiMapper;

    /**
     * Maps AddressDto to AddressEntity for creation (encrypts PII fields)
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true) // User relationship set separately
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "line1Enc", source = "line1", qualifiedByName = "encryptPii")
    @Mapping(target = "line2Enc", source = "line2", qualifiedByName = "encryptPii")
    @Mapping(target = "cityEnc", source = "city", qualifiedByName = "encryptPii")
    @Mapping(target = "stateEnc", source = "state", qualifiedByName = "encryptPii")
    @Mapping(target = "postalCodeEnc", source = "postalCode", qualifiedByName = "encryptPii")
    @Mapping(target = "countryEnc", source = "country", qualifiedByName = "encryptPii")
    public abstract AddressEntity toEntity(AddressDto dto);

    /**
     * Maps AddressEntity to AddressDto for response (decrypts and masks based on role)
     */
    @Mapping(target = "line1", source = "line1Enc", qualifiedByName = "decryptAndMaskAddress")
    @Mapping(target = "line2", source = "line2Enc", qualifiedByName = "decryptAndMaskAddress")
    @Mapping(target = "city", source = "cityEnc", qualifiedByName = "decryptAndMaskPii")
    @Mapping(target = "state", source = "stateEnc", qualifiedByName = "decryptAndMaskPii")
    @Mapping(target = "postalCode", source = "postalCodeEnc", qualifiedByName = "decryptAndMaskPostalCode")
    @Mapping(target = "country", source = "countryEnc", qualifiedByName = "decryptPii")
    public abstract AddressDto toDto(AddressEntity entity);

    /**
     * Maps AddressEntity to AddressDto with full data (for admin users)
     */
    @Mapping(target = "line1", source = "line1Enc", qualifiedByName = "decryptPii")
    @Mapping(target = "line2", source = "line2Enc", qualifiedByName = "decryptPii")
    @Mapping(target = "city", source = "cityEnc", qualifiedByName = "decryptPii")
    @Mapping(target = "state", source = "stateEnc", qualifiedByName = "decryptPii")
    @Mapping(target = "postalCode", source = "postalCodeEnc", qualifiedByName = "decryptPii")
    @Mapping(target = "country", source = "countryEnc", qualifiedByName = "decryptPii")
    public abstract AddressDto toDtoFull(AddressEntity entity);

    /**
     * Maps AddressEntity to minimal AddressDto (for internal services)
     */
    @Mapping(target = "line1", ignore = true) // Don't include detailed address
    @Mapping(target = "line2", ignore = true)
    @Mapping(target = "city", source = "cityEnc", qualifiedByName = "decryptPii")
    @Mapping(target = "state", source = "stateEnc", qualifiedByName = "decryptPii")
    @Mapping(target = "postalCode", source = "postalCodeEnc", qualifiedByName = "maskPostalCodeOnly")
    @Mapping(target = "country", source = "countryEnc", qualifiedByName = "decryptPii")
    public abstract AddressDto toDtoMinimal(AddressEntity entity);

    /**
     * Updates existing AddressEntity from AddressDto (for updates)
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true) // User relationship cannot be changed
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true) // Will be set by JPA
    @Mapping(target = "line1Enc", source = "line1", qualifiedByName = "encryptPiiIfPresent")
    @Mapping(target = "line2Enc", source = "line2", qualifiedByName = "encryptPiiIfPresent")
    @Mapping(target = "cityEnc", source = "city", qualifiedByName = "encryptPiiIfPresent")
    @Mapping(target = "stateEnc", source = "state", qualifiedByName = "encryptPiiIfPresent")
    @Mapping(target = "postalCodeEnc", source = "postalCode", qualifiedByName = "encryptPiiIfPresent")
    @Mapping(target = "countryEnc", source = "country", qualifiedByName = "encryptPiiIfPresent")
    public abstract void updateEntityFromDto(AddressDto dto, @MappingTarget AddressEntity entity);

    // Custom mapping methods for PII encryption/decryption

    @Named("encryptPii")
    protected String encryptPii(String plaintext) {
        return piiMapper.processPiiForStorage(plaintext);
    }

    @Named("encryptPiiIfPresent")
    protected String encryptPiiIfPresent(String plaintext) {
        return plaintext != null ? piiMapper.processPiiForStorage(plaintext) : null;
    }

    @Named("decryptPii")
    protected String decryptPii(String encryptedData) {
        return piiMapper.decryptPii(encryptedData);
    }

    @Named("decryptAndMaskPii")
    protected String decryptAndMaskPii(String encryptedData) {
        if (isAdminUser()) {
            return piiMapper.decryptPii(encryptedData);
        } else {
            String decrypted = piiMapper.decryptPii(encryptedData);
            return piiMapper.maskPii(decrypted);
        }
    }

    @Named("decryptAndMaskAddress")
    protected String decryptAndMaskAddress(String encryptedAddress) {
        if (isAdminUser()) {
            return piiMapper.decryptPii(encryptedAddress);
        } else {
            String decrypted = piiMapper.decryptPii(encryptedAddress);
            return maskAddressLine(decrypted);
        }
    }

    @Named("decryptAndMaskPostalCode")
    protected String decryptAndMaskPostalCode(String encryptedPostalCode) {
        if (isAdminUser()) {
            return piiMapper.decryptPii(encryptedPostalCode);
        } else {
            String decrypted = piiMapper.decryptPii(encryptedPostalCode);
            return maskPostalCode(decrypted);
        }
    }

    @Named("maskPostalCodeOnly")
    protected String maskPostalCodeOnly(String encryptedPostalCode) {
        String decrypted = piiMapper.decryptPii(encryptedPostalCode);
        return maskPostalCode(decrypted);
    }

    /**
     * Masks address line (shows first few characters)
     */
    private String maskAddressLine(String addressLine) {
        if (addressLine == null || addressLine.trim().isEmpty()) {
            return null;
        }

        try {
            String trimmed = addressLine.trim();
            if (trimmed.length() <= 3) {
                return "*".repeat(trimmed.length());
            } else if (trimmed.length() <= 10) {
                return trimmed.substring(0, 2) + "*".repeat(trimmed.length() - 2);
            } else {
                return trimmed.substring(0, 3) + "*".repeat(Math.min(10, trimmed.length() - 3));
            }
        } catch (Exception e) {
            return "***";
        }
    }

    /**
     * Masks postal code (shows first 2 digits for Indian postal codes)
     */
    private String maskPostalCode(String postalCode) {
        if (postalCode == null || postalCode.trim().isEmpty()) {
            return null;
        }

        try {
            String trimmed = postalCode.trim();
            if (trimmed.length() <= 2) {
                return "*".repeat(trimmed.length());
            } else if (trimmed.length() == 6) { // Indian postal code
                return trimmed.substring(0, 2) + "****";
            } else if (trimmed.length() == 5) { // US ZIP code
                return trimmed.substring(0, 2) + "***";
            } else {
                return trimmed.substring(0, 2) + "*".repeat(Math.min(4, trimmed.length() - 2));
            }
        } catch (Exception e) {
            return "***";
        }
    }

    /**
     * Checks if the current user has admin privileges for full data access
     */
    private boolean isAdminUser() {
        return RoleHierarchyConfig.SecurityUtils.isAdmin() || 
               RoleHierarchyConfig.SecurityUtils.isSupportOrAdmin();
    }

    /**
     * Creates a summary of addresses for user profile
     */
    public AddressSummaryDto createAddressSummary(AddressEntity entity) {
        if (entity == null) {
            return null;
        }

        AddressSummaryDto summary = new AddressSummaryDto();
        summary.setId(entity.getId());
        summary.setType(entity.getType().name());
        summary.setIsPrimary(entity.getIsPrimary());
        
        // Only show city, state, country in summary
        summary.setCity(piiMapper.decryptPii(entity.getCityEnc()));
        summary.setState(piiMapper.decryptPii(entity.getStateEnc()));
        summary.setCountry(piiMapper.decryptPii(entity.getCountryEnc()));
        
        return summary;
    }

    /**
     * DTO for address summary (used in user profiles)
     */
    public static class AddressSummaryDto {
        private java.util.UUID id;
        private String type;
        private Boolean isPrimary;
        private String city;
        private String state;
        private String country;

        // Getters and setters
        public java.util.UUID getId() { return id; }
        public void setId(java.util.UUID id) { this.id = id; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public Boolean getIsPrimary() { return isPrimary; }
        public void setIsPrimary(Boolean isPrimary) { this.isPrimary = isPrimary; }
        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }
        public String getState() { return state; }
        public void setState(String state) { this.state = state; }
        public String getCountry() { return country; }
        public void setCountry(String country) { this.country = country; }
    }
}
