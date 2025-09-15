package com.mysillydreams.userservice.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity for storing external system references for users.
 * Enables cross-service user identification and integration.
 */
@Entity
@Table(name = "user_external_refs", indexes = {
    @Index(name = "idx_user_external_refs_user_id", columnList = "user_id"),
    @Index(name = "idx_user_external_refs_system", columnList = "system"),
    @Index(name = "idx_user_external_refs_external_id", columnList = "external_id")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_user_external_refs_system_external_id", 
                     columnNames = {"system", "external_id"})
})
@Data
@EqualsAndHashCode(of = {"user", "system"})
@ToString(exclude = "user")
public class UserExternalRefEntity {

    @EmbeddedId
    private UserExternalRefId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "system", nullable = false, length = 50, insertable = false, updatable = false)
    private String system;

    @Column(name = "external_id", nullable = false, length = 255)
    private String externalId;

    @Column(name = "metadata", length = 1000)
    private String metadata; // JSON string for additional system-specific data

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Constructors
    public UserExternalRefEntity() {}

    public UserExternalRefEntity(UserEntity user, ExternalSystem system, String externalId) {
        this.id = new UserExternalRefId(user.getId(), system.name());
        this.user = user;
        this.system = system.name();
        this.externalId = externalId;
        this.active = true;
    }

    public UserExternalRefEntity(UserEntity user, ExternalSystem system, String externalId, String metadata) {
        this(user, system, externalId);
        this.metadata = metadata;
    }

    // Business methods
    public boolean isActive() {
        return active != null && active;
    }

    public void deactivate() {
        this.active = false;
    }

    public void reactivate() {
        this.active = true;
    }

    public ExternalSystem getSystemEnum() {
        try {
            return ExternalSystem.valueOf(system);
        } catch (IllegalArgumentException e) {
            return ExternalSystem.OTHER;
        }
    }

    // External system enum
    public enum ExternalSystem {
        PAYMENTS("Payment Service"),
        ORDERS("Order Service"),
        NOTIFICATIONS("Notification Service"),
        ANALYTICS("Analytics Service"),
        SUPPORT("Support System"),
        CRM("Customer Relationship Management"),
        MARKETING("Marketing Platform"),
        INVENTORY("Inventory Management"),
        DELIVERY("Delivery Service"),
        OTHER("Other System");

        private final String displayName;

        ExternalSystem(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * Composite primary key for UserExternalRefEntity
     */
    @Embeddable
    @Data
    @EqualsAndHashCode
    public static class UserExternalRefId {
        @Column(name = "user_id")
        private UUID userId;

        @Column(name = "system", length = 50)
        private String system;

        public UserExternalRefId() {}

        public UserExternalRefId(UUID userId, String system) {
            this.userId = userId;
            this.system = system;
        }
    }

    // Validation
    @PrePersist
    @PreUpdate
    private void validateEntity() {
        if (system == null || system.trim().isEmpty()) {
            throw new IllegalStateException("System cannot be null or empty");
        }
        
        if (externalId == null || externalId.trim().isEmpty()) {
            throw new IllegalStateException("External ID cannot be null or empty");
        }
        
        if (active == null) {
            active = true;
        }
        
        // Validate system name
        try {
            ExternalSystem.valueOf(system);
        } catch (IllegalArgumentException e) {
            // Allow unknown systems but log a warning
            System.out.println("Warning: Unknown external system: " + system);
        }
    }

    // Helper methods for common systems
    public static UserExternalRefEntity forPayments(UserEntity user, String paymentUserId) {
        return new UserExternalRefEntity(user, ExternalSystem.PAYMENTS, paymentUserId);
    }

    public static UserExternalRefEntity forOrders(UserEntity user, String orderUserId) {
        return new UserExternalRefEntity(user, ExternalSystem.ORDERS, orderUserId);
    }

    public static UserExternalRefEntity forNotifications(UserEntity user, String notificationUserId) {
        return new UserExternalRefEntity(user, ExternalSystem.NOTIFICATIONS, notificationUserId);
    }

    public static UserExternalRefEntity forAnalytics(UserEntity user, String analyticsUserId) {
        return new UserExternalRefEntity(user, ExternalSystem.ANALYTICS, analyticsUserId);
    }

    public static UserExternalRefEntity forSupport(UserEntity user, String supportUserId) {
        return new UserExternalRefEntity(user, ExternalSystem.SUPPORT, supportUserId);
    }
}
