package com.mysillydreams.userservice.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.stereotype.Component;

/**
 * Configuration for role hierarchy and method-level security.
 * Defines the hierarchical relationship between roles and enables method security.
 */
@Configuration
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true, jsr250Enabled = true)
public class RoleHierarchyConfig {

    /**
     * Defines the role hierarchy for the User Service.
     * Higher roles inherit permissions from lower roles.
     * 
     * Hierarchy (from highest to lowest):
     * 1. ROLE_ADMIN - Full system access
     * 2. ROLE_SUPPORT_USER - Support operations
     * 3. ROLE_VENDOR - Vendor-specific operations  
     * 4. ROLE_INVENTORY_USER - Inventory management
     * 5. ROLE_DELIVERY_USER - Delivery operations
     * 6. ROLE_CUSTOMER - Basic user operations
     * 7. ROLE_INTERNAL_CONSUMER - Service-to-service operations
     * 8. ROLE_SERVICE_USER_LOOKUP - User lookup operations
     */
    @Bean
    public RoleHierarchy roleHierarchy() {
        RoleHierarchyImpl hierarchy = new RoleHierarchyImpl();
        
        // Define role hierarchy using Spring Security's format
        // Format: "HIGHER_ROLE > LOWER_ROLE"
        String roleHierarchyString = """
            ROLE_ADMIN > ROLE_SUPPORT_USER
            ROLE_ADMIN > ROLE_VENDOR
            ROLE_ADMIN > ROLE_INVENTORY_USER
            ROLE_ADMIN > ROLE_DELIVERY_USER
            ROLE_ADMIN > ROLE_CUSTOMER
            ROLE_ADMIN > ROLE_INTERNAL_CONSUMER
            ROLE_ADMIN > ROLE_SERVICE_USER_LOOKUP
            ROLE_SUPPORT_USER > ROLE_CUSTOMER
            ROLE_SUPPORT_USER > ROLE_SERVICE_USER_LOOKUP
            ROLE_VENDOR > ROLE_CUSTOMER
            ROLE_INVENTORY_USER > ROLE_CUSTOMER
            ROLE_DELIVERY_USER > ROLE_CUSTOMER
            ROLE_INTERNAL_CONSUMER > ROLE_SERVICE_USER_LOOKUP
            """;
        
        hierarchy.setHierarchy(roleHierarchyString);
        return hierarchy;
    }

    /**
     * Role constants for use throughout the application
     */
    public static final class Roles {
        public static final String ADMIN = "ROLE_ADMIN";
        public static final String SUPPORT_USER = "ROLE_SUPPORT_USER";
        public static final String VENDOR = "ROLE_VENDOR";
        public static final String INVENTORY_USER = "ROLE_INVENTORY_USER";
        public static final String DELIVERY_USER = "ROLE_DELIVERY_USER";
        public static final String CUSTOMER = "ROLE_CUSTOMER";
        public static final String INTERNAL_CONSUMER = "ROLE_INTERNAL_CONSUMER";
        public static final String SERVICE_USER_LOOKUP = "ROLE_SERVICE_USER_LOOKUP";

        private Roles() {
            // Utility class
        }
    }

    /**
     * Permission constants for fine-grained access control
     */
    public static final class Permissions {
        // User management permissions
        public static final String USER_READ = "user:read";
        public static final String USER_WRITE = "user:write";
        public static final String USER_DELETE = "user:delete";
        public static final String USER_ADMIN = "user:admin";
        
        // Address management permissions
        public static final String ADDRESS_READ = "address:read";
        public static final String ADDRESS_WRITE = "address:write";
        public static final String ADDRESS_DELETE = "address:delete";
        
        // Consent management permissions
        public static final String CONSENT_READ = "consent:read";
        public static final String CONSENT_WRITE = "consent:write";
        public static final String CONSENT_ADMIN = "consent:admin";
        
        // Role management permissions
        public static final String ROLE_READ = "role:read";
        public static final String ROLE_ASSIGN = "role:assign";
        public static final String ROLE_ADMIN = "role:admin";
        
        // Session management permissions
        public static final String SESSION_READ = "session:read";
        public static final String SESSION_ADMIN = "session:admin";
        
        // Audit and compliance permissions
        public static final String AUDIT_READ = "audit:read";
        public static final String AUDIT_ADMIN = "audit:admin";
        public static final String COMPLIANCE_READ = "compliance:read";
        public static final String COMPLIANCE_ADMIN = "compliance:admin";
        
        // Internal service permissions
        public static final String INTERNAL_USER_LOOKUP = "internal:user:lookup";
        public static final String INTERNAL_BULK_OPERATIONS = "internal:bulk:operations";

        private Permissions() {
            // Utility class
        }
    }

    /**
     * Security expressions for use in @PreAuthorize annotations
     */
    public static final class SecurityExpressions {
        
