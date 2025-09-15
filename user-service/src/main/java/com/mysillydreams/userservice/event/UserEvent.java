package com.mysillydreams.userservice.event;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Base class for all user-related events published to Kafka.
 * Provides common event metadata and structure.
 */
@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "eventType")
@JsonSubTypes({
    @JsonSubTypes.Type(value = UserCreatedEvent.class, name = "USER_CREATED"),
    @JsonSubTypes.Type(value = UserUpdatedEvent.class, name = "USER_UPDATED"),
    @JsonSubTypes.Type(value = UserDeletedEvent.class, name = "USER_DELETED"),
    @JsonSubTypes.Type(value = UserReactivatedEvent.class, name = "USER_REACTIVATED"),
    @JsonSubTypes.Type(value = UserRoleChangedEvent.class, name = "USER_ROLE_CHANGED"),
    @JsonSubTypes.Type(value = UserDataDeletionEvent.class, name = "USER_DATA_DELETION")
})
public abstract class UserEvent {

    private String eventId = UUID.randomUUID().toString();
    private String eventType;
    private String userReferenceId;
    private UUID userId;
    private LocalDateTime timestamp = LocalDateTime.now();
    private String correlationId;
    private String source = "user-service";
    private String version = "1.0";
    private Map<String, Object> metadata;

    protected UserEvent(String eventType, String userReferenceId, UUID userId) {
        this.eventType = eventType;
        this.userReferenceId = userReferenceId;
        this.userId = userId;
    }

    public UserEvent withCorrelationId(String correlationId) {
        this.correlationId = correlationId;
        return this;
    }

    public UserEvent withMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
        return this;
    }
}

// UserCreatedEvent moved to separate file

/**
 * Event published when user information is updated
 */
@Data
@EqualsAndHashCode(callSuper=false)
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
class UserUpdatedEvent extends UserEvent {
    private Map<String, Object> previousValues;
    private Map<String, Object> newValues;
    private java.util.Set<String> changedFields;

    public UserUpdatedEvent(String userReferenceId, UUID userId, Map<String, Object> previousValues,
                           Map<String, Object> newValues, java.util.Set<String> changedFields) {
        super("USER_UPDATED", userReferenceId, userId);
        this.previousValues = previousValues;
        this.newValues = newValues;
        this.changedFields = changedFields;
    }
}

/**
 * Event published when a user is soft deleted
 */
@Data
@EqualsAndHashCode(callSuper=false)
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
class UserDeletedEvent extends UserEvent {
    private String deletionReason;
    private LocalDateTime deletedAt;
    private UUID deletedBy;

    public UserDeletedEvent(String userReferenceId, UUID userId, String deletionReason, UUID deletedBy) {
        super("USER_DELETED", userReferenceId, userId);
        this.deletionReason = deletionReason;
        this.deletedAt = LocalDateTime.now();
        this.deletedBy = deletedBy;
    }
}

/**
 * Event published when a user is reactivated
 */
@Data
@EqualsAndHashCode(callSuper=false)
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
class UserReactivatedEvent extends UserEvent {
    private LocalDateTime reactivatedAt;
    private UUID reactivatedBy;

    public UserReactivatedEvent(String userReferenceId, UUID userId, UUID reactivatedBy) {
        super("USER_REACTIVATED", userReferenceId, userId);
        this.reactivatedAt = LocalDateTime.now();
        this.reactivatedBy = reactivatedBy;
    }
}

/**
 * Event published when user roles are changed
 */
@Data
@EqualsAndHashCode(callSuper=false)
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
class UserRoleChangedEvent extends UserEvent {
    private java.util.Set<String> previousRoles;
    private java.util.Set<String> newRoles;
    private java.util.Set<String> addedRoles;
    private java.util.Set<String> removedRoles;
    private UUID changedBy;

    public UserRoleChangedEvent(String userReferenceId, UUID userId, java.util.Set<String> previousRoles,
                               java.util.Set<String> newRoles, java.util.Set<String> addedRoles,
                               java.util.Set<String> removedRoles, UUID changedBy) {
        super("USER_ROLE_CHANGED", userReferenceId, userId);
        this.previousRoles = previousRoles;
        this.newRoles = newRoles;
        this.addedRoles = addedRoles;
        this.removedRoles = removedRoles;
        this.changedBy = changedBy;
    }
}

// UserDataDeletionEvent moved to separate file

/**
 * Event published when user consents are changed
 */
@Data
@EqualsAndHashCode(callSuper=false)
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
class ConsentEvent extends UserEvent {
    private String consentKey;
    private boolean granted;
    private String consentVersion;
    private String source;
    private String legalBasis;
    private String ipAddress;
    private String userAgent;

    public ConsentEvent(String eventType, String userReferenceId, UUID userId, String consentKey,
                       boolean granted, String consentVersion, String source, String legalBasis,
                       String ipAddress, String userAgent) {
        super(eventType, userReferenceId, userId);
        this.consentKey = consentKey;
        this.granted = granted;
        this.consentVersion = consentVersion;
        this.source = source;
        this.legalBasis = legalBasis;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
    }
}

/**
 * Event published when user address is changed
 */
@Data
@EqualsAndHashCode(callSuper=false)
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
class AddressEvent extends UserEvent {
    private UUID addressId;
    private String addressType;
    private boolean isPrimary;
    private String action; // CREATED, UPDATED, DELETED, SET_PRIMARY

    public AddressEvent(String userReferenceId, UUID userId, UUID addressId, String addressType,
                       boolean isPrimary, String action) {
        super("ADDRESS_" + action, userReferenceId, userId);
        this.addressId = addressId;
        this.addressType = addressType;
        this.isPrimary = isPrimary;
        this.action = action;
    }
}

/**
 * Event published for audit trail
 */
@Data
@EqualsAndHashCode(callSuper=false)
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
class AuditEvent extends UserEvent {
    private String auditEventType;
    private String description;
    private UUID performedBy;
    private String ipAddress;
    private String userAgent;
    private String sessionId;
    private Map<String, Object> auditDetails;

    public AuditEvent(String userReferenceId, UUID userId, String auditEventType, String description,
                     UUID performedBy, String ipAddress, String userAgent, String sessionId,
                     Map<String, Object> auditDetails) {
        super("AUDIT_EVENT", userReferenceId, userId);
        this.auditEventType = auditEventType;
        this.description = description;
        this.performedBy = performedBy;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.sessionId = sessionId;
        this.auditDetails = auditDetails;
    }
}
