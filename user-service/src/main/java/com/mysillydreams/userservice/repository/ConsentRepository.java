package com.mysillydreams.userservice.repository;

import com.mysillydreams.userservice.domain.ConsentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for ConsentEntity operations.
 * Provides methods for GDPR/DPDP consent management and queries.
 */
@Repository
public interface ConsentRepository extends JpaRepository<ConsentEntity, ConsentEntity.ConsentId> {

    /**
     * Find all consents for a specific user
     */
    List<ConsentEntity> findByUserId(UUID userId);

    /**
     * Find all granted consents for a user
     */
    List<ConsentEntity> findByUserIdAndGrantedTrue(UUID userId);

    /**
     * Find all withdrawn consents for a user
     */
    List<ConsentEntity> findByUserIdAndGrantedFalse(UUID userId);

    /**
     * Find specific consent for a user
     */
    Optional<ConsentEntity> findByUserIdAndConsentKey(UUID userId, String consentKey);

    /**
     * Find all users who granted a specific consent
     */
    List<ConsentEntity> findByConsentKeyAndGrantedTrue(String consentKey);

    /**
     * Find all users who withdrew a specific consent
     */
    List<ConsentEntity> findByConsentKeyAndGrantedFalse(String consentKey);

    /**
     * Check if user has granted a specific consent
     */
    boolean existsByUserIdAndConsentKeyAndGrantedTrue(UUID userId, String consentKey);

    /**
     * Check if user has any consent record for a specific key
     */
    boolean existsByUserIdAndConsentKey(UUID userId, String consentKey);

    /**
     * Count granted consents for a specific consent key
     */
    long countByConsentKeyAndGrantedTrue(String consentKey);

    /**
     * Count withdrawn consents for a specific consent key
     */
    long countByConsentKeyAndGrantedFalse(String consentKey);

    /**
     * Find consents granted within a date range
     */
    @Query("SELECT c FROM ConsentEntity c WHERE c.grantedAt >= :startDate AND c.grantedAt <= :endDate AND c.granted = true ORDER BY c.grantedAt DESC")
    List<ConsentEntity> findConsentsGrantedBetween(@Param("startDate") LocalDateTime startDate,
                                                   @Param("endDate") LocalDateTime endDate);

    /**
     * Find consents withdrawn within a date range
     */
    @Query("SELECT c FROM ConsentEntity c WHERE c.withdrawnAt >= :startDate AND c.withdrawnAt <= :endDate AND c.granted = false ORDER BY c.withdrawnAt DESC")
    List<ConsentEntity> findConsentsWithdrawnBetween(@Param("startDate") LocalDateTime startDate,
                                                     @Param("endDate") LocalDateTime endDate);

    /**
     * Find all distinct consent keys in the system
     */
    @Query(value = "SELECT DISTINCT consent_key FROM consents", nativeQuery = true)
    List<String> findAllConsentKeys();

    /**
     * Find consents by source
     */
    @Query(value = "SELECT * FROM consents WHERE source = :source", nativeQuery = true)
    List<ConsentEntity> findBySource(@Param("source") String source);

    /**
     * Find consents by legal basis
     */
    @Query(value = "SELECT * FROM consents WHERE legal_basis = :legalBasis", nativeQuery = true)
    List<ConsentEntity> findByLegalBasis(@Param("legalBasis") String legalBasis);

    /**
     * Find consents by version
     */
    List<ConsentEntity> findByConsentVersion(String consentVersion);

    /**
     * Find users with incomplete consent (missing required consents)
     */
    @Query(value = "SELECT DISTINCT u.id FROM users u WHERE u.active = true AND NOT EXISTS " +
           "(SELECT 1 FROM consents c WHERE c.user_id = u.id AND c.consent_key = :requiredConsentKey AND c.granted = true)", nativeQuery = true)
    List<UUID> findUsersWithoutRequiredConsent(@Param("requiredConsentKey") String requiredConsentKey);

    /**
     * Find users who granted all marketing consents
     */
    @Query(value = "SELECT user_id FROM consents WHERE consent_key IN ('marketing_emails', 'marketing_sms') AND granted = true " +
           "GROUP BY user_id HAVING COUNT(DISTINCT consent_key) = 2", nativeQuery = true)
    List<UUID> findUsersWithAllMarketingConsents();

    /**
     * Find users who withdrew any marketing consent
     */
    @Query(value = "SELECT DISTINCT user_id FROM consents WHERE consent_key IN ('marketing_emails', 'marketing_sms') AND granted = false", nativeQuery = true)
    List<UUID> findUsersWithWithdrawnMarketingConsents();

    /**
     * Get consent statistics by key
     */
    @Query(value = "SELECT consent_key, " +
           "SUM(CASE WHEN granted = true THEN 1 ELSE 0 END) as granted, " +
           "SUM(CASE WHEN granted = false THEN 1 ELSE 0 END) as withdrawn " +
           "FROM consents GROUP BY consent_key", nativeQuery = true)
    List<Object[]> getConsentStatistics();

    /**
     * Find consents that need to be refreshed (old versions)
     */
    @Query(value = "SELECT * FROM consents WHERE consent_version != :currentVersion AND granted = true", nativeQuery = true)
    List<ConsentEntity> findConsentsNeedingRefresh(@Param("currentVersion") String currentVersion);

    /**
     * Delete all consents for a user (for user deletion)
     */
    void deleteByUserId(UUID userId);

    /**
     * Find recent consent changes for audit
     */
    @Query(value = "SELECT * FROM consents WHERE granted_at >= :since OR withdrawn_at >= :since ORDER BY GREATEST(granted_at, COALESCE(withdrawn_at, granted_at)) DESC", nativeQuery = true)
    List<ConsentEntity> findRecentConsentChanges(@Param("since") LocalDateTime since);

    /**
     * Find consents by IP address (for fraud detection)
     */
    List<ConsentEntity> findByIpAddress(java.net.InetAddress ipAddress);

    /**
     * Count consents by source
     */
    @Query(value = "SELECT source, COUNT(*) FROM consents GROUP BY source", nativeQuery = true)
    List<Object[]> countConsentsBySource();

    /**
     * Count consents by legal basis
     */
    @Query(value = "SELECT legal_basis, COUNT(*) FROM consents GROUP BY legal_basis", nativeQuery = true)
    List<Object[]> countConsentsByLegalBasis();
}