        // User access expressions
        public static final String IS_ADMIN = "hasRole('ADMIN')";
        public static final String IS_SUPPORT_OR_ADMIN = "hasAnyRole('SUPPORT_USER', 'ADMIN')";
        public static final String IS_INTERNAL_CONSUMER = "hasRole('INTERNAL_CONSUMER')";
        
        // Resource ownership expressions
        public static final String IS_OWNER_OR_ADMIN =
            "hasRole('ADMIN') or @securityUtils.isOwner(#referenceId)";

        public static final String IS_OWNER_OR_SUPPORT =
            "hasAnyRole('SUPPORT_USER', 'ADMIN') or @securityUtils.isOwner(#referenceId)";
        
        // Service access expressions
        public static final String CAN_LOOKUP_USERS = "hasAnyRole('SERVICE_USER_LOOKUP', 'INTERNAL_CONSUMER', 'ADMIN')";
        public static final String CAN_BULK_OPERATIONS = "hasAnyRole('INTERNAL_CONSUMER', 'ADMIN')";
        
        // Consent management expressions
        public static final String CAN_MANAGE_CONSENTS =
            "hasRole('ADMIN') or @securityUtils.isOwner(#referenceId)";

        // Address management expressions
        public static final String CAN_MANAGE_ADDRESSES =
            "hasRole('ADMIN') or @securityUtils.isOwner(#referenceId)";
        
        // Role management expressions
        public static final String CAN_ASSIGN_ROLES = "hasRole('ADMIN')";
        public static final String CAN_VIEW_ROLES = "hasAnyRole('SUPPORT_USER', 'ADMIN')";
        
        // Audit access expressions
        public static final String CAN_VIEW_AUDIT = "hasAnyRole('SUPPORT_USER', 'ADMIN')";
        public static final String CAN_MANAGE_AUDIT = "hasRole('ADMIN')";

        private SecurityExpressions() {
            // Utility class
        }
    }

    /**
     * Bean for security utilities that can be used in @PreAuthorize expressions
     */
    @Bean
    public SecurityUtils securityUtils() {
        return new SecurityUtils();
    }

    /**
     * Utility methods for role and permission checking
     */
    @Component
    public static class SecurityUtils {
        
        /**
         * Checks if the current user has admin role
         */
        public static boolean isAdmin() {
            return org.springframework.security.core.context.SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getAuthorities()
                .stream()
                .anyMatch(auth -> Roles.ADMIN.equals(auth.getAuthority()));
        }
        
        /**
         * Checks if the current user has support role or higher
         */
        public static boolean isSupportOrAdmin() {
            var authorities = org.springframework.security.core.context.SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getAuthorities();
            
            return authorities.stream()
                .anyMatch(auth -> Roles.ADMIN.equals(auth.getAuthority()) || 
                                Roles.SUPPORT_USER.equals(auth.getAuthority()));
        }
        
        /**
         * Gets the current user's reference ID if available
         */
        public static String getCurrentUserReferenceId() {
            var authentication = org.springframework.security.core.context.SecurityContextHolder
                .getContext()
                .getAuthentication();

            if (authentication != null && authentication.getPrincipal() instanceof org.springframework.security.oauth2.jwt.Jwt jwt) {
                // Extract user_ref from JWT claims
                return jwt.getClaimAsString("user_ref");
            }

            return null;
        }
        
        /**
         * Gets the current user's ID if available
         */
        public static java.util.UUID getCurrentUserId() {
            var authentication = org.springframework.security.core.context.SecurityContextHolder
                .getContext()
                .getAuthentication();

            if (authentication != null && authentication.getPrincipal() instanceof org.springframework.security.oauth2.jwt.Jwt jwt) {
                // Extract user ID from JWT claims (typically 'sub' claim)
                String userId = jwt.getClaimAsString("sub");
                if (userId != null) {
                    try {
                        return java.util.UUID.fromString(userId);
                    } catch (IllegalArgumentException e) {
                        // If sub is not a UUID, try user_id claim
                        String userIdClaim = jwt.getClaimAsString("user_id");
                        if (userIdClaim != null) {
                            return java.util.UUID.fromString(userIdClaim);
                        }
                    }
                }
            }

            return null;
        }
        
        /**
         * Checks if the current user is the owner of a resource
         */
        public boolean isOwner(String resourceUserReferenceId) {
            String currentUserRef = getCurrentUserReferenceId();
            return currentUserRef != null && currentUserRef.equals(resourceUserReferenceId);
        }
        
        /**
         * Checks if the current user can access a resource (owner, support, or admin)
         */
        public boolean canAccessResource(String resourceUserReferenceId) {
            return isAdmin() || isSupportOrAdmin() || isOwner(resourceUserReferenceId);
        }

        private SecurityUtils() {
            // Utility class
        }
    }
}
