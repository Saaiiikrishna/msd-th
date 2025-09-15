package com.mysillydreams.userservice.config;

import com.mysillydreams.userservice.encryption.PiiMapper;
import com.mysillydreams.userservice.service.UserLookupService;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Test configuration for User Service tests.
 * Provides mock beans and simplified security configuration for testing.
 */
@TestConfiguration
@Profile("test")
public class TestConfig {

    /**
     * Mock PiiMapper for testing without actual encryption
     */
    @Bean
    @Primary
    public PiiMapper mockPiiMapper() {
        PiiMapper mockMapper = Mockito.mock(PiiMapper.class);
        
        // Default mock behavior
        Mockito.when(mockMapper.encryptPii(Mockito.anyString())).thenAnswer(invocation -> 
            "encrypted_" + invocation.getArgument(0));
        
        Mockito.when(mockMapper.decryptPii(Mockito.anyString())).thenAnswer(invocation -> {
            String encrypted = invocation.getArgument(0);
            return encrypted.startsWith("encrypted_") ? encrypted.substring(10) : encrypted;
        });
        
        Mockito.when(mockMapper.generateHmac(Mockito.anyString())).thenAnswer(invocation -> 
            "hmac_" + invocation.getArgument(0).hashCode());
        
        Mockito.when(mockMapper.isValidEmail(Mockito.anyString())).thenAnswer(invocation -> {
            String email = invocation.getArgument(0);
            return email != null && email.contains("@") && email.contains(".");
        });
        
        Mockito.when(mockMapper.isValidPhone(Mockito.anyString())).thenAnswer(invocation -> {
            String phone = invocation.getArgument(0);
            return phone != null && phone.matches("^[+]?[0-9\\s\\-\\(\\)]{7,15}$");
        });
        
        return mockMapper;
    }

    /**
     * Mock UserLookupService for testing without actual HMAC lookups
     */
    @Bean
    @Primary
    public UserLookupService mockUserLookupService() {
        UserLookupService mockService = Mockito.mock(UserLookupService.class);
        
        // Default mock behavior - no existing users
        Mockito.when(mockService.isEmailRegistered(Mockito.anyString())).thenReturn(false);
        Mockito.when(mockService.isPhoneRegistered(Mockito.anyString())).thenReturn(false);
        
        return mockService;
    }

    /**
     * Simplified security configuration for testing
     */
    @TestConfiguration
    @EnableWebSecurity
    @Profile("test")
    static class TestSecurityConfig {

        @Bean
        public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
            http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(authz -> authz
                    .requestMatchers("/api/v1/health/**").permitAll()
                    .requestMatchers("/actuator/**").permitAll()
                    .anyRequest().authenticated()
                )
                .httpBasic(httpBasic -> {});
            
