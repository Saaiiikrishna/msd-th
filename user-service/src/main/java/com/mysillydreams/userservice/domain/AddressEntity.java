package com.mysillydreams.userservice.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing user addresses with encrypted PII fields.
 * Supports multiple address types and primary address designation.
 */
@Entity
@Table(name = "addresses", indexes = {
    @Index(name = "idx_addresses_user_id", columnList = "user_id"),
    @Index(name = "idx_addresses_type", columnList = "type"),
    @Index(name = "idx_addresses_is_primary", columnList = "is_primary")
})
@Data
@EqualsAndHashCode(of = "id")
@ToString(exclude = "user")
public class AddressEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    // Address type
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private AddressType type = AddressType.HOME;

    // Encrypted address fields
    @Column(name = "line1_enc", length = 1000)
    private String line1Enc;

    @Column(name = "line2_enc", length = 1000)
    private String line2Enc;

    @Column(name = "city_enc", length = 1000)
    private String cityEnc;

    @Column(name = "state_enc", length = 1000)
    private String stateEnc;

    @Column(name = "postal_code_enc", length = 1000)
    private String postalCodeEnc;

    @Column(name = "country_enc", length = 1000)
    private String countryEnc;

    // Metadata
    @Column(name = "is_primary", nullable = false)
    private Boolean isPrimary = false;

    // Audit fields
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Constructors
    public AddressEntity() {}

    public AddressEntity(UserEntity user, AddressType type) {
        this.user = user;
        this.type = type;
        this.isPrimary = false;
    }

    // Business methods
    public void markAsPrimary() {
        this.isPrimary = true;
    }

    public void markAsSecondary() {
        this.isPrimary = false;
    }

    public boolean isPrimary() {
        return isPrimary != null && isPrimary;
    }

    // Address type enum
    public enum AddressType {
        HOME("Home Address"),
        WORK("Work Address"),
        BILLING("Billing Address"),
        SHIPPING("Shipping Address"),
        OTHER("Other Address");

        private final String displayName;

        AddressType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    // Validation
    @PrePersist
    @PreUpdate
    private void validateEntity() {
        if (user == null) {
            throw new IllegalStateException("Address must be associated with a user");
        }
        
        if (type == null) {
            type = AddressType.HOME;
        }
        
        if (isPrimary == null) {
            isPrimary = false;
        }
    }

    // Helper method to check if address has required fields
    public boolean hasRequiredFields() {
        return line1Enc != null && !line1Enc.trim().isEmpty() &&
               cityEnc != null && !cityEnc.trim().isEmpty() &&
               countryEnc != null && !countryEnc.trim().isEmpty();
    }

    // Helper method to get address summary (for display purposes)
    public String getAddressSummary() {
        StringBuilder summary = new StringBuilder();
        
        if (type != null) {
            summary.append(type.getDisplayName());
        }
        
        if (isPrimary()) {
            summary.append(" (Primary)");
        }
        
        return summary.toString();
    }
}
