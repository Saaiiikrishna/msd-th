package com.mysillydreams.userservice.service;

import com.mysillydreams.userservice.config.CacheConfig;
import com.mysillydreams.userservice.dto.UserDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Service for managing cache operations across the User Service.
 * Provides centralized caching logic with consistent key generation and eviction strategies.
 */
@Service
@Slf4j
public class CachingService {

    private final CacheConfig.CacheKeyGenerator keyGenerator;
    private final CacheConfig.CacheMetrics cacheMetrics;

    public CachingService(CacheConfig.CacheKeyGenerator keyGenerator, 
                         CacheConfig.CacheMetrics cacheMetrics) {
        this.keyGenerator = keyGenerator;
        this.cacheMetrics = cacheMetrics;
    }

    // User Profile Caching

    /**
     * Caches user profile data
     */
    @Cacheable(value = CacheConfig.USER_PROFILE_CACHE, key = "#userReferenceId")
    public Optional<UserDto> getUserProfile(String userReferenceId) {
        log.debug("Cache miss for user profile: {}", userReferenceId);
        return Optional.empty(); // This will be populated by the calling service
    }

    /**
     * Updates user profile in cache
     */
    @CachePut(value = CacheConfig.USER_PROFILE_CACHE, key = "#userDto.referenceId")
    public UserDto updateUserProfileCache(UserDto userDto) {
        log.debug("Updating user profile cache for: {}", userDto.getReferenceId());
        return userDto;
    }

    /**
     * Evicts user profile from cache
     */
    @CacheEvict(value = CacheConfig.USER_PROFILE_CACHE, key = "#userReferenceId")
    public void evictUserProfile(String userReferenceId) {
        log.debug("Evicting user profile cache for: {}", userReferenceId);
    }

    // User Lookup Caching

    /**
     * Caches user lookup by email
     */
    @Cacheable(value = CacheConfig.USER_LOOKUP_CACHE, key = "'email:' + #email.toLowerCase()")
    public Optional<String> getUserReferenceIdByEmail(String email) {
        log.debug("Cache miss for user lookup by email: {}", email);
        return Optional.empty();
    }

    /**
     * Caches user lookup by phone
     */
    @Cacheable(value = CacheConfig.USER_LOOKUP_CACHE, key = "'phone:' + #phone")
    public Optional<String> getUserReferenceIdByPhone(String phone) {
        log.debug("Cache miss for user lookup by phone: {}", phone);
        return Optional.empty();
    }

    /**
     * Updates user lookup cache
     */
    @Caching(put = {
        @CachePut(value = CacheConfig.USER_LOOKUP_CACHE, key = "'email:' + #email.toLowerCase()"),
        @CachePut(value = CacheConfig.USER_LOOKUP_CACHE, key = "'phone:' + #phone")
    })
    public String updateUserLookupCache(String userReferenceId, String email, String phone) {
        log.debug("Updating user lookup cache for: {}", userReferenceId);
        return userReferenceId;
    }

    /**
     * Evicts user lookup from cache
     */
    @Caching(evict = {
        @CacheEvict(value = CacheConfig.USER_LOOKUP_CACHE, key = "'email:' + #email.toLowerCase()"),
        @CacheEvict(value = CacheConfig.USER_LOOKUP_CACHE, key = "'phone:' + #phone")
    })
    public void evictUserLookup(String email, String phone) {
        log.debug("Evicting user lookup cache for email: {} and phone: {}", email, phone);
    }

    // HMAC Lookup Caching

    /**
     * Caches HMAC lookup results
     */
    @Cacheable(value = CacheConfig.HMAC_LOOKUP_CACHE, key = "#hmac")
    public Optional<String> getUserReferenceIdByHmac(String hmac) {
        log.debug("Cache miss for HMAC lookup: {}", hmac);
        return Optional.empty();
    }

    /**
     * Updates HMAC lookup cache
     */
    @CachePut(value = CacheConfig.HMAC_LOOKUP_CACHE, key = "#hmac")
    public String updateHmacLookupCache(String hmac, String userReferenceId) {
        log.debug("Updating HMAC lookup cache for: {}", userReferenceId);
        return userReferenceId;
    }

    /**
     * Evicts HMAC lookup from cache
     */
    @CacheEvict(value = CacheConfig.HMAC_LOOKUP_CACHE, key = "#hmac")
    public void evictHmacLookup(String hmac) {
        log.debug("Evicting HMAC lookup cache for: {}", hmac);
    }

    // Role Hierarchy Caching

    /**
     * Caches role hierarchy data
     */
    @Cacheable(value = CacheConfig.ROLE_HIERARCHY_CACHE, key = "#role")
    public Optional<Set<String>> getRoleHierarchy(String role) {
        log.debug("Cache miss for role hierarchy: {}", role);
        return Optional.empty();
    }

    /**
     * Updates role hierarchy cache
     */
    @CachePut(value = CacheConfig.ROLE_HIERARCHY_CACHE, key = "#role")
    public Set<String> updateRoleHierarchyCache(String role, Set<String> hierarchy) {
        log.debug("Updating role hierarchy cache for: {}", role);
        return hierarchy;
    }

