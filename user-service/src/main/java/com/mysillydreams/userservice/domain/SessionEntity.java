package com.mysillydreams.userservice.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Entity for storing session metadata (read-only).
 * Actual session management is handled by the Auth Service.
 */
@Entity
@Table(name = "sessions", indexes = {
    @Index(name = "idx_sessions_user_id", columnList = "user_id"),
    @Index(name = "idx_sessions_token_hash", columnList = "session_token_hash"),
    @Index(name = "idx_sessions_active", columnList = "active"),
    @Index(name = "idx_sessions_expires_at", columnList = "expires_at")
})
@Data
@EqualsAndHashCode(of = "id")
@ToString(exclude = "user")
public class SessionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    // Session metadata (hash of actual token for lookup)
    @Column(name = "session_token_hash", nullable = false, unique = true, length = 64)
    private String sessionTokenHash;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "device_info", columnDefinition = "jsonb")
    private Map<String, Object> deviceInfo;

    @Column(name = "ip_address", columnDefinition = "inet")
    private InetAddress ipAddress;

    @Column(name = "user_agent", length = 1000)
    private String userAgent;

    // Session lifecycle
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "last_accessed_at", nullable = false)
    private LocalDateTime lastAccessedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    // Status
    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Column(name = "terminated_at")
    private LocalDateTime terminatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "termination_reason", length = 50)
    private TerminationReason terminationReason;

    // Constructors
    public SessionEntity() {}

    public SessionEntity(UserEntity user, String sessionTokenHash, LocalDateTime expiresAt) {
        this.user = user;
        this.sessionTokenHash = sessionTokenHash;
        this.expiresAt = expiresAt;
        this.active = true;
    }

    // Business methods
    public boolean isActive() {
        return active != null && active && 
               terminatedAt == null && 
               expiresAt != null && expiresAt.isAfter(LocalDateTime.now());
    }

    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(LocalDateTime.now());
    }

    public void terminate(TerminationReason reason) {
        this.active = false;
        this.terminatedAt = LocalDateTime.now();
        this.terminationReason = reason;
    }

    public void updateLastAccessed() {
        this.lastAccessedAt = LocalDateTime.now();
    }

    // Termination reasons
    public enum TerminationReason {
        EXPIRED("Session expired"),
        LOGOUT("User logout"),
        ADMIN_TERMINATED("Terminated by admin"),
        SECURITY_BREACH("Security breach"),
        CONCURRENT_LOGIN("Concurrent login limit"),
        SYSTEM_MAINTENANCE("System maintenance"),
        INACTIVITY("Inactivity timeout");

        private final String description;

        TerminationReason(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    // Validation
    @PrePersist
    @PreUpdate
    private void validateEntity() {
        if (sessionTokenHash == null || sessionTokenHash.trim().isEmpty()) {
            throw new IllegalStateException("Session token hash cannot be null or empty");
        }
        
        if (expiresAt == null) {
            throw new IllegalStateException("Expiration time cannot be null");
        }
        
        if (expiresAt.isBefore(createdAt != null ? createdAt : LocalDateTime.now())) {
            throw new IllegalStateException("Expiration time cannot be before creation time");
        }
        
        if (active == null) {
            active = true;
        }
        
        // Ensure terminated_at is set when active is false
        if (!active && terminatedAt == null) {
            terminatedAt = LocalDateTime.now();
        }
        
        // Ensure terminated_at is null when active is true
        if (active && terminatedAt != null) {
            terminatedAt = null;
            terminationReason = null;
        }
    }
}
