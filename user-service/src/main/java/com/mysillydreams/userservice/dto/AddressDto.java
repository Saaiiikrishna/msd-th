package com.mysillydreams.userservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Data Transfer Object for Address information.
 * Contains encrypted address fields with role-based masking.
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "User address information with privacy masking")
public class AddressDto {

    @Schema(description = "Address unique identifier", example = "123e4567-e89b-12d3-a456-426614174000", accessMode = Schema.AccessMode.READ_ONLY)
    private UUID id;

    @Schema(description = "Address type", example = "HOME", allowableValues = {"HOME", "WORK", "BILLING", "SHIPPING", "OTHER"})
    @NotNull(message = "Address type is required")
    private String type;

    @Schema(description = "Address line 1", example = "123 Main Street")
    @NotBlank(message = "Address line 1 is required")
    @Size(max = 255, message = "Address line 1 cannot exceed 255 characters")
    private String line1;

    @Schema(description = "Address line 2", example = "Apartment 4B")
    @Size(max = 255, message = "Address line 2 cannot exceed 255 characters")
    private String line2;

    @Schema(description = "City", example = "New York")
    @NotBlank(message = "City is required")
    @Size(max = 100, message = "City cannot exceed 100 characters")
    private String city;

    @Schema(description = "State or Province", example = "NY")
    @NotBlank(message = "State is required")
    @Size(max = 100, message = "State cannot exceed 100 characters")
    private String state;

    @Schema(description = "Postal code", example = "10001")
    @NotBlank(message = "Postal code is required")
    @Size(max = 20, message = "Postal code cannot exceed 20 characters")
    private String postalCode;

    @Schema(description = "Country", example = "United States")
    @NotBlank(message = "Country is required")
    @Size(max = 100, message = "Country cannot exceed 100 characters")
    private String country;

    @Schema(description = "Whether this is the primary address", example = "true")
    private Boolean isPrimary;

    @Schema(description = "When the address was created", example = "2024-01-01T12:00:00", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime createdAt;

    @Schema(description = "When the address was last updated", example = "2024-01-01T12:00:00", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime updatedAt;

    // Computed fields for API responses
    @Schema(description = "Full address as single line", example = "123 Main Street, Apartment 4B, New York, NY 10001, United States", accessMode = Schema.AccessMode.READ_ONLY)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public String getFullAddress() {
        StringBuilder address = new StringBuilder();
        
        if (line1 != null && !line1.trim().isEmpty()) {
            address.append(line1);
        }
        
        if (line2 != null && !line2.trim().isEmpty()) {
            if (address.length() > 0) {
                address.append(", ");
            }
            address.append(line2);
        }
        
        if (city != null && !city.trim().isEmpty()) {
            if (address.length() > 0) {
                address.append(", ");
            }
            address.append(city);
        }
        
        if (state != null && !state.trim().isEmpty()) {
            if (address.length() > 0) {
                address.append(", ");
            }
            address.append(state);
        }
        
        if (postalCode != null && !postalCode.trim().isEmpty()) {
            if (address.length() > 0) {
                address.append(" ");
            }
            address.append(postalCode);
        }
        
        if (country != null && !country.trim().isEmpty()) {
            if (address.length() > 0) {
                address.append(", ");
            }
            address.append(country);
        }
        
        return address.toString();
    }

    // Manual getter for isPrimary since Lombok isn't working properly
    public Boolean getIsPrimary() {
        return isPrimary;
    }

    @Schema(description = "Address summary for display", example = "Home Address (Primary)", accessMode = Schema.AccessMode.READ_ONLY)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public String getAddressSummary() {
        StringBuilder summary = new StringBuilder();
        
        if (type != null) {
            switch (type.toUpperCase()) {
                case "HOME":
                    summary.append("Home Address");
                    break;
                case "WORK":
                    summary.append("Work Address");
                    break;
                case "BILLING":
                    summary.append("Billing Address");
                    break;
                case "SHIPPING":
                    summary.append("Shipping Address");
                    break;
                default:
                    summary.append("Other Address");
                    break;
            }
        }
        
        if (isPrimary != null && isPrimary) {
            summary.append(" (Primary)");
        }
        
        return summary.toString();
    }

    @Schema(description = "Short address for display", example = "New York, NY", accessMode = Schema.AccessMode.READ_ONLY)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public String getShortAddress() {
        StringBuilder shortAddr = new StringBuilder();
        
        if (city != null && !city.trim().isEmpty()) {
            shortAddr.append(city);
        }
        
        if (state != null && !state.trim().isEmpty()) {
            if (shortAddr.length() > 0) {
                shortAddr.append(", ");
            }
            shortAddr.append(state);
        }
        
        return shortAddr.toString();
    }

    @Schema(description = "Whether address has all required fields", accessMode = Schema.AccessMode.READ_ONLY)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public Boolean getIsComplete() {
        return line1 != null && !line1.trim().isEmpty() &&
               city != null && !city.trim().isEmpty() &&
               state != null && !state.trim().isEmpty() &&
               postalCode != null && !postalCode.trim().isEmpty() &&
               country != null && !country.trim().isEmpty();
    }

    // Validation groups for different operations
    public interface CreateValidation {}
    public interface UpdateValidation {}

    // Builder pattern for easier construction
    public static AddressDtoBuilder builder() {
        return new AddressDtoBuilder();
    }

    public static class AddressDtoBuilder {
        private final AddressDto addressDto = new AddressDto();

        public AddressDtoBuilder id(UUID id) {
            addressDto.setId(id);
            return this;
        }

        public AddressDtoBuilder type(String type) {
            addressDto.setType(type);
            return this;
        }

        public AddressDtoBuilder line1(String line1) {
            addressDto.setLine1(line1);
            return this;
        }

        public AddressDtoBuilder line2(String line2) {
            addressDto.setLine2(line2);
            return this;
        }

        public AddressDtoBuilder city(String city) {
            addressDto.setCity(city);
            return this;
        }

        public AddressDtoBuilder state(String state) {
            addressDto.setState(state);
            return this;
        }

        public AddressDtoBuilder postalCode(String postalCode) {
            addressDto.setPostalCode(postalCode);
            return this;
        }

        public AddressDtoBuilder country(String country) {
            addressDto.setCountry(country);
            return this;
        }

        public AddressDtoBuilder isPrimary(Boolean isPrimary) {
            addressDto.setIsPrimary(isPrimary);
            return this;
        }

        public AddressDtoBuilder createdAt(LocalDateTime createdAt) {
            addressDto.setCreatedAt(createdAt);
            return this;
        }

        public AddressDtoBuilder updatedAt(LocalDateTime updatedAt) {
            addressDto.setUpdatedAt(updatedAt);
            return this;
        }

        public AddressDto build() {
            return addressDto;
        }
    }
}
