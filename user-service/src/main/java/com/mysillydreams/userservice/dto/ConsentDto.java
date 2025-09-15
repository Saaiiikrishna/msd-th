package com.mysillydreams.userservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for Consent information.
 * Supports GDPR/DPDP compliance tracking.
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "User consent information for GDPR/DPDP compliance")
public class ConsentDto {

    @Schema(description = "Consent key identifier", example = "marketing_emails")
    @NotBlank(message = "Consent key is required")
    private String consentKey;

    @Schema(description = "Whether consent is granted", example = "true")
    @NotNull(message = "Granted status is required")
    private Boolean granted;

    @Schema(description = "When consent was granted", example = "2024-01-01T12:00:00", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime grantedAt;

    @Schema(description = "When consent was withdrawn", example = "2024-01-01T12:00:00", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime withdrawnAt;

    @Schema(description = "Consent version", example = "v1.0")
    private String consentVersion;

    @Schema(description = "Source of consent", example = "WEB", allowableValues = {"WEB", "MOBILE", "API", "ADMIN", "SYSTEM"})
    private String source;

    @Schema(description = "Legal basis for processing", example = "CONSENT", allowableValues = {"CONSENT", "CONTRACT", "LEGAL_OBLIGATION", "VITAL_INTERESTS", "PUBLIC_TASK", "LEGITIMATE_INTERESTS"})
    private String legalBasis;

    @Schema(description = "IP address when consent was given", example = "192.168.1.1", accessMode = Schema.AccessMode.READ_ONLY)
    private String ipAddress;

    @Schema(description = "User agent when consent was given", accessMode = Schema.AccessMode.READ_ONLY)
    private String userAgent;

    // Computed fields for API responses
    @Schema(description = "Human-readable consent description", example = "Marketing Emails", accessMode = Schema.AccessMode.READ_ONLY)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public String getConsentDescription() {
        if (consentKey == null) {
            return "Unknown Consent";
        }
        
        switch (consentKey.toLowerCase()) {
            case "terms_of_service":
                return "Terms of Service";
            case "privacy_policy":
                return "Privacy Policy";
            case "marketing_emails":
                return "Marketing Emails";
            case "marketing_sms":
                return "Marketing SMS";
            case "analytics":
                return "Analytics & Performance";
            case "personalization":
                return "Personalization";
            case "third_party_sharing":
                return "Third Party Data Sharing";
            case "data_processing":
                return "Data Processing";
            case "cookies_functional":
                return "Functional Cookies";
            case "cookies_analytics":
                return "Analytics Cookies";
            case "cookies_marketing":
                return "Marketing Cookies";
            default:
                return formatConsentKey(consentKey);
        }
    }

    @Schema(description = "Consent status summary", example = "GRANTED", accessMode = Schema.AccessMode.READ_ONLY)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public String getConsentStatus() {
        if (granted == null) {
            return "UNKNOWN";
        }
        
        if (granted) {
            return "GRANTED";
        } else {
            return "WITHDRAWN";
        }
    }

    @Schema(description = "Whether consent is currently active", accessMode = Schema.AccessMode.READ_ONLY)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public Boolean getIsActive() {
        return granted != null && granted && withdrawnAt == null;
    }

    @Schema(description = "Last action timestamp", example = "2024-01-01T12:00:00", accessMode = Schema.AccessMode.READ_ONLY)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public LocalDateTime getLastActionAt() {
        if (withdrawnAt != null) {
            return withdrawnAt;
        }
        return grantedAt;
    }

    @Schema(description = "Source description", example = "Web Application", accessMode = Schema.AccessMode.READ_ONLY)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public String getSourceDescription() {
        if (source == null) {
            return "Unknown";
        }
        
        switch (source.toUpperCase()) {
            case "WEB":
                return "Web Application";
            case "MOBILE":
                return "Mobile Application";
            case "API":
                return "API Call";
            case "ADMIN":
                return "Admin Panel";
            case "SYSTEM":
                return "System Generated";
            default:
                return source;
        }
    }

    @Schema(description = "Legal basis description", example = "Consent (GDPR Art. 6(1)(a))", accessMode = Schema.AccessMode.READ_ONLY)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public String getLegalBasisDescription() {
        if (legalBasis == null) {
            return "Not specified";
        }
        
        switch (legalBasis.toUpperCase()) {
            case "CONSENT":
                return "Consent (GDPR Art. 6(1)(a))";
            case "CONTRACT":
                return "Contract (GDPR Art. 6(1)(b))";
            case "LEGAL_OBLIGATION":
                return "Legal Obligation (GDPR Art. 6(1)(c))";
            case "VITAL_INTERESTS":
                return "Vital Interests (GDPR Art. 6(1)(d))";
            case "PUBLIC_TASK":
                return "Public Task (GDPR Art. 6(1)(e))";
            case "LEGITIMATE_INTERESTS":
                return "Legitimate Interests (GDPR Art. 6(1)(f))";
            default:
                return legalBasis;
        }
    }

    @Schema(description = "Whether this is a marketing-related consent", accessMode = Schema.AccessMode.READ_ONLY)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public Boolean getIsMarketingConsent() {
        return consentKey != null && 
               (consentKey.toLowerCase().contains("marketing") || 
                consentKey.toLowerCase().contains("promotional"));
    }

    @Schema(description = "Whether this is a cookie-related consent", accessMode = Schema.AccessMode.READ_ONLY)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public Boolean getIsCookieConsent() {
        return consentKey != null && consentKey.toLowerCase().contains("cookie");
    }

    // Helper method to format consent key
    private String formatConsentKey(String key) {
        if (key == null) {
            return "Unknown";
        }
        
        String result = key.replace("_", " ").toLowerCase();
        // Capitalize first letter of each word
        StringBuilder sb = new StringBuilder();
        boolean capitalizeNext = true;
        for (char c : result.toCharArray()) {
            if (Character.isWhitespace(c)) {
                capitalizeNext = true;
                sb.append(c);
            } else if (capitalizeNext) {
                sb.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    // Validation groups for different operations
    public interface CreateValidation {}
    public interface UpdateValidation {}

    // Builder pattern for easier construction
    public static ConsentDtoBuilder builder() {
        return new ConsentDtoBuilder();
    }

    public static class ConsentDtoBuilder {
        private final ConsentDto consentDto = new ConsentDto();

        public ConsentDtoBuilder consentKey(String consentKey) {
            consentDto.setConsentKey(consentKey);
            return this;
        }

        public ConsentDtoBuilder granted(Boolean granted) {
            consentDto.setGranted(granted);
            return this;
        }

        public ConsentDtoBuilder grantedAt(LocalDateTime grantedAt) {
            consentDto.setGrantedAt(grantedAt);
            return this;
        }

        public ConsentDtoBuilder withdrawnAt(LocalDateTime withdrawnAt) {
            consentDto.setWithdrawnAt(withdrawnAt);
            return this;
        }

        public ConsentDtoBuilder consentVersion(String consentVersion) {
            consentDto.setConsentVersion(consentVersion);
            return this;
        }

        public ConsentDtoBuilder source(String source) {
            consentDto.setSource(source);
            return this;
        }

        public ConsentDtoBuilder legalBasis(String legalBasis) {
            consentDto.setLegalBasis(legalBasis);
            return this;
        }

        public ConsentDtoBuilder ipAddress(String ipAddress) {
            consentDto.setIpAddress(ipAddress);
            return this;
        }

        public ConsentDtoBuilder userAgent(String userAgent) {
            consentDto.setUserAgent(userAgent);
            return this;
        }

        public ConsentDto build() {
            return consentDto;
        }
    }

    // Common consent keys as constants
    public static final class ConsentKeys {
        // Registration consent keys
        public static final String TERMS_OF_SERVICE = "terms_of_service";
        public static final String PRIVACY_POLICY = "privacy_policy";

        // Marketing consent keys
        public static final String MARKETING_EMAILS = "marketing_emails";
        public static final String MARKETING_SMS = "marketing_sms";

        // Data processing consent keys
        public static final String ANALYTICS = "analytics";
        public static final String PERSONALIZATION = "personalization";
        public static final String THIRD_PARTY_SHARING = "third_party_sharing";
        public static final String DATA_PROCESSING = "data_processing";

        // Cookie consent keys
        public static final String COOKIES_FUNCTIONAL = "cookies_functional";
        public static final String COOKIES_ANALYTICS = "cookies_analytics";
        public static final String COOKIES_MARKETING = "cookies_marketing";

        private ConsentKeys() {
            // Utility class
        }
    }
}
