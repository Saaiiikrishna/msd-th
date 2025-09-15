package com.mysillydreams.userservice.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Entity for auditing user-related actions and changes.
 * Provides comprehensive audit trail for compliance and security.
 */
@Entity
@Table(name = "user_audit", indexes = {
    @Index(name = "idx_user_audit_user_id", columnList = "user_id"),
    @Index(name = "idx_user_audit_event_type", columnList = "event_type"),
    @Index(name = "idx_user_audit_created_at", columnList = "created_at"),
    @Index(name = "idx_user_audit_performed_by", columnList = "performed_by")
})
@Data
@EqualsAndHashCode(of = "id")
@ToString(exclude = "user")
public class UserAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private AuditEventType eventType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "details", columnDefinition = "jsonb")
    private Map<String, Object> details;

    // Context information
    @Column(name = "performed_by")
    private UUID performedBy; // User who performed the action

    @Column(name = "ip_address", columnDefinition = "inet")
    private InetAddress ipAddress;

    @Column(name = "user_agent", length = 1000)
    private String userAgent;

    @Column(name = "session_id", length = 100)
    private String sessionId;

    @Column(name = "correlation_id", length = 100)
    private String correlationId;

    // Timestamp
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // GDPR anonymization fields
    @Column(name = "anonymized")
    private Boolean anonymized = false;

    @Column(name = "anonymized_at")
    private LocalDateTime anonymizedAt;

    // Constructors
    public UserAuditEntity() {}

    public UserAuditEntity(UserEntity user, AuditEventType eventType) {
        this.user = user;
        this.eventType = eventType;
    }

    public UserAuditEntity(UserEntity user, AuditEventType eventType, Map<String, Object> details) {
        this(user, eventType);
        this.details = details;
    }

    public UserAuditEntity(UserEntity user, AuditEventType eventType, Map<String, Object> details,
                          UUID performedBy, InetAddress ipAddress, String userAgent) {
        this(user, eventType, details);
        this.performedBy = performedBy;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
    }

    // Business methods
    public void addDetail(String key, Object value) {
        if (details == null) {
            details = new java.util.HashMap<>();
        }
        details.put(key, value);
    }

    public Object getDetail(String key) {
        return details != null ? details.get(key) : null;
    }

    public boolean hasDetail(String key) {
        return details != null && details.containsKey(key);
    }

    public void setContext(UUID performedBy, InetAddress ipAddress, String userAgent, 
                          String sessionId, String correlationId) {
        this.performedBy = performedBy;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.sessionId = sessionId;
        this.correlationId = correlationId;
    }

    // Audit event types
    public enum AuditEventType {
        // User lifecycle events
        USER_CREATED("User account created"),
        USER_UPDATED("User profile updated"),
        USER_DELETED("User account deleted"),
        USER_ARCHIVED("User account archived"),
        USER_REACTIVATED("User account reactivated"),

        // Authentication events
        LOGIN_SUCCESS("Successful login"),
        LOGIN_FAILED("Failed login attempt"),
        LOGOUT("User logout"),
        PASSWORD_CHANGED("Password changed"),
        PASSWORD_RESET_REQUESTED("Password reset requested"),
        PASSWORD_RESET_COMPLETED("Password reset completed"),

        // Role and permission events
        ROLE_ASSIGNED("Role assigned to user"),
        ROLE_REMOVED("Role removed from user"),
        PERMISSION_GRANTED("Permission granted"),
        PERMISSION_REVOKED("Permission revoked"),

        // Consent events
        CONSENT_GRANTED("Consent granted"),
        CONSENT_WITHDRAWN("Consent withdrawn"),
        CONSENT_UPDATED("Consent preferences updated"),

        // Address events
        ADDRESS_ADDED("Address added"),
        ADDRESS_UPDATED("Address updated"),
        ADDRESS_DELETED("Address deleted"),
        ADDRESS_PRIMARY_CHANGED("Primary address changed"),

        // Data events
        DATA_EXPORT_REQUESTED("Data export requested"),
        DATA_EXPORT_GENERATED("Data export generated"),
        DATA_EXPORT_COMPLETED("Data export completed"),
        DATA_EXPORT_DOWNLOADED("Data export downloaded"),
        DATA_DELETION_REQUESTED("Data deletion requested"),
        DATA_ANONYMIZED("User data anonymized"),

        // GDPR/DPDP specific events
        GDPR_DELETION_REQUESTED("GDPR deletion requested"),
        GDPR_DELETION_COMPLETED("GDPR deletion completed"),
        GDPR_DELETION_FAILED("GDPR deletion failed"),
        CONSENT_DELETED("Consent deleted"),
        RETENTION_POLICY_APPLIED("Retention policy applied"),

        // Security events
        SUSPICIOUS_ACTIVITY("Suspicious activity detected"),
        ACCOUNT_LOCKED("Account locked"),
        ACCOUNT_UNLOCKED("Account unlocked"),
        SECURITY_BREACH("Security breach detected"),

        // Privacy events
        PRIVACY_SETTINGS_CHANGED("Privacy settings changed"),
        PII_ACCESSED("PII data accessed"),
        PII_MODIFIED("PII data modified"),

        // System events
        SYSTEM_MIGRATION("System migration performed"),
        DATA_CORRECTION("Data correction applied"),
        COMPLIANCE_CHECK("Compliance check performed");

        private final String description;

        AuditEventType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }

        // Helper methods to categorize events
        public boolean isSecurityEvent() {
            return this == LOGIN_FAILED || this == SUSPICIOUS_ACTIVITY || 
                   this == ACCOUNT_LOCKED || this == SECURITY_BREACH;
        }

        public boolean isPrivacyEvent() {
            return this == CONSENT_GRANTED || this == CONSENT_WITHDRAWN || 
                   this == DATA_EXPORT_REQUESTED || this == DATA_DELETION_REQUESTED ||
                   this == PRIVACY_SETTINGS_CHANGED || this == PII_ACCESSED || this == PII_MODIFIED;
        }

        public boolean isUserLifecycleEvent() {
            return this == USER_CREATED || this == USER_UPDATED || 
                   this == USER_DELETED || this == USER_ARCHIVED || this == USER_REACTIVATED;
        }
    }

    // Validation
    @PrePersist
    private void validateEntity() {
        if (eventType == null) {
            throw new IllegalStateException("Event type cannot be null");
        }
    }

    // Helper method to create audit record
    public static UserAuditEntity createAuditRecord(UserEntity user, AuditEventType eventType, 
                                                   String description, UUID performedBy) {
        UserAuditEntity audit = new UserAuditEntity(user, eventType);
        audit.addDetail("description", description);
        audit.setPerformedBy(performedBy);
        return audit;
    }

    // Helper method to create audit record with full context
    public static UserAuditEntity createAuditRecord(UserEntity user, AuditEventType eventType,
                                                   Map<String, Object> details, UUID performedBy,
                                                   InetAddress ipAddress, String userAgent,
                                                   String sessionId, String correlationId) {
        UserAuditEntity audit = new UserAuditEntity(user, eventType, details, performedBy, ipAddress, userAgent);
        audit.setSessionId(sessionId);
        audit.setCorrelationId(correlationId);
        return audit;
    }

    // GDPR anonymization methods
    public void anonymize() {
        this.ipAddress = null;
        this.userAgent = "ANONYMIZED";
        this.sessionId = "ANONYMIZED";
        this.anonymized = true;
        this.anonymizedAt = LocalDateTime.now();

        // Anonymize sensitive details
        if (this.details != null) {
            this.details.replaceAll((key, value) -> {
                if (key.toLowerCase().contains("email") ||
                    key.toLowerCase().contains("phone") ||
                    key.toLowerCase().contains("name")) {
                    return "ANONYMIZED";
                }
                return value;
            });
        }
    }

    public boolean isAnonymized() {
        return Boolean.TRUE.equals(anonymized);
    }
}
