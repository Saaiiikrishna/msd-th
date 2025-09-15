package com.mysillydreams.userservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Data export object for GDPR Article 20 - Right to Data Portability.
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "User data export for GDPR compliance")
public class UserDataExport {

    @Schema(description = "Unique export identifier")
    private UUID exportId = UUID.randomUUID();

    @Schema(description = "User reference ID")
    private String userReferenceId;

    @Schema(description = "Export generation date")
    private LocalDateTime exportDate;

    @Schema(description = "Who requested the export")
    private String requestedBy;

    @Schema(description = "User profile data")
    private UserData userData;

    @Schema(description = "User addresses")
    private java.util.List<AddressData> addresses;

    @Schema(description = "User consents")
    private java.util.List<ConsentData> consents;

    @Schema(description = "User audit records")
    private java.util.List<AuditData> auditRecords;

    public int getTotalRecords() {
        int total = 0;
        if (userData != null) total += 1;
        if (addresses != null) total += addresses.size();
        if (consents != null) total += consents.size();
        if (auditRecords != null) total += auditRecords.size();
        return total;
    }

    @Data
    @Schema(description = "User profile data for export")
    public static class UserData {
        private String referenceId;
        private String firstName;
        private String lastName;
        private String email;
        private String phone;
        private String dateOfBirth;
        private String gender;
        private String avatarUrl;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private java.util.Set<String> roles;
    }

    @Data
    @Schema(description = "Address data for export")
    public static class AddressData {
        private String type;
        private String line1;
        private String line2;
        private String city;
        private String state;
        private String postalCode;
        private String country;
        private Boolean isPrimary;
        private LocalDateTime createdAt;
    }

    @Data
    @Schema(description = "Consent data for export")
    public static class ConsentData {
        private String consentKey;
        private Boolean granted;
        private LocalDateTime grantedAt;
        private LocalDateTime withdrawnAt;
        private String consentVersion;
        private String source;
        private String legalBasis;
    }

    @Data
    @Schema(description = "Audit data for export")
    public static class AuditData {
        private String eventType;
        private String description;
        private LocalDateTime timestamp;
        private String ipAddress;
        private String userAgent;
        private Map<String, Object> details;
    }
}
