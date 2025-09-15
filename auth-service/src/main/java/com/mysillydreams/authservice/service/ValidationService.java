package com.mysillydreams.authservice.service;

import org.springframework.stereotype.Service;

/**
 * Business Logic Validation Service
 *
 * SINGLE RESPONSIBILITY: Handle ONLY custom business validation rules
 *
 * This service handles validation that goes beyond what Keycloak provides:
 * - Custom business rules (e.g., user eligibility, business constraints)
 * - Complex cross-system validation logic
 * - Domain-specific validation rules
 *
 * IMPORTANT: Basic input validation (email format, password policy, required fields, etc.)
 * is handled by Keycloak. Do NOT duplicate Keycloak's validation here.
 *
 * Use this service ONLY for business logic that Keycloak cannot handle.
 */
@Service
public class ValidationService {

    /**
     * Validate business rules for user creation
     *
     * Example business rules (customize based on your needs):
     * - User eligibility based on domain
     * - Account limits
     * - Business-specific constraints
     *
     * @param email User email
     * @param firstName User first name
     * @param lastName User last name
     * @throws RuntimeException if business rules are violated
     */
    public void validateUserCreationBusinessRules(String email, String firstName, String lastName) {
        // Example: Block certain email domains for business reasons
        if (email != null && email.endsWith("@blocked-domain.com")) {
            throw new RuntimeException("Email domain not allowed for registration");
        }

        // Example: Check for prohibited names (business rule)
        if (firstName != null && firstName.equalsIgnoreCase("admin")) {
            throw new RuntimeException("Username 'admin' is reserved");
        }

        // Add your custom business validation rules here
        // Do NOT add basic validation (email format, etc.) - Keycloak handles that
    }

    /**
     * Validate user eligibility for specific operations
     *
     * Example: Check if user can perform certain actions based on business rules
     */
    public void validateUserEligibility(String userRef, String operation) {
        // Add business logic validation here
        // Example: Check user status, subscription level, etc.
    }
}