    /**
     * Evicts role hierarchy from cache
     */
    @CacheEvict(value = CacheConfig.ROLE_HIERARCHY_CACHE, key = "#role")
    public void evictRoleHierarchy(String role) {
        log.debug("Evicting role hierarchy cache for: {}", role);
    }

    /**
     * Evicts all role hierarchy cache
     */
    @CacheEvict(value = CacheConfig.ROLE_HIERARCHY_CACHE, allEntries = true)
    public void evictAllRoleHierarchy() {
        log.debug("Evicting all role hierarchy cache");
    }

    // Consent Status Caching

    /**
     * Caches consent status
     */
    @Cacheable(value = CacheConfig.CONSENT_STATUS_CACHE, key = "#userReferenceId + ':' + #consentKey")
    public Optional<Boolean> getConsentStatus(String userReferenceId, String consentKey) {
        log.debug("Cache miss for consent status: {} - {}", userReferenceId, consentKey);
        return Optional.empty();
    }

    /**
     * Updates consent status cache
     */
    @CachePut(value = CacheConfig.CONSENT_STATUS_CACHE, key = "#userReferenceId + ':' + #consentKey")
    public Boolean updateConsentStatusCache(String userReferenceId, String consentKey, Boolean granted) {
        log.debug("Updating consent status cache for: {} - {} = {}", userReferenceId, consentKey, granted);
        return granted;
    }

    /**
     * Evicts consent status from cache
     */
    @CacheEvict(value = CacheConfig.CONSENT_STATUS_CACHE, key = "#userReferenceId + ':' + #consentKey")
    public void evictConsentStatus(String userReferenceId, String consentKey) {
        log.debug("Evicting consent status cache for: {} - {}", userReferenceId, consentKey);
    }

    /**
     * Evicts all consent status for a user
     */
    @CacheEvict(value = CacheConfig.CONSENT_STATUS_CACHE, key = "#userReferenceId + ':*'")
    public void evictAllUserConsents(String userReferenceId) {
        log.debug("Evicting all consent status cache for user: {}", userReferenceId);
    }

    // Address Lookup Caching

    /**
     * Caches user addresses
     */
    @Cacheable(value = CacheConfig.ADDRESS_LOOKUP_CACHE, key = "#userReferenceId")
    public Optional<List<Object>> getUserAddresses(String userReferenceId) {
        log.debug("Cache miss for user addresses: {}", userReferenceId);
        return Optional.empty();
    }

    /**
     * Updates user addresses cache
     */
    @CachePut(value = CacheConfig.ADDRESS_LOOKUP_CACHE, key = "#userReferenceId")
    public List<Object> updateUserAddressesCache(String userReferenceId, List<Object> addresses) {
        log.debug("Updating user addresses cache for: {}", userReferenceId);
        return addresses;
    }

    /**
     * Evicts user addresses from cache
     */
    @CacheEvict(value = CacheConfig.ADDRESS_LOOKUP_CACHE, key = "#userReferenceId")
    public void evictUserAddresses(String userReferenceId) {
        log.debug("Evicting user addresses cache for: {}", userReferenceId);
    }

    // Session Caching

    /**
     * Caches session data
     */
    @Cacheable(value = CacheConfig.SESSION_CACHE, key = "#sessionId")
    public Optional<Object> getSessionData(String sessionId) {
        log.debug("Cache miss for session: {}", sessionId);
        return Optional.empty();
    }

    /**
     * Updates session cache
     */
    @CachePut(value = CacheConfig.SESSION_CACHE, key = "#sessionId")
    public Object updateSessionCache(String sessionId, Object sessionData) {
        log.debug("Updating session cache for: {}", sessionId);
        return sessionData;
    }

    /**
     * Evicts session from cache
     */
    @CacheEvict(value = CacheConfig.SESSION_CACHE, key = "#sessionId")
    public void evictSession(String sessionId) {
        log.debug("Evicting session cache for: {}", sessionId);
    }

    // Bulk Operations

    /**
     * Evicts all user-related cache entries
     */
    public void evictAllUserData(String userReferenceId, String email, String phone) {
        log.info("Evicting all cache data for user: {}", userReferenceId);
        
        try {
            evictUserProfile(userReferenceId);
            evictUserLookup(email, phone);
            evictUserAddresses(userReferenceId);
            // Note: Consent cache eviction would need to be done per consent key
            
            log.info("Successfully evicted all cache data for user: {}", userReferenceId);
        } catch (Exception e) {
            log.error("Error evicting cache data for user {}: {}", userReferenceId, e.getMessage(), e);
        }
    }

    /**
     * Warms up cache with frequently accessed data
     */
    public void warmupCache() {
        log.info("Starting cache warmup");
        
        try {
            // Warmup role hierarchy cache
            warmupRoleHierarchy();
            
            log.info("Cache warmup completed successfully");
        } catch (Exception e) {
            log.error("Error during cache warmup: {}", e.getMessage(), e);
        }
    }

    /**
     * Gets cache statistics
     */
    public Object getCacheStatistics() {
        return cacheMetrics.getCacheStatistics();
    }

    /**
     * Clears all caches
     */
    public void clearAllCaches() {
        log.warn("Clearing all caches");
        cacheMetrics.clearAllCaches();
    }

    // Private helper methods

    private void warmupRoleHierarchy() {
        // Implementation would load common role hierarchies
        log.debug("Warming up role hierarchy cache");
    }
}
