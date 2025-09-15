package com.mysillydreams.userservice.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * User entity representing a user in the system.
 * Contains encrypted PII fields with HMAC search indices and proper relationships.
 */
@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_users_reference_id", columnList = "reference_id"),
    @Index(name = "idx_users_email_hmac", columnList = "email_hmac"),
    @Index(name = "idx_users_phone_hmac", columnList = "phone_hmac"),
    @Index(name = "idx_users_active", columnList = "active"),
    @Index(name = "idx_users_created_at", columnList = "created_at"),
    @Index(name = "idx_users_deleted_at", columnList = "deleted_at")
})
@Data
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"addresses", "sessions", "roles", "consents", "auditRecords"})
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "reference_id", unique = true, nullable = false, length = 36)
    private String referenceId;

    // Encrypted PII fields (using _enc suffix for clarity)
    @Column(name = "first_name_enc", length = 1000)
    private String firstNameEnc;

    @Column(name = "last_name_enc", length = 1000)
    private String lastNameEnc;

    @Column(name = "email_enc", length = 1000)
    private String emailEnc;

    @Column(name = "phone_enc", length = 1000)
    private String phoneEnc;

    @Column(name = "dob_enc", length = 1000) // Date of birth as encrypted string
    private String dobEnc;

    // HMAC search fields (deterministic hashes for uniqueness and search)
    @Column(name = "email_hmac", length = 64, unique = true)
    private String emailHmac;

    @Column(name = "phone_hmac", length = 64, unique = true)
    private String phoneHmac;

    // Non-PII fields
    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 25)
    private Gender gender;

    @Column(name = "avatar_url", length = 1000)
    private String avatarUrl;

    // Status fields
    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "anonymized")
    private Boolean anonymized = false;

    @Column(name = "anonymized_at")
    private LocalDateTime anonymizedAt;

    @Column(name = "deletion_reason")
    private String deletionReason;

    // Audit fields
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

    // Relationships
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<UserRoleEntity> roles = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<AddressEntity> addresses = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<ConsentEntity> consents = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<SessionEntity> sessions = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<UserAuditEntity> auditRecords = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<UserExternalRefEntity> externalRefs = new ArrayList<>();

    // Constructors
    public UserEntity() {}

    public UserEntity(String referenceId) {
        this.referenceId = referenceId;
        this.active = true;
    }

    // Helper methods for relationships
    public void addRole(UserRoleEntity role) {
        roles.add(role);
        role.setUser(this);
    }

    public void removeRole(UserRoleEntity role) {
        roles.remove(role);
        role.setUser(null);
    }

    public void addAddress(AddressEntity address) {
        addresses.add(address);
        address.setUser(this);
    }

    public void removeAddress(AddressEntity address) {
        addresses.remove(address);
        address.setUser(null);
    }

    public void addConsent(ConsentEntity consent) {
        consents.add(consent);
        consent.setUser(this);
    }

    public void removeConsent(ConsentEntity consent) {
        consents.remove(consent);
        consent.setUser(null);
    }

    public void addExternalRef(UserExternalRefEntity externalRef) {
        externalRefs.add(externalRef);
        externalRef.setUser(this);
    }

    public void removeExternalRef(UserExternalRefEntity externalRef) {
        externalRefs.remove(externalRef);
        externalRef.setUser(null);
    }

    // Business methods
    public boolean isActive() {
        return active != null && active && deletedAt == null;
    }

    public void markAsDeleted() {
        this.active = false;
        this.deletedAt = LocalDateTime.now();
    }

    public void reactivate() {
        this.active = true;
        this.deletedAt = null;
        this.anonymized = false;
        this.anonymizedAt = null;
        this.deletionReason = null;
    }

    public void anonymize() {
        this.anonymized = true;
        this.anonymizedAt = LocalDateTime.now();
    }

    public boolean isAnonymized() {
        return Boolean.TRUE.equals(anonymized);
    }

    public Set<String> getRoleNames() {
        Set<String> roleNames = new HashSet<>();
        for (UserRoleEntity role : roles) {
            roleNames.add(role.getRole());
        }
        return roleNames;
    }

    public boolean hasRole(String roleName) {
        return roles.stream().anyMatch(role -> role.getRole().equals(roleName));
    }

    public AddressEntity getPrimaryAddress() {
        return addresses.stream()
                .filter(AddressEntity::getIsPrimary)
                .findFirst()
                .orElse(null);
    }

    // Gender enum
    public enum Gender {
        MALE, FEMALE, OTHER, PREFER_NOT_TO_SAY
    }

    // Manual getters for fields that Lombok isn't generating properly
    // Note: These return encrypted values for now - proper decryption should be handled by service layer
    public String getFirstName() {
        return firstNameEnc; // TODO: Add decryption logic
    }

    public String getLastName() {
        return lastNameEnc; // TODO: Add decryption logic
    }

    public String getEmail() {
        return emailEnc; // TODO: Add decryption logic
    }

    public String getPhone() {
        return phoneEnc; // TODO: Add decryption logic
    }

    public String getDob() {
        return dobEnc; // TODO: Add decryption logic
    }

    public Gender getGender() {
        return gender;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public UUID getId() {
        return id;
    }

    // Validation constraints
    @PrePersist
    @PreUpdate
    private void validateEntity() {
        if (referenceId == null || referenceId.trim().isEmpty()) {
            throw new IllegalStateException("Reference ID cannot be null or empty");
        }
        
        if (active == null) {
            active = true;
        }
        
        // Ensure deleted_at is set when active is false
        if (!active && deletedAt == null) {
            deletedAt = LocalDateTime.now();
        }
        
        // Ensure deleted_at is null when active is true
        if (active && deletedAt != null) {
            deletedAt = null;
        }
    }
}
