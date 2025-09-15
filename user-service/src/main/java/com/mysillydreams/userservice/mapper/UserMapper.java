package com.mysillydreams.userservice.mapper;

import com.mysillydreams.userservice.domain.UserEntity;
import com.mysillydreams.userservice.domain.UserRoleEntity;
import com.mysillydreams.userservice.dto.UserDto;
import com.mysillydreams.userservice.encryption.PiiMapper;
import com.mysillydreams.userservice.security.RoleHierarchyConfig;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Set;

/**
 * MapStruct mapper for User entities and DTOs with PII encryption/decryption support.
 * Handles field-level encryption and role-based data masking.
 */
@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public abstract class UserMapper {

    @Autowired
    protected PiiMapper piiMapper;

    /**
     * Maps UserDto to UserEntity for creation (encrypts PII fields)
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "active", constant = "true")
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "firstNameEnc", source = "firstName", qualifiedByName = "encryptPii")
    @Mapping(target = "lastNameEnc", source = "lastName", qualifiedByName = "encryptPii")
    @Mapping(target = "emailEnc", source = "email", qualifiedByName = "encryptEmail")
    @Mapping(target = "phoneEnc", source = "phone", qualifiedByName = "encryptPhone")
    @Mapping(target = "dobEnc", source = "dob", qualifiedByName = "encryptPii")
    @Mapping(target = "emailHmac", source = "email", qualifiedByName = "generateEmailHmac")
    @Mapping(target = "phoneHmac", source = "phone", qualifiedByName = "generatePhoneHmac")
    @Mapping(target = "roles", ignore = true) // Roles are managed separately
    @Mapping(target = "addresses", ignore = true) // Addresses are managed separately
    @Mapping(target = "consents", ignore = true) // Consents are managed separately
    @Mapping(target = "sessions", ignore = true) // Sessions are managed separately
    @Mapping(target = "auditRecords", ignore = true) // Audit records are managed separately
    public abstract UserEntity toEntity(UserDto dto);

    /**
     * Maps UserCreateRequestDto to UserDto for service layer
     */
    @Mapping(target = "referenceId", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "addresses", ignore = true)
    @Mapping(target = "consents", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    public abstract UserDto fromCreateRequest(com.mysillydreams.userservice.dto.UserCreateRequestDto request);

    /**
     * Maps UserEntity to UserDto for response (decrypts and masks PII fields based on role)
     */
    @Mapping(target = "firstName", source = "firstNameEnc", qualifiedByName = "decryptAndMaskPii")
    @Mapping(target = "lastName", source = "lastNameEnc", qualifiedByName = "decryptAndMaskPii")
    @Mapping(target = "email", source = "emailEnc", qualifiedByName = "decryptAndMaskEmail")
    @Mapping(target = "phone", source = "phoneEnc", qualifiedByName = "decryptAndMaskPhone")
    @Mapping(target = "dob", source = "dobEnc", qualifiedByName = "decryptAndMaskPii")
    @Mapping(target = "roles", source = "roles", qualifiedByName = "mapRolesToStrings")
    public abstract UserDto toDto(UserEntity entity);

    /**
     * Maps UserEntity to UserDto with full data (for admin users)
     */
    @Mapping(target = "firstName", source = "firstNameEnc", qualifiedByName = "decryptPii")
    @Mapping(target = "lastName", source = "lastNameEnc", qualifiedByName = "decryptPii")
    @Mapping(target = "email", source = "emailEnc", qualifiedByName = "decryptPii")
    @Mapping(target = "phone", source = "phoneEnc", qualifiedByName = "decryptPii")
    @Mapping(target = "dob", source = "dobEnc", qualifiedByName = "decryptPii")
    @Mapping(target = "roles", source = "roles", qualifiedByName = "mapRolesToStrings")
    public abstract UserDto toDtoFull(UserEntity entity);

    /**
     * Maps UserEntity to minimal UserDto (for internal services)
     */
    @Mapping(target = "firstName", source = "firstNameEnc", qualifiedByName = "decryptPii")
    @Mapping(target = "lastName", source = "lastNameEnc", qualifiedByName = "decryptPii")
    @Mapping(target = "email", source = "emailEnc", qualifiedByName = "maskEmailOnly")
    @Mapping(target = "phone", source = "phoneEnc", qualifiedByName = "maskPhoneOnly")
    @Mapping(target = "dob", ignore = true) // Don't include DOB in minimal response
    @Mapping(target = "gender", ignore = true)
    @Mapping(target = "roles", source = "roles", qualifiedByName = "mapRolesToStrings")
    public abstract UserDto toDtoMinimal(UserEntity entity);

    /**
     * Updates existing UserEntity from UserDto (for updates)
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "referenceId", ignore = true) // Reference ID cannot be changed
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true) // Will be set by JPA
    @Mapping(target = "active", ignore = true) // Active status managed separately
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "firstNameEnc", source = "firstName", qualifiedByName = "encryptPiiIfPresent")
    @Mapping(target = "lastNameEnc", source = "lastName", qualifiedByName = "encryptPiiIfPresent")
    @Mapping(target = "emailEnc", source = "email", qualifiedByName = "encryptEmailIfPresent")
    @Mapping(target = "phoneEnc", source = "phone", qualifiedByName = "encryptPhoneIfPresent")
    @Mapping(target = "dobEnc", source = "dob", qualifiedByName = "encryptPiiIfPresent")
    @Mapping(target = "emailHmac", source = "email", qualifiedByName = "generateEmailHmacIfPresent")
    @Mapping(target = "phoneHmac", source = "phone", qualifiedByName = "generatePhoneHmacIfPresent")
    @Mapping(target = "roles", ignore = true) // Roles are managed separately
    @Mapping(target = "addresses", ignore = true)
    @Mapping(target = "consents", ignore = true)
    @Mapping(target = "sessions", ignore = true)
    @Mapping(target = "auditRecords", ignore = true)
    public abstract void updateEntityFromDto(UserDto dto, @MappingTarget UserEntity entity);

    // Custom mapping methods for PII encryption/decryption

    @Named("encryptPii")
    protected String encryptPii(String plaintext) {
        return piiMapper.processPiiForStorage(plaintext);
    }

    @Named("encryptPiiIfPresent")
    protected String encryptPiiIfPresent(String plaintext) {
        return plaintext != null ? piiMapper.processPiiForStorage(plaintext) : null;
    }

    @Named("encryptEmail")
    protected String encryptEmail(String email) {
        if (email == null) return null;
        PiiMapper.PiiData emailData = piiMapper.processEmailForStorage(email);
        return emailData.getEncryptedValue();
    }

    @Named("encryptEmailIfPresent")
    protected String encryptEmailIfPresent(String email) {
        return email != null ? encryptEmail(email) : null;
    }

    @Named("encryptPhone")
    protected String encryptPhone(String phone) {
        if (phone == null) return null;
        PiiMapper.PiiData phoneData = piiMapper.processPhoneForStorage(phone);
        return phoneData.getEncryptedValue();
    }

    @Named("encryptPhoneIfPresent")
    protected String encryptPhoneIfPresent(String phone) {
        return phone != null ? encryptPhone(phone) : null;
    }

    @Named("generateEmailHmac")
    protected String generateEmailHmac(String email) {
        if (email == null) return null;
        PiiMapper.PiiData emailData = piiMapper.processEmailForStorage(email);
        return emailData.getHmacValue();
    }

    @Named("generateEmailHmacIfPresent")
    protected String generateEmailHmacIfPresent(String email) {
        return email != null ? generateEmailHmac(email) : null;
    }

    @Named("generatePhoneHmac")
    protected String generatePhoneHmac(String phone) {
        if (phone == null) return null;
        PiiMapper.PiiData phoneData = piiMapper.processPhoneForStorage(phone);
        return phoneData.getHmacValue();
    }

    @Named("generatePhoneHmacIfPresent")
    protected String generatePhoneHmacIfPresent(String phone) {
        return phone != null ? generatePhoneHmac(phone) : null;
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

    @Named("decryptAndMaskEmail")
    protected String decryptAndMaskEmail(String encryptedEmail) {
        if (isAdminUser()) {
            return piiMapper.decryptPii(encryptedEmail);
        } else {
            String decrypted = piiMapper.decryptPii(encryptedEmail);
            return piiMapper.maskEmail(decrypted);
        }
    }

    @Named("decryptAndMaskPhone")
    protected String decryptAndMaskPhone(String encryptedPhone) {
        if (isAdminUser()) {
            return piiMapper.decryptPii(encryptedPhone);
        } else {
            String decrypted = piiMapper.decryptPii(encryptedPhone);
            return piiMapper.maskPhone(decrypted);
        }
    }

    @Named("maskEmailOnly")
    protected String maskEmailOnly(String encryptedEmail) {
        String decrypted = piiMapper.decryptPii(encryptedEmail);
        return piiMapper.maskEmail(decrypted);
    }

    @Named("maskPhoneOnly")
    protected String maskPhoneOnly(String encryptedPhone) {
        String decrypted = piiMapper.decryptPii(encryptedPhone);
        return piiMapper.maskPhone(decrypted);
    }

    /**
     * Checks if the current user has admin privileges for full data access
     */
    private boolean isAdminUser() {
        return RoleHierarchyConfig.SecurityUtils.isAdmin() || 
               RoleHierarchyConfig.SecurityUtils.isSupportOrAdmin();
    }

    /**
     * Creates a lookup DTO for HMAC-based searches
     */
    public UserLookupDto createLookupDto(String email, String phone) {
        UserLookupDto lookupDto = new UserLookupDto();
        
        if (email != null) {
            lookupDto.setEmailHmac(piiMapper.generateEmailLookupHmac(email));
        }
        
        if (phone != null) {
            lookupDto.setPhoneHmac(piiMapper.generatePhoneLookupHmac(phone));
        }
        
        return lookupDto;
    }

    /**
     * Maps List<UserRoleEntity> to Set<String>
     */
    @Named("mapRolesToStrings")
    protected Set<String> mapRolesToStrings(List<UserRoleEntity> roles) {
        if (roles == null) return null;
        return roles.stream()
                .map(UserRoleEntity::getRole)
                .collect(java.util.stream.Collectors.toSet());
    }

    /**
     * Maps InetAddress to String
     */
    protected String map(java.net.InetAddress value) {
        return value != null ? value.getHostAddress() : null;
    }

    /**
     * DTO for HMAC-based user lookups
     */
    public static class UserLookupDto {
        private String emailHmac;
        private String phoneHmac;

        // Getters and setters
        public String getEmailHmac() { return emailHmac; }
        public void setEmailHmac(String emailHmac) { this.emailHmac = emailHmac; }
        public String getPhoneHmac() { return phoneHmac; }
        public void setPhoneHmac(String phoneHmac) { this.phoneHmac = phoneHmac; }
    }
}