            return http.build();
        }
    }

    /**
     * Test data builder utilities
     */
    public static class TestDataBuilder {
        
        public static com.mysillydreams.userservice.domain.UserEntity createTestUser(String referenceId) {
            com.mysillydreams.userservice.domain.UserEntity user = new com.mysillydreams.userservice.domain.UserEntity();
            user.setReferenceId(referenceId);
            user.setFirstNameEnc("encrypted_john");
            user.setLastNameEnc("encrypted_doe");
            user.setEmailEnc("encrypted_john.doe@example.com");
            user.setPhoneEnc("encrypted_+1234567890");
            user.setEmailHmac("hmac_email_123");
            user.setPhoneHmac("hmac_phone_123");
            user.setGender("MALE");
            user.setActive(true);
            user.setCreatedAt(java.time.LocalDateTime.now());
            user.setUpdatedAt(java.time.LocalDateTime.now());
            return user;
        }
        
        public static com.mysillydreams.userservice.dto.UserDto createTestUserDto(String referenceId) {
            com.mysillydreams.userservice.dto.UserDto dto = new com.mysillydreams.userservice.dto.UserDto();
            dto.setReferenceId(referenceId);
            dto.setFirstName("John");
            dto.setLastName("Doe");
            dto.setEmail("john.doe@example.com");
            dto.setPhone("+1234567890");
            dto.setGender("MALE");
            dto.setActive(true);
            dto.setCreatedAt(java.time.LocalDateTime.now());
            dto.setUpdatedAt(java.time.LocalDateTime.now());
            return dto;
        }
        
        public static com.mysillydreams.userservice.dto.UserCreateRequestDto createTestUserCreateRequest() {
            com.mysillydreams.userservice.dto.UserCreateRequestDto request = new com.mysillydreams.userservice.dto.UserCreateRequestDto();
            request.setFirstName("John");
            request.setLastName("Doe");
            request.setEmail("john.doe@example.com");
            request.setPhone("+1234567890");
            request.setGender("MALE");
            return request;
        }
        
        public static com.mysillydreams.userservice.dto.AddressDto createTestAddressDto() {
            com.mysillydreams.userservice.dto.AddressDto address = new com.mysillydreams.userservice.dto.AddressDto();
            address.setType("HOME");
            address.setLine1("123 Main Street");
            address.setCity("New York");
            address.setState("NY");
            address.setPostalCode("10001");
            address.setCountry("United States");
            address.setIsPrimary(true);
            return address;
        }
        
        public static com.mysillydreams.userservice.dto.ConsentDto createTestConsentDto(String consentKey, boolean granted) {
            com.mysillydreams.userservice.dto.ConsentDto consent = new com.mysillydreams.userservice.dto.ConsentDto();
            consent.setConsentKey(consentKey);
            consent.setGranted(granted);
            consent.setConsentVersion("v1.0");
            consent.setSource("WEB");
            consent.setLegalBasis("CONSENT");
            if (granted) {
                consent.setGrantedAt(java.time.LocalDateTime.now());
            } else {
                consent.setWithdrawnAt(java.time.LocalDateTime.now());
            }
            return consent;
        }
    }

    /**
     * Test assertion utilities
     */
    public static class TestAssertions {
        
        public static void assertUserDtoEquals(com.mysillydreams.userservice.dto.UserDto expected, 
                                             com.mysillydreams.userservice.dto.UserDto actual) {
            org.junit.jupiter.api.Assertions.assertEquals(expected.getReferenceId(), actual.getReferenceId());
            org.junit.jupiter.api.Assertions.assertEquals(expected.getFirstName(), actual.getFirstName());
            org.junit.jupiter.api.Assertions.assertEquals(expected.getLastName(), actual.getLastName());
            org.junit.jupiter.api.Assertions.assertEquals(expected.getEmail(), actual.getEmail());
            org.junit.jupiter.api.Assertions.assertEquals(expected.getPhone(), actual.getPhone());
            org.junit.jupiter.api.Assertions.assertEquals(expected.getGender(), actual.getGender());
            org.junit.jupiter.api.Assertions.assertEquals(expected.getActive(), actual.getActive());
        }
        
        public static void assertAddressDtoEquals(com.mysillydreams.userservice.dto.AddressDto expected, 
                                                com.mysillydreams.userservice.dto.AddressDto actual) {
            org.junit.jupiter.api.Assertions.assertEquals(expected.getType(), actual.getType());
            org.junit.jupiter.api.Assertions.assertEquals(expected.getLine1(), actual.getLine1());
            org.junit.jupiter.api.Assertions.assertEquals(expected.getLine2(), actual.getLine2());
            org.junit.jupiter.api.Assertions.assertEquals(expected.getCity(), actual.getCity());
            org.junit.jupiter.api.Assertions.assertEquals(expected.getState(), actual.getState());
            org.junit.jupiter.api.Assertions.assertEquals(expected.getPostalCode(), actual.getPostalCode());
            org.junit.jupiter.api.Assertions.assertEquals(expected.getCountry(), actual.getCountry());
            org.junit.jupiter.api.Assertions.assertEquals(expected.getIsPrimary(), actual.getIsPrimary());
        }
        
        public static void assertConsentDtoEquals(com.mysillydreams.userservice.dto.ConsentDto expected, 
                                                com.mysillydreams.userservice.dto.ConsentDto actual) {
            org.junit.jupiter.api.Assertions.assertEquals(expected.getConsentKey(), actual.getConsentKey());
            org.junit.jupiter.api.Assertions.assertEquals(expected.getGranted(), actual.getGranted());
            org.junit.jupiter.api.Assertions.assertEquals(expected.getConsentVersion(), actual.getConsentVersion());
            org.junit.jupiter.api.Assertions.assertEquals(expected.getSource(), actual.getSource());
            org.junit.jupiter.api.Assertions.assertEquals(expected.getLegalBasis(), actual.getLegalBasis());
        }
    }

    /**
     * Test constants
     */
    public static class TestConstants {
        public static final String TEST_USER_REFERENCE_ID = "USR12345678";
        public static final String TEST_EMAIL = "test@example.com";
        public static final String TEST_PHONE = "+1234567890";
        public static final String TEST_FIRST_NAME = "John";
        public static final String TEST_LAST_NAME = "Doe";
        public static final String TEST_GENDER = "MALE";
        
        public static final String ADMIN_ROLE = "ROLE_ADMIN";
        public static final String CUSTOMER_ROLE = "ROLE_CUSTOMER";
        public static final String SUPPORT_USER_ROLE = "ROLE_SUPPORT_USER";
        
        public static final String CONSENT_MARKETING_EMAILS = "marketing_emails";
        public static final String CONSENT_MARKETING_SMS = "marketing_sms";
        public static final String CONSENT_ANALYTICS = "analytics";
        
        public static final String ADDRESS_TYPE_HOME = "HOME";
        public static final String ADDRESS_TYPE_WORK = "WORK";
        public static final String ADDRESS_TYPE_BILLING = "BILLING";
        public static final String ADDRESS_TYPE_SHIPPING = "SHIPPING";
    }
}
