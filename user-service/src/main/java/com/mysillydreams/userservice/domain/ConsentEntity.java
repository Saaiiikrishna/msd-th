package com.mysillydreams.userservice.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing user consents for GDPR/DPDP compliance.
 * Tracks consent grants, withdrawals, and audit information.
 */
@Entity
@Table(name = "consents", indexes = {
    @Index(name = "idx_consents_user_id", columnList = "user_id"),
    @Index(name = "idx_consents_key", columnList = "consent_key"),
    @Index(name = "idx_consents_granted", columnList = "granted"),
    @Index(name = "idx_consents_granted_at", columnList = "granted_at")
})
@Data
@EqualsAndHashCode(of = {"user", "consentKey"})
@ToString(exclude = "user")
public class ConsentEntity {

    @EmbeddedId
    private ConsentId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "consent_key", nullable = false, length = 100, insertable = false, updatable = false)
    private String consentKey;

    @Column(name = "granted", nullable = false)
    private Boolean granted;

    @Column(name = "granted_at", nullable = false)
    private LocalDateTime grantedAt;

    @Column(name = "withdrawn_at")
    private LocalDateTime withdrawnAt;

    // Metadata for compliance
    @Column(name = "consent_version", length = 10)
    private String consentVersion;

    @Column(name = "ip_address", columnDefinition = "inet")
    private InetAddress ipAddress;

    @Column(name = "user_agent", length = 1000)
    private String userAgent;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", length = 50)
    private ConsentSource source = ConsentSource.WEB;

    @Enumerated(EnumType.STRING)
    @Column(name = "legal_basis", length = 50)
    private LegalBasis legalBasis = LegalBasis.CONSENT;

    // Constructors
    public ConsentEntity() {}

    public ConsentEntity(UserEntity user, String consentKey, Boolean granted) {
        this.id = new ConsentId(user.getId(), consentKey);
        this.user = user;
        this.consentKey = consentKey;
        this.granted = granted;
        this.grantedAt = LocalDateTime.now();
        
        if (!granted) {
            this.withdrawnAt = LocalDateTime.now();
        }
    }

    public ConsentEntity(UserEntity user, String consentKey, Boolean granted, 
                        String consentVersion, InetAddress ipAddress, String userAgent) {
        this(user, consentKey, granted);
        this.consentVersion = consentVersion;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
    }

    // Business methods
    public boolean isActive() {
        return granted != null && granted && withdrawnAt == null;
    }

    public void grantConsent(String version, InetAddress ip, String agent) {
        this.granted = true;
        this.grantedAt = LocalDateTime.now();
        this.withdrawnAt = null;
        this.consentVersion = version;
        this.ipAddress = ip;
        this.userAgent = agent;
    }

    public void withdrawConsent(InetAddress ip, String agent) {
        this.granted = false;
        this.withdrawnAt = LocalDateTime.now();
        this.ipAddress = ip;
        this.userAgent = agent;
    }

    public boolean isWithdrawn() {
        return !granted || withdrawnAt != null;
    }

    // Enums
    public enum ConsentSource {
        WEB("Web Application"),
        MOBILE("Mobile Application"),
        API("API Call"),
        ADMIN("Admin Panel"),
        SYSTEM("System Generated");

        private final String displayName;

        ConsentSource(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public enum LegalBasis {
        CONSENT("Consent (GDPR Art. 6(1)(a))"),
        CONTRACT("Contract (GDPR Art. 6(1)(b))"),
        LEGAL_OBLIGATION("Legal Obligation (GDPR Art. 6(1)(c))"),
        VITAL_INTERESTS("Vital Interests (GDPR Art. 6(1)(d))"),
        PUBLIC_TASK("Public Task (GDPR Art. 6(1)(e))"),
        LEGITIMATE_INTERESTS("Legitimate Interests (GDPR Art. 6(1)(f))");

        private final String description;

        LegalBasis(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Composite primary key for ConsentEntity
     */
    @Embeddable
    @Data
    @EqualsAndHashCode
    public static class ConsentId {
        @Column(name = "user_id")
        private UUID userId;

        @Column(name = "consent_key", length = 100)
        private String consentKey;

        public ConsentId() {}

        public ConsentId(UUID userId, String consentKey) {
            this.userId = userId;
            this.consentKey = consentKey;
        }
    }

    // Validation
    @PrePersist
    @PreUpdate
    private void validateEntity() {
        if (consentKey == null || consentKey.trim().isEmpty()) {
            throw new IllegalStateException("Consent key cannot be null or empty");
        }
        
        if (granted == null) {
            throw new IllegalStateException("Granted status cannot be null");
        }
        
        if (grantedAt == null) {
            grantedAt = LocalDateTime.now();
        }
        
        // Ensure withdrawn_at is set when granted is false
        if (!granted && withdrawnAt == null) {
            withdrawnAt = LocalDateTime.now();
        }
        
        // Ensure withdrawn_at is null when granted is true
        if (granted && withdrawnAt != null) {
            withdrawnAt = null;
        }
        
        if (source == null) {
            source = ConsentSource.WEB;
        }
        
        if (legalBasis == null) {
            legalBasis = LegalBasis.CONSENT;
        }
    }

    // Common consent keys (constants)
    public static final class ConsentKeys {
        public static final String MARKETING_EMAILS = "marketing_emails";
        public static final String MARKETING_SMS = "marketing_sms";
        public static final String ANALYTICS = "analytics";
        public static final String PERSONALIZATION = "personalization";
        public static final String THIRD_PARTY_SHARING = "third_party_sharing";
        public static final String DATA_PROCESSING = "data_processing";
        public static final String COOKIES_FUNCTIONAL = "cookies_functional";
        public static final String COOKIES_ANALYTICS = "cookies_analytics";
        public static final String COOKIES_MARKETING = "cookies_marketing";

        // Location and tracking consents (required by Treasure Service)
        public static final String LOCATION_SHARE = "location_share";

        private ConsentKeys() {
            // Utility class
        }
    }
}
