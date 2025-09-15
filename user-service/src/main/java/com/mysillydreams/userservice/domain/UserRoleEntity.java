package com.mysillydreams.userservice.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing user role assignments.
 * Supports role hierarchy and audit trail for role changes.
 */
@Entity
@Table(name = "user_roles", indexes = {
    @Index(name = "idx_user_roles_user_id", columnList = "user_id"),
    @Index(name = "idx_user_roles_role", columnList = "role"),
    @Index(name = "idx_user_roles_assigned_at", columnList = "assigned_at")
})
@Data
@EqualsAndHashCode(of = {"user", "role"})
@ToString(exclude = "user")
public class UserRoleEntity {

    @EmbeddedId
    private UserRoleId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "role", nullable = false, length = 50, insertable = false, updatable = false)
    private String role;

    @CreationTimestamp
    @Column(name = "assigned_at", nullable = false, updatable = false)
    private LocalDateTime assignedAt;

    @Column(name = "assigned_by")
    private UUID assignedBy; // Reference to admin user who assigned the role

    @Column(name = "expires_at")
    private LocalDateTime expiresAt; // Optional role expiration

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    // Constructors
    public UserRoleEntity() {}

    public UserRoleEntity(UserEntity user, String role) {
        this.id = new UserRoleId(user.getId(), role);
        this.user = user;
        this.role = role;
        this.active = true;
    }

    public UserRoleEntity(UserEntity user, String role, UUID assignedBy) {
        this(user, role);
        this.assignedBy = assignedBy;
    }

    // Business methods
    public boolean isActive() {
        return active != null && active && (expiresAt == null || expiresAt.isAfter(LocalDateTime.now()));
    }

    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(LocalDateTime.now());
    }

    public void deactivate() {
        this.active = false;
    }

    public void setExpiration(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    // Validation
    @PrePersist
    @PreUpdate
    private void validateEntity() {
        if (role == null || role.trim().isEmpty()) {
            throw new IllegalStateException("Role cannot be null or empty");
        }
        
        if (!role.startsWith("ROLE_")) {
            throw new IllegalStateException("Role must start with 'ROLE_' prefix");
        }
        
        if (active == null) {
            active = true;
        }
    }

    /**
     * Composite primary key for UserRoleEntity
     */
    @Embeddable
    @Data
    @EqualsAndHashCode
    public static class UserRoleId {
        @Column(name = "user_id")
        private UUID userId;

        @Column(name = "role", length = 50)
        private String role;

        public UserRoleId() {}

        public UserRoleId(UUID userId, String role) {
            this.userId = userId;
            this.role = role;
        }
    }
}
