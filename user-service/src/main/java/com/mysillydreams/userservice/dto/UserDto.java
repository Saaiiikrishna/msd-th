package com.mysillydreams.userservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * Data Transfer Object for User information.
 * Supports field masking based on user roles and permissions.
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "User information with role-based field masking")
public class UserDto {

    @Schema(description = "User's unique reference ID", example = "01234567-89ab-cdef-0123-456789abcdef", accessMode = Schema.AccessMode.READ_ONLY)
    private String referenceId;

    @Schema(description = "User's first name", example = "John")
    @Size(max = 100, message = "First name cannot exceed 100 characters")
    private String firstName;

    @Schema(description = "User's last name", example = "Doe")
    @Size(max = 100, message = "Last name cannot exceed 100 characters")
    private String lastName;

    @Schema(description = "User's email address", example = "john.doe@example.com")
    @Email(message = "Invalid email format")
    @NotBlank(message = "Email is required")
    @Size(max = 255, message = "Email cannot exceed 255 characters")
    private String email;

    @Schema(description = "User's phone number", example = "+919876543210")
    @Pattern(regexp = "^[+]?[0-9\\s\\-\\(\\)]{7,15}$", message = "Invalid phone number format")
    private String phone;

    @Schema(description = "User's date of birth", example = "1990-01-01")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "Date of birth must be in YYYY-MM-DD format")
    private String dob;

    @Schema(description = "User's gender", example = "MALE", allowableValues = {"MALE", "FEMALE", "OTHER", "PREFER_NOT_TO_SAY"})
    private String gender;

    @Schema(description = "URL to user's avatar image", example = "https://example.com/avatar.jpg")
    @Size(max = 1000, message = "Avatar URL cannot exceed 1000 characters")
    private String avatarUrl;

    @Schema(description = "Whether the user account is active", example = "true", accessMode = Schema.AccessMode.READ_ONLY)
    private Boolean active;

    @Schema(description = "User's assigned roles", example = "[\"ROLE_CUSTOMER\", \"ROLE_VENDOR\"]", accessMode = Schema.AccessMode.READ_ONLY)
    private Set<String> roles;

    @Schema(description = "User's addresses", accessMode = Schema.AccessMode.READ_ONLY)
    private List<AddressDto> addresses;

    @Schema(description = "User's consent preferences", accessMode = Schema.AccessMode.READ_ONLY)
    private List<ConsentDto> consents;

    @Schema(description = "When the user account was created", example = "2024-01-01T12:00:00", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime createdAt;

    @Schema(description = "When the user account was last updated", example = "2024-01-01T12:00:00", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime updatedAt;

    @Schema(description = "When the user account was deleted (if soft deleted)", example = "2024-01-01T12:00:00", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime deletedAt;

    // Computed fields for API responses
    @Schema(description = "User's full name", example = "John Doe", accessMode = Schema.AccessMode.READ_ONLY)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public String getFullName() {
        if (firstName == null && lastName == null) {
            return null;
        }
        if (firstName == null) {
            return lastName;
        }
        if (lastName == null) {
            return firstName;
        }
        return firstName + " " + lastName;
    }

    @Schema(description = "User's display name (masked for privacy)", example = "J*** D***", accessMode = Schema.AccessMode.READ_ONLY)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public String getDisplayName() {
        String fullName = getFullName();
        if (fullName == null || fullName.trim().isEmpty()) {
            return "User";
        }

        // Mask the name for privacy
        String[] parts = fullName.split(" ");
        StringBuilder masked = new StringBuilder();

        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                masked.append(" ");
            }
            String part = parts[i];
            if (part.length() <= 1) {
                masked.append(part);
            } else {
                masked.append(part.charAt(0)).append("***");
            }
        }

        return masked.toString();
    }

    @Schema(description = "Whether user has any addresses", accessMode = Schema.AccessMode.READ_ONLY)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public Boolean getHasAddresses() {
        return addresses != null && !addresses.isEmpty();
    }

    @Schema(description = "Primary address if available", accessMode = Schema.AccessMode.READ_ONLY)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public AddressDto getPrimaryAddress() {
        if (addresses == null) {
            return null;
        }
        return addresses.stream()
            .filter(addr -> addr.getIsPrimary() != null && addr.getIsPrimary())
            .findFirst()
            .orElse(null);
    }

    @Schema(description = "Whether user has granted marketing consents", accessMode = Schema.AccessMode.READ_ONLY)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public Boolean getHasMarketingConsents() {
        if (consents == null) {
            return false;
        }
        return consents.stream()
            .anyMatch(consent ->
                (consent.getConsentKey().contains("marketing") ||
                 consent.getConsentKey().contains("promotional")) &&
                consent.getGranted() != null && consent.getGranted()
            );
    }

    @Schema(description = "User account status summary", accessMode = Schema.AccessMode.READ_ONLY)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public String getAccountStatus() {
        if (deletedAt != null) {
            return "DELETED";
        }
        if (active == null || !active) {
            return "INACTIVE";
        }
        return "ACTIVE";
    }

    // Factory method to create UserDto from UserEntity
    public static UserDto from(com.mysillydreams.userservice.domain.UserEntity entity) {
        if (entity == null) {
            return null;
        }

        UserDto dto = new UserDto();
        dto.setReferenceId(entity.getReferenceId());
        dto.setFirstName(entity.getFirstName());
        dto.setLastName(entity.getLastName());
        dto.setEmail(entity.getEmail());
        dto.setPhone(entity.getPhone());
        dto.setDob(entity.getDob());
        dto.setGender(entity.getGender() != null ? entity.getGender().name() : null);
        dto.setAvatarUrl(entity.getAvatarUrl());
        dto.setActive(entity.getActive());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        dto.setDeletedAt(entity.getDeletedAt());

        // Note: This is a basic conversion. For production, use UserMapper for proper
        // PII decryption and role-based masking
        return dto;
    }

    // Validation groups for different operations
    public interface CreateValidation {}
    public interface UpdateValidation {}
    public interface AdminValidation {}
}
