package com.mysillydreams.userservice.service;

import com.mysillydreams.userservice.domain.UserEntity;
import com.mysillydreams.userservice.domain.UserRoleEntity;
import com.mysillydreams.userservice.dto.UserDto;
import com.mysillydreams.userservice.encryption.PiiMapper;
import com.mysillydreams.userservice.mapper.UserMapper;
import com.mysillydreams.userservice.repository.*;
import com.mysillydreams.userservice.security.RoleHierarchyConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserService.
 * Tests core user management operations with mocked dependencies.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private UserAuditRepository userAuditRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PiiMapper piiMapper;

    @Mock
    private UserLookupService userLookupService;

    @Mock
    private EventPublishingService eventPublishingService;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private UserService userService;

    private UserDto testUserDto;
    private UserEntity testUserEntity;
    private String testReferenceId;

    @BeforeEach
    void setUp() {
        testReferenceId = "USR12345678";

        testUserDto = new UserDto();
        testUserDto.setFirstName("John");
        testUserDto.setLastName("Doe");
        testUserDto.setEmail("john.doe@example.com");
        testUserDto.setPhone("+1234567890");
        testUserDto.setGender("MALE");

        testUserEntity = new UserEntity();
        testUserEntity.setId(UUID.randomUUID());
        testUserEntity.setReferenceId(testReferenceId);
        testUserEntity.setFirstNameEnc("encrypted_john");
        testUserEntity.setLastNameEnc("encrypted_doe");
        testUserEntity.setEmailEnc("encrypted_email");
        testUserEntity.setPhoneEnc("encrypted_phone");
        testUserEntity.setEmailHmac("email_hmac");
        testUserEntity.setPhoneHmac("phone_hmac");
        testUserEntity.setGender("MALE");
        testUserEntity.setActive(true);
        testUserEntity.setCreatedAt(LocalDateTime.now());
        testUserEntity.setUpdatedAt(LocalDateTime.now());
    }
    }

    @Test
    void createUser_Success() {
        // Given
        when(piiMapper.isValidEmail(testUserDto.getEmail())).thenReturn(true);
        when(piiMapper.isValidPhone(testUserDto.getPhone())).thenReturn(true);
        when(userLookupService.isEmailRegistered(testUserDto.getEmail())).thenReturn(false);
        when(userLookupService.isPhoneRegistered(testUserDto.getPhone())).thenReturn(false);
        when(userMapper.toEntity(testUserDto)).thenReturn(testUserEntity);
        when(userRepository.save(any(UserEntity.class))).thenReturn(testUserEntity);
        when(userMapper.toDto(testUserEntity)).thenReturn(testUserDto);

        try (MockedStatic<RoleHierarchyConfig.SecurityUtils> mockedSecurityUtils = mockStatic(RoleHierarchyConfig.SecurityUtils.class)) {
            mockedSecurityUtils.when(RoleHierarchyConfig.SecurityUtils::getCurrentUserId).thenReturn(UUID.randomUUID());

            // When
            UserDto result = userService.createUser(testUserDto);

            // Then
            assertNotNull(result);
            assertEquals(testUserDto.getFirstName(), result.getFirstName());
            assertEquals(testUserDto.getEmail(), result.getEmail());

            verify(userRepository).save(any(UserEntity.class));
            verify(auditService).createUserAudit(any(UserEntity.class), any(), anyString(), any());
            verify(eventPublishingService).publishUserCreatedEvent(any(UserEntity.class));
        }
    }

    @Test
    void createUser_EmailAlreadyExists_ThrowsException() {
        // Given
        when(piiMapper.isValidEmail(testUserDto.getEmail())).thenReturn(true);
        when(userLookupService.isEmailRegistered(testUserDto.getEmail())).thenReturn(true);

        // When & Then
        UserService.UserValidationException exception = assertThrows(
            UserService.UserValidationException.class,
            () -> userService.createUser(testUserDto)
        );

        assertEquals("Email already registered", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void createUser_InvalidEmail_ThrowsException() {
        // Given
        when(piiMapper.isValidEmail(testUserDto.getEmail())).thenReturn(false);

        // When & Then
        UserService.UserValidationException exception = assertThrows(
            UserService.UserValidationException.class,
            () -> userService.createUser(testUserDto)
        );

        assertEquals("Invalid email format", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void createUser_PhoneAlreadyExists_ThrowsException() {
        // Given
        when(piiMapper.isValidEmail(testUserDto.getEmail())).thenReturn(true);
        when(piiMapper.isValidPhone(testUserDto.getPhone())).thenReturn(true);
        when(userLookupService.isEmailRegistered(testUserDto.getEmail())).thenReturn(false);
        when(userLookupService.isPhoneRegistered(testUserDto.getPhone())).thenReturn(true);

        // When & Then
        UserService.UserValidationException exception = assertThrows(
            UserService.UserValidationException.class,
            () -> userService.createUser(testUserDto)
        );

        assertEquals("Phone number already registered", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void getUserByReferenceId_Success() {
        // Given
        when(userRepository.findByReferenceIdAndActiveTrue(testReferenceId)).thenReturn(Optional.of(testUserEntity));
        when(userMapper.toDto(testUserEntity)).thenReturn(testUserDto);

        try (MockedStatic<RoleHierarchyConfig.SecurityUtils> mockedSecurityUtils = mockStatic(RoleHierarchyConfig.SecurityUtils.class)) {
            mockedSecurityUtils.when(RoleHierarchyConfig.SecurityUtils::getCurrentUserId).thenReturn(UUID.randomUUID());
            mockedSecurityUtils.when(RoleHierarchyConfig.SecurityUtils::isAdmin).thenReturn(false);

            // When
            UserDto result = userService.getUserByReferenceId(testReferenceId);

            // Then
            assertNotNull(result);
            assertEquals(testUserDto.getFirstName(), result.getFirstName());

            verify(auditService).createUserAudit(any(UserEntity.class), any(), anyString(), any());
        }
    }

    @Test
    void getUserByReferenceId_UserNotFound_ThrowsException() {
        // Given
        when(userRepository.findByReferenceIdAndActiveTrue(testReferenceId)).thenReturn(Optional.empty());

        // When & Then
        UserService.UserNotFoundException exception = assertThrows(
            UserService.UserNotFoundException.class,
            () -> userService.getUserByReferenceId(testReferenceId)
        );

        assertEquals("User not found: " + testReferenceId, exception.getMessage());
    }

    @Test
    void updateUser_Success() {
        // Given
        UserDto updateDto = new UserDto();
        updateDto.setFirstName("Jane");
        updateDto.setLastName("Smith");

        when(userRepository.findByReferenceIdAndActiveTrue(testReferenceId)).thenReturn(Optional.of(testUserEntity));
        when(userRepository.save(testUserEntity)).thenReturn(testUserEntity);
        when(userMapper.toDto(testUserEntity)).thenReturn(updateDto);

        try (MockedStatic<RoleHierarchyConfig.SecurityUtils> mockedSecurityUtils = mockStatic(RoleHierarchyConfig.SecurityUtils.class)) {
            mockedSecurityUtils.when(RoleHierarchyConfig.SecurityUtils::getCurrentUserId).thenReturn(UUID.randomUUID());

            // When
            UserDto result = userService.updateUser(testReferenceId, updateDto);

            // Then
            assertNotNull(result);
            assertEquals(updateDto.getFirstName(), result.getFirstName());

            verify(userMapper).updateEntityFromDto(updateDto, testUserEntity);
            verify(userRepository).save(testUserEntity);
            verify(auditService).createUserAudit(any(UserEntity.class), any(), anyString(), any(), any());
            verify(eventPublishingService).publishUserUpdatedEvent(any(UserEntity.class), any());
        }
    }

    @Test
    void deleteUser_Success() {
        // Given
        String reason = "User requested deletion";
        when(userRepository.findByReferenceIdAndActiveTrue(testReferenceId)).thenReturn(Optional.of(testUserEntity));
        when(userRepository.save(testUserEntity)).thenReturn(testUserEntity);

        try (MockedStatic<RoleHierarchyConfig.SecurityUtils> mockedSecurityUtils = mockStatic(RoleHierarchyConfig.SecurityUtils.class)) {
            mockedSecurityUtils.when(RoleHierarchyConfig.SecurityUtils::getCurrentUserId).thenReturn(UUID.randomUUID());

            // When
            userService.deleteUser(testReferenceId, reason);

            // Then
            verify(userRoleRepository).deactivateAllUserRoles(testUserEntity.getId());
            verify(userRepository).save(testUserEntity);
            verify(auditService).createUserAudit(any(UserEntity.class), any(), anyString(), any(), any());
            verify(eventPublishingService).publishUserDeletedEvent(testUserEntity, reason);
        }
    }

    @Test
    void deleteUser_LastAdmin_ThrowsException() {
        // Given
        testUserEntity.addRole(new UserRoleEntity(testUserEntity, RoleHierarchyConfig.Roles.ADMIN));
        when(userRepository.findByReferenceIdAndActiveTrue(testReferenceId)).thenReturn(Optional.of(testUserEntity));
        when(userRoleRepository.countByRoleAndActiveTrue(RoleHierarchyConfig.Roles.ADMIN)).thenReturn(1L);

        // When & Then
        UserService.UserServiceException exception = assertThrows(
            UserService.UserServiceException.class,
            () -> userService.deleteUser(testReferenceId, "test")
        );

        assertEquals("Cannot delete the last admin user", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void reactivateUser_Success() {
        // Given
        testUserEntity.setActive(false);
        testUserEntity.setDeletedAt(LocalDateTime.now());

        when(userRepository.findByReferenceId(testReferenceId)).thenReturn(Optional.of(testUserEntity));
        when(userRepository.save(testUserEntity)).thenReturn(testUserEntity);
        when(userMapper.toDto(testUserEntity)).thenReturn(testUserDto);

        try (MockedStatic<RoleHierarchyConfig.SecurityUtils> mockedSecurityUtils = mockStatic(RoleHierarchyConfig.SecurityUtils.class)) {
            mockedSecurityUtils.when(RoleHierarchyConfig.SecurityUtils::getCurrentUserId).thenReturn(UUID.randomUUID());

            // When
            UserDto result = userService.reactivateUser(testReferenceId);

            // Then
            assertNotNull(result);
            assertTrue(testUserEntity.getActive());
            assertNull(testUserEntity.getDeletedAt());

            verify(userRepository).save(testUserEntity);
            verify(auditService).createUserAudit(any(UserEntity.class), any(), anyString(), any());
            verify(eventPublishingService).publishUserReactivatedEvent(testUserEntity);
        }
    }

    @Test
    void reactivateUser_AlreadyActive_ThrowsException() {
        // Given
        when(userRepository.findByReferenceId(testReferenceId)).thenReturn(Optional.of(testUserEntity));

        // When & Then
        UserService.UserServiceException exception = assertThrows(
            UserService.UserServiceException.class,
            () -> userService.reactivateUser(testReferenceId)
        );

        assertEquals("User is already active: " + testReferenceId, exception.getMessage());
    }

    @Test
    void listUsers_Success() {
        // Given
        List<UserEntity> userList = Arrays.asList(testUserEntity);
        Page<UserEntity> userPage = new PageImpl<>(userList);
        Pageable pageable = PageRequest.of(0, 20);

        when(userRepository.findByActiveTrue(pageable)).thenReturn(userPage);
        when(userMapper.toDto(testUserEntity)).thenReturn(testUserDto);

        // When
        Page<UserDto> result = userService.listUsers(pageable, false);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(testUserDto.getFirstName(), result.getContent().get(0).getFirstName());
    }

    @Test
    void searchUsers_Success() {
        // Given
        String query = "USR123";
        List<UserEntity> userList = Arrays.asList(testUserEntity);
        Page<UserEntity> userPage = new PageImpl<>(userList);
        Pageable pageable = PageRequest.of(0, 20);

        when(userRepository.findByReferenceIdContaining(query, pageable)).thenReturn(userPage);
        when(userMapper.toDto(testUserEntity)).thenReturn(testUserDto);

        // When
        Page<UserDto> result = userService.searchUsers(query, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(testUserDto.getFirstName(), result.getContent().get(0).getFirstName());
    }
}
