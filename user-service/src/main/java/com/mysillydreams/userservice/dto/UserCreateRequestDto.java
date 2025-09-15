package com.mysillydreams.userservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * Data Transfer Object for user creation requests.
 * Contains validation rules for new user registration.
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Request payload for creating a new user")
public class UserCreateRequestDto {

    @Schema(description = "User's reference ID (optional, will be generated if not provided)", example = "01234567-89ab-cdef-0123-456789abcdef")
    @Size(max = 36, message = "Reference ID cannot exceed 36 characters")
    private String referenceId;

    @Schema(description = "User's first name", example = "John", required = true)
    @NotBlank(message = "First name is required")
    @Size(min = 1, max = 100, message = "First name must be between 1 and 100 characters")
    private String firstName;

    @Schema(description = "User's last name", example = "Doe", required = true)
    @NotBlank(message = "Last name is required")
    @Size(min = 1, max = 100, message = "Last name must be between 1 and 100 characters")
    private String lastName;

    @Schema(description = "User's email address", example = "john.doe@example.com", required = true)
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(max = 255, message = "Email cannot exceed 255 characters")
    private String email;

    @Schema(description = "User's phone number", example = "+919876543210")
    @Pattern(regexp = "^[+]?[0-9\\s\\-\\(\\)]{7,15}$", message = "Invalid phone number format")
    private String phone;

    @Schema(description = "User's date of birth", example = "1990-01-01")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "Date of birth must be in YYYY-MM-DD format")
    private String dob;

    @Schema(description = "User's gender", example = "MALE", allowableValues = {"MALE", "FEMALE", "OTHER", "PREFER_NOT_TO_SAY"})
    @Pattern(regexp = "^(MALE|FEMALE|OTHER|PREFER_NOT_TO_SAY)$", message = "Invalid gender value")
    private String gender;

    @Schema(description = "URL to user's avatar image", example = "https://example.com/avatar.jpg")
    @Size(max = 1000, message = "Avatar URL cannot exceed 1000 characters")
    private String avatarUrl;

    @Schema(description = "User's initial address (optional)")
    @Valid
    private AddressCreateRequestDto address;

    @Schema(description = "Initial consent preferences")
    @Valid
    private List<ConsentRequestDto> consents;

    @Schema(description = "Marketing preferences")
    private MarketingPreferencesDto marketingPreferences;

    // Nested DTOs for related data
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Address creation request")
    public static class AddressCreateRequestDto {

        @Schema(description = "Address type", example = "HOME", allowableValues = {"HOME", "WORK", "BILLING", "SHIPPING", "OTHER"})
        @NotBlank(message = "Address type is required")
        private String type;

        @Schema(description = "Address line 1", example = "123 Main Street", required = true)
        @NotBlank(message = "Address line 1 is required")
        @Size(max = 255, message = "Address line 1 cannot exceed 255 characters")
        private String line1;

        @Schema(description = "Address line 2", example = "Apartment 4B")
        @Size(max = 255, message = "Address line 2 cannot exceed 255 characters")
        private String line2;

        @Schema(description = "City", example = "New York", required = true)
        @NotBlank(message = "City is required")
        @Size(max = 100, message = "City cannot exceed 100 characters")
        private String city;

        @Schema(description = "State or Province", example = "NY", required = true)
        @NotBlank(message = "State is required")
        @Size(max = 100, message = "State cannot exceed 100 characters")
        private String state;

        @Schema(description = "Postal code", example = "10001", required = true)
        @NotBlank(message = "Postal code is required")
        @Size(max = 20, message = "Postal code cannot exceed 20 characters")
        private String postalCode;

        @Schema(description = "Country", example = "United States", required = true)
        @NotBlank(message = "Country is required")
        @Size(max = 100, message = "Country cannot exceed 100 characters")
        private String country;

        @Schema(description = "Whether this should be the primary address", example = "true")
        private Boolean isPrimary = true;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Consent request")
    public static class ConsentRequestDto {

        @Schema(description = "Consent key", example = "marketing_emails", required = true)
        @NotBlank(message = "Consent key is required")
        private String consentKey;

        @Schema(description = "Whether consent is granted", example = "true", required = true)
        private Boolean granted;

        @Schema(description = "Consent version", example = "v1.0")
        private String consentVersion;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Marketing preferences")
    public static class MarketingPreferencesDto {

        @Schema(description = "Accept marketing emails", example = "true")
        private Boolean emailMarketing;

        @Schema(description = "Accept marketing SMS", example = "false")
        private Boolean smsMarketing;

        @Schema(description = "Accept push notifications", example = "true")
        private Boolean pushNotifications;

        @Schema(description = "Accept phone calls", example = "false")
        private Boolean phoneMarketing;

        @Schema(description = "Preferred communication frequency", example = "WEEKLY", allowableValues = {"DAILY", "WEEKLY", "MONTHLY", "NEVER"})
        private String frequency;

        @Schema(description = "Marketing categories of interest")
        private List<String> interests;
    }

    // Validation groups
    public interface BasicValidation {}
    public interface FullValidation {}

    // Helper methods
    public boolean hasAddress() {
        return address != null;
    }

    public boolean hasConsents() {
        return consents != null && !consents.isEmpty();
    }

    public boolean hasMarketingPreferences() {
        return marketingPreferences != null;
    }

    // Convert to UserDto for service layer
    public UserDto toUserDto() {
        UserDto userDto = new UserDto();
        userDto.setReferenceId(this.referenceId);
        userDto.setFirstName(this.firstName);
        userDto.setLastName(this.lastName);
        userDto.setEmail(this.email);
        userDto.setPhone(this.phone);
        userDto.setDob(this.dob);
        userDto.setGender(this.gender);
        userDto.setAvatarUrl(this.avatarUrl);
        return userDto;
    }

    // Convert address to AddressDto
    public AddressDto toAddressDto() {
        if (address == null) {
            return null;
        }

        AddressDto addressDto = new AddressDto();
        addressDto.setType(address.getType());
        addressDto.setLine1(address.getLine1());
        addressDto.setLine2(address.getLine2());
        addressDto.setCity(address.getCity());
        addressDto.setState(address.getState());
        addressDto.setPostalCode(address.getPostalCode());
        addressDto.setCountry(address.getCountry());
        addressDto.setIsPrimary(address.getIsPrimary());
        return addressDto;
    }

    // Convert consents to ConsentDto list
    public List<ConsentDto> toConsentDtos() {
        if (consents == null) {
            return null;
        }

        return consents.stream()
            .map(consent -> {
                ConsentDto consentDto = new ConsentDto();
                consentDto.setConsentKey(consent.getConsentKey());
                consentDto.setGranted(consent.getGranted());
                consentDto.setConsentVersion(consent.getConsentVersion());
                consentDto.setSource("WEB"); // Default source
                consentDto.setLegalBasis("CONSENT"); // Default legal basis
                return consentDto;
            })
            .toList();
    }
}
