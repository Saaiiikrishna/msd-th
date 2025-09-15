package com.mysillydreams.userservice.repository;

import com.mysillydreams.userservice.domain.UserEntity;
import com.mysillydreams.userservice.domain.UserRoleEntity;
import com.mysillydreams.userservice.security.RoleHierarchyConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Repository tests for UserRepository.
 * Tests database operations with in-memory H2 database.
 */
@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    private UserEntity testUser;
    private String testReferenceId;

    @BeforeEach
    void setUp() {
        testReferenceId = "USR12345678";

        testUser = new UserEntity();
        testUser.setReferenceId(testReferenceId);
        testUser.setFirstNameEnc("encrypted_john");
        testUser.setLastNameEnc("encrypted_doe");
        testUser.setEmailEnc("encrypted_email");
        testUser.setPhoneEnc("encrypted_phone");
        testUser.setEmailHmac("email_hmac_123");
        testUser.setPhoneHmac("phone_hmac_123");
        testUser.setGender("MALE");
        testUser.setActive(true);
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setUpdatedAt(LocalDateTime.now());

        // Add a role
        UserRoleEntity role = new UserRoleEntity(testUser, RoleHierarchyConfig.Roles.CUSTOMER);
        testUser.addRole(role);
    }

    @Test
    void save_ValidUser_SavesSuccessfully() {
        // When
        UserEntity savedUser = userRepository.save(testUser);

        // Then
        assertNotNull(savedUser.getId());
        assertEquals(testReferenceId, savedUser.getReferenceId());
        assertEquals("encrypted_john", savedUser.getFirstNameEnc());
        assertEquals("encrypted_email", savedUser.getEmailEnc());
        assertTrue(savedUser.getActive());
        assertNotNull(savedUser.getCreatedAt());
        assertNotNull(savedUser.getUpdatedAt());
    }

    @Test
    void findByReferenceId_ExistingUser_ReturnsUser() {
        // Given
        entityManager.persistAndFlush(testUser);

        // When
        Optional<UserEntity> found = userRepository.findByReferenceId(testReferenceId);

        // Then
        assertTrue(found.isPresent());
        assertEquals(testReferenceId, found.get().getReferenceId());
        assertEquals("encrypted_john", found.get().getFirstNameEnc());
    }

    @Test
    void findByReferenceId_NonExistingUser_ReturnsEmpty() {
        // When
        Optional<UserEntity> found = userRepository.findByReferenceId("NONEXISTENT");

        // Then
        assertFalse(found.isPresent());
    }

    @Test
    void findByReferenceIdAndActiveTrue_ActiveUser_ReturnsUser() {
        // Given
        entityManager.persistAndFlush(testUser);

        // When
        Optional<UserEntity> found = userRepository.findByReferenceIdAndActiveTrue(testReferenceId);

        // Then
        assertTrue(found.isPresent());
        assertEquals(testReferenceId, found.get().getReferenceId());
        assertTrue(found.get().getActive());
    }

    @Test
    void findByReferenceIdAndActiveTrue_InactiveUser_ReturnsEmpty() {
        // Given
        testUser.setActive(false);
        testUser.setDeletedAt(LocalDateTime.now());
        entityManager.persistAndFlush(testUser);

        // When
        Optional<UserEntity> found = userRepository.findByReferenceIdAndActiveTrue(testReferenceId);

        // Then
        assertFalse(found.isPresent());
    }

    @Test
    void findByEmailHmac_ExistingEmail_ReturnsUser() {
        // Given
        entityManager.persistAndFlush(testUser);

        // When
        Optional<UserEntity> found = userRepository.findByEmailHmac("email_hmac_123");

        // Then
        assertTrue(found.isPresent());
        assertEquals("email_hmac_123", found.get().getEmailHmac());
    }

    @Test
    void findByPhoneHmac_ExistingPhone_ReturnsUser() {
        // Given
        entityManager.persistAndFlush(testUser);

        // When
        Optional<UserEntity> found = userRepository.findByPhoneHmac("phone_hmac_123");

        // Then
        assertTrue(found.isPresent());
        assertEquals("phone_hmac_123", found.get().getPhoneHmac());
    }

    @Test
    void existsByEmailHmac_ExistingEmail_ReturnsTrue() {
        // Given
        entityManager.persistAndFlush(testUser);

        // When
        boolean exists = userRepository.existsByEmailHmac("email_hmac_123");

        // Then
        assertTrue(exists);
    }

    @Test
    void existsByEmailHmac_NonExistingEmail_ReturnsFalse() {
        // When
        boolean exists = userRepository.existsByEmailHmac("nonexistent_hmac");

        // Then
        assertFalse(exists);
    }

    @Test
    void existsByPhoneHmac_ExistingPhone_ReturnsTrue() {
        // Given
        entityManager.persistAndFlush(testUser);

        // When
        boolean exists = userRepository.existsByPhoneHmac("phone_hmac_123");

        // Then
        assertTrue(exists);
    }

    @Test
    void findByActiveTrue_ActiveUsers_ReturnsOnlyActiveUsers() {
        // Given
        UserEntity inactiveUser = new UserEntity();
        inactiveUser.setReferenceId("USR87654321");
        inactiveUser.setFirstNameEnc("encrypted_jane");
        inactiveUser.setLastNameEnc("encrypted_smith");
        inactiveUser.setEmailEnc("encrypted_email2");
        inactiveUser.setEmailHmac("email_hmac_456");
        inactiveUser.setActive(false);
        inactiveUser.setDeletedAt(LocalDateTime.now());
        inactiveUser.setCreatedAt(LocalDateTime.now());
        inactiveUser.setUpdatedAt(LocalDateTime.now());

        entityManager.persistAndFlush(testUser);
        entityManager.persistAndFlush(inactiveUser);

        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<UserEntity> activePage = userRepository.findByActiveTrue(pageable);

        // Then
        assertEquals(1, activePage.getTotalElements());
        assertTrue(activePage.getContent().get(0).getActive());
        assertEquals(testReferenceId, activePage.getContent().get(0).getReferenceId());
    }

    @Test
    void findByReferenceIdContaining_PartialMatch_ReturnsMatchingUsers() {
        // Given
        UserEntity anotherUser = new UserEntity();
        anotherUser.setReferenceId("USR87654321");
        anotherUser.setFirstNameEnc("encrypted_jane");
        anotherUser.setLastNameEnc("encrypted_smith");
        anotherUser.setEmailEnc("encrypted_email2");
        anotherUser.setEmailHmac("email_hmac_456");
        anotherUser.setActive(true);
        anotherUser.setCreatedAt(LocalDateTime.now());
        anotherUser.setUpdatedAt(LocalDateTime.now());

        entityManager.persistAndFlush(testUser);
        entityManager.persistAndFlush(anotherUser);

        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<UserEntity> searchResults = userRepository.findByReferenceIdContaining("USR123", pageable);

        // Then
        assertEquals(1, searchResults.getTotalElements());
        assertEquals(testReferenceId, searchResults.getContent().get(0).getReferenceId());
    }

    @Test
    void findAll_WithPagination_ReturnsPagedResults() {
        // Given
        UserEntity user2 = new UserEntity();
        user2.setReferenceId("USR87654321");
        user2.setFirstNameEnc("encrypted_jane");
        user2.setLastNameEnc("encrypted_smith");
        user2.setEmailEnc("encrypted_email2");
        user2.setEmailHmac("email_hmac_456");
        user2.setActive(true);
        user2.setCreatedAt(LocalDateTime.now());
        user2.setUpdatedAt(LocalDateTime.now());

        UserEntity user3 = new UserEntity();
        user3.setReferenceId("USR11111111");
        user3.setFirstNameEnc("encrypted_bob");
        user3.setLastNameEnc("encrypted_johnson");
        user3.setEmailEnc("encrypted_email3");
        user3.setEmailHmac("email_hmac_789");
        user3.setActive(false);
        user3.setDeletedAt(LocalDateTime.now());
        user3.setCreatedAt(LocalDateTime.now());
        user3.setUpdatedAt(LocalDateTime.now());

        entityManager.persistAndFlush(testUser);
        entityManager.persistAndFlush(user2);
        entityManager.persistAndFlush(user3);

        Pageable pageable = PageRequest.of(0, 2);

        // When
        Page<UserEntity> page = userRepository.findAll(pageable);

        // Then
        assertEquals(3, page.getTotalElements());
        assertEquals(2, page.getSize());
        assertEquals(2, page.getContent().size());
        assertEquals(2, page.getTotalPages());
    }

    @Test
    void cascadeOperations_SaveUserWithRoles_SavesRolesAutomatically() {
        // Given
        UserRoleEntity adminRole = new UserRoleEntity(testUser, RoleHierarchyConfig.Roles.ADMIN);
        testUser.addRole(adminRole);

        // When
        UserEntity savedUser = userRepository.save(testUser);
        entityManager.flush();
        entityManager.clear();

        // Then
        UserEntity foundUser = userRepository.findById(savedUser.getId()).orElse(null);
        assertNotNull(foundUser);
        assertEquals(2, foundUser.getRoles().size()); // CUSTOMER + ADMIN
        assertTrue(foundUser.getRoleNames().contains(RoleHierarchyConfig.Roles.CUSTOMER));
        assertTrue(foundUser.getRoleNames().contains(RoleHierarchyConfig.Roles.ADMIN));
    }

    @Test
    void softDelete_MarkAsDeleted_UpdatesActiveAndDeletedAt() {
        // Given
        entityManager.persistAndFlush(testUser);
        
        // When
        testUser.markAsDeleted();
        userRepository.save(testUser);
        entityManager.flush();

        // Then
        UserEntity updatedUser = userRepository.findById(testUser.getId()).orElse(null);
        assertNotNull(updatedUser);
        assertFalse(updatedUser.getActive());
        assertNotNull(updatedUser.getDeletedAt());
    }

    @Test
    void reactivate_RestoreDeletedUser_UpdatesActiveAndClearsDeletedAt() {
        // Given
        testUser.setActive(false);
        testUser.setDeletedAt(LocalDateTime.now());
        entityManager.persistAndFlush(testUser);

        // When
        testUser.reactivate();
        userRepository.save(testUser);
        entityManager.flush();

        // Then
        UserEntity reactivatedUser = userRepository.findById(testUser.getId()).orElse(null);
        assertNotNull(reactivatedUser);
        assertTrue(reactivatedUser.getActive());
        assertNull(reactivatedUser.getDeletedAt());
    }

    @Test
    void auditFields_AutomaticTimestamps_UpdatedOnSave() {
        // Given
        LocalDateTime beforeSave = LocalDateTime.now().minusSeconds(1);

        // When
        UserEntity savedUser = userRepository.save(testUser);
        entityManager.flush();

        // Then
        assertNotNull(savedUser.getCreatedAt());
        assertNotNull(savedUser.getUpdatedAt());
        assertTrue(savedUser.getCreatedAt().isAfter(beforeSave));
        assertTrue(savedUser.getUpdatedAt().isAfter(beforeSave));
    }

    @Test
    void uniqueConstraints_DuplicateEmailHmac_ThrowsException() {
        // Given
        entityManager.persistAndFlush(testUser);

        UserEntity duplicateUser = new UserEntity();
        duplicateUser.setReferenceId("USR87654321");
        duplicateUser.setFirstNameEnc("encrypted_jane");
        duplicateUser.setLastNameEnc("encrypted_smith");
        duplicateUser.setEmailEnc("encrypted_email2");
        duplicateUser.setEmailHmac("email_hmac_123"); // Same HMAC as testUser
        duplicateUser.setActive(true);
        duplicateUser.setCreatedAt(LocalDateTime.now());
        duplicateUser.setUpdatedAt(LocalDateTime.now());

        // When & Then
        assertThrows(Exception.class, () -> {
            userRepository.save(duplicateUser);
            entityManager.flush();
        });
    }
}
