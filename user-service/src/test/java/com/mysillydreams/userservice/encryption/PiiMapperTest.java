package com.mysillydreams.userservice.encryption;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PiiMapperTest {

    @Mock
    private EncryptionService encryptionService;

    private PiiMapper piiMapper;

    @BeforeEach
    void setUp() {
        piiMapper = new PiiMapper(encryptionService);
    }

    @Test
    void shouldProcessEmailForStorage() {
        // Given
        String email = "test@example.com";
        String normalizedEmail = "test@example.com";
        String encryptedEmail = "encrypted_email";
        String emailHmac = "email_hmac";

        when(encryptionService.normalizeEmail(email)).thenReturn(normalizedEmail);
        when(encryptionService.encrypt(normalizedEmail)).thenReturn(encryptedEmail);
        when(encryptionService.generateEmailHmac(email)).thenReturn(emailHmac);

        // When
        PiiMapper.PiiData result = piiMapper.processEmailForStorage(email);

        // Then
        assertNotNull(result);
        assertEquals(encryptedEmail, result.getEncryptedValue());
        assertEquals(emailHmac, result.getHmacValue());

        verify(encryptionService).normalizeEmail(email);
        verify(encryptionService).encrypt(normalizedEmail);
        verify(encryptionService).generateEmailHmac(email);
    }

    @Test
    void shouldProcessPhoneForStorage() {
        // Given
        String phone = "+91-9876543210";
        String normalizedPhone = "+919876543210";
        String encryptedPhone = "encrypted_phone";
        String phoneHmac = "phone_hmac";

        when(encryptionService.normalizePhone(phone)).thenReturn(normalizedPhone);
        when(encryptionService.encrypt(normalizedPhone)).thenReturn(encryptedPhone);
        when(encryptionService.generatePhoneHmac(phone)).thenReturn(phoneHmac);

        // When
        PiiMapper.PiiData result = piiMapper.processPhoneForStorage(phone);

        // Then
        assertNotNull(result);
        assertEquals(encryptedPhone, result.getEncryptedValue());
        assertEquals(phoneHmac, result.getHmacValue());

        verify(encryptionService).normalizePhone(phone);
        verify(encryptionService).encrypt(normalizedPhone);
        verify(encryptionService).generatePhoneHmac(phone);
    }

    @Test
    void shouldProcessGeneralPiiForStorage() {
        // Given
        String piiData = "John Doe";
        String encryptedData = "encrypted_john_doe";

        when(encryptionService.encrypt("John Doe")).thenReturn(encryptedData);

        // When
        String result = piiMapper.processPiiForStorage(piiData);

        // Then
        assertEquals(encryptedData, result);
        verify(encryptionService).encrypt("John Doe");
    }

    @Test
    void shouldDecryptPii() {
        // Given
        String encryptedData = "encrypted_data";
        String decryptedData = "decrypted_data";

        when(encryptionService.decrypt(encryptedData)).thenReturn(decryptedData);

        // When
        String result = piiMapper.decryptPii(encryptedData);

        // Then
        assertEquals(decryptedData, result);
        verify(encryptionService).decrypt(encryptedData);
    }

    @Test
    void shouldGenerateEmailLookupHmac() {
        // Given
        String email = "Test@Example.Com";
        String emailHmac = "email_hmac";

        when(encryptionService.generateEmailHmac(email)).thenReturn(emailHmac);

        // When
        String result = piiMapper.generateEmailLookupHmac(email);

        // Then
        assertEquals(emailHmac, result);
        verify(encryptionService).generateEmailHmac(email);
    }

    @Test
    void shouldGeneratePhoneLookupHmac() {
        // Given
        String phone = "9876543210";
        String phoneHmac = "phone_hmac";

        when(encryptionService.generatePhoneHmac(phone)).thenReturn(phoneHmac);

        // When
        String result = piiMapper.generatePhoneLookupHmac(phone);

        // Then
        assertEquals(phoneHmac, result);
        verify(encryptionService).generatePhoneHmac(phone);
    }

    @Test
    void shouldMaskEmail() {
        // Given
        String email = "john.doe@example.com";

        // When
        String result = piiMapper.maskEmail(email);

        // Then
        assertEquals("jo***@e***.com", result);
    }

    @Test
    void shouldMaskShortEmail() {
        // Given
        String email = "a@b.co";

        // When
        String result = piiMapper.maskEmail(email);

        // Then
        assertEquals("***@b***.co", result);
    }

    @Test
    void shouldMaskPhone() {
        // Given
        String phone = "+919876543210";

        when(encryptionService.normalizePhone(phone)).thenReturn(phone);

        // When
        String result = piiMapper.maskPhone(phone);

        // Then
        assertEquals("+91*******3210", result);
    }

    @Test
    void shouldMaskGeneralPii() {
        // Given
        String piiData = "John Doe";

        // When
        String result = piiMapper.maskPii(piiData);

        // Then
        assertEquals("Jo******", result);
    }

    @Test
    void shouldMaskShortPii() {
        // Given
        String piiData = "Jo";

        // When
        String result = piiMapper.maskPii(piiData);

        // Then
        assertEquals("**", result);
    }

    @Test
    void shouldValidateEmail() {
        // Valid emails
        assertTrue(piiMapper.isValidEmail("test@example.com"));
        assertTrue(piiMapper.isValidEmail("user.name+tag@domain.co.uk"));
        assertTrue(piiMapper.isValidEmail("test123@test-domain.org"));

        // Invalid emails
        assertFalse(piiMapper.isValidEmail("invalid-email"));
        assertFalse(piiMapper.isValidEmail("@example.com"));
        assertFalse(piiMapper.isValidEmail("test@"));
        assertFalse(piiMapper.isValidEmail("test.example.com"));
        assertFalse(piiMapper.isValidEmail(null));
        assertFalse(piiMapper.isValidEmail(""));
    }

    @Test
    void shouldValidatePhone() {
        // Valid phones
        assertTrue(piiMapper.isValidPhone("+919876543210"));
        assertTrue(piiMapper.isValidPhone("9876543210"));
        assertTrue(piiMapper.isValidPhone("+1-555-123-4567"));
        assertTrue(piiMapper.isValidPhone("(555) 123-4567"));

        // Invalid phones
        assertFalse(piiMapper.isValidPhone("123"));
        assertFalse(piiMapper.isValidPhone("abcdefghij"));
        assertFalse(piiMapper.isValidPhone(null));
        assertFalse(piiMapper.isValidPhone(""));
    }

    @Test
    void shouldHandleNullAndEmptyValues() {
        // Email processing
        PiiMapper.PiiData emailResult = piiMapper.processEmailForStorage(null);
        assertNotNull(emailResult);
        assertNull(emailResult.getEncryptedValue());
        assertNull(emailResult.getHmacValue());

        emailResult = piiMapper.processEmailForStorage("");
        assertNotNull(emailResult);
        assertNull(emailResult.getEncryptedValue());
        assertNull(emailResult.getHmacValue());

        // Phone processing
        PiiMapper.PiiData phoneResult = piiMapper.processPhoneForStorage(null);
        assertNotNull(phoneResult);
        assertNull(phoneResult.getEncryptedValue());
        assertNull(phoneResult.getHmacValue());

        // General PII processing
        assertNull(piiMapper.processPiiForStorage(null));
        assertNull(piiMapper.processPiiForStorage(""));
        assertNull(piiMapper.processPiiForStorage("   "));

        // Decryption
        assertNull(piiMapper.decryptPii(null));
        assertNull(piiMapper.decryptPii(""));

        // HMAC generation
        assertNull(piiMapper.generateEmailLookupHmac(null));
        assertNull(piiMapper.generatePhoneLookupHmac(null));

        // Masking
        assertNull(piiMapper.maskEmail(null));
        assertNull(piiMapper.maskPhone(null));
        assertNull(piiMapper.maskPii(null));
    }

    @Test
    void shouldThrowExceptionForInvalidEmailFormat() {
        // Given
        String invalidEmail = "invalid-email";

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            piiMapper.processEmailForStorage(invalidEmail);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            piiMapper.generateEmailLookupHmac(invalidEmail);
        });
    }

    @Test
    void shouldThrowExceptionForInvalidPhoneFormat() {
        // Given
        String invalidPhone = "abc";

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            piiMapper.processPhoneForStorage(invalidPhone);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            piiMapper.generatePhoneLookupHmac(invalidPhone);
        });
    }

    @Test
    void shouldBatchProcessUserPii() {
        // Given
        String firstName = "John";
        String lastName = "Doe";
        String email = "john@example.com";
        String phone = "9876543210";
        String dob = "1990-01-01";

        when(encryptionService.encrypt(anyString())).thenReturn("encrypted");
        when(encryptionService.normalizeEmail(email)).thenReturn(email);
        when(encryptionService.normalizePhone(phone)).thenReturn("+919876543210");
        when(encryptionService.generateEmailHmac(email)).thenReturn("email_hmac");
        when(encryptionService.generatePhoneHmac(phone)).thenReturn("phone_hmac");

        // When
        PiiMapper.BatchPiiResult result = piiMapper.batchProcessUserPii(firstName, lastName, email, phone, dob);

        // Then
        assertNotNull(result);
        assertEquals("encrypted", result.getEncryptedFirstName());
        assertEquals("encrypted", result.getEncryptedLastName());
        assertEquals("encrypted", result.getEmailData().getEncryptedValue());
        assertEquals("email_hmac", result.getEmailData().getHmacValue());
        assertEquals("encrypted", result.getPhoneData().getEncryptedValue());
        assertEquals("phone_hmac", result.getPhoneData().getHmacValue());
        assertEquals("encrypted", result.getEncryptedDob());
    }

    @Test
    void shouldSafelyProcessPiiWithErrorHandling() {
        // Given
        String piiData = "test data";
        when(encryptionService.encrypt(piiData)).thenThrow(new RuntimeException("Encryption failed"));
        when(encryptionService.decrypt("encrypted_data")).thenThrow(new RuntimeException("Decryption failed"));

        // When & Then
        assertNull(piiMapper.safeProcessPii(piiData, true)); // Encryption failure returns null
        assertEquals("encrypted_data", piiMapper.safeProcessPii("encrypted_data", false)); // Decryption failure returns original
    }
}
