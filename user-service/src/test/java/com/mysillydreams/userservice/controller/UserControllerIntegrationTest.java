package com.mysillydreams.userservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mysillydreams.userservice.dto.UserCreateRequestDto;
import com.mysillydreams.userservice.dto.UserDto;
import com.mysillydreams.userservice.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for UserController.
 * Tests REST endpoints with mocked service layer.
 */
@WebMvcTest(UserController.class)
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    private UserDto testUserDto;
    private UserCreateRequestDto createRequestDto;
    private String testReferenceId;

    @BeforeEach
    void setUp() {
        testReferenceId = "USR12345678";

        testUserDto = new UserDto();
        testUserDto.setReferenceId(testReferenceId);
        testUserDto.setFirstName("John");
        testUserDto.setLastName("Doe");
        testUserDto.setEmail("john.doe@example.com");
        testUserDto.setPhone("+1234567890");
        testUserDto.setGender("MALE");
        testUserDto.setActive(true);
        testUserDto.setCreatedAt(LocalDateTime.now());
        testUserDto.setUpdatedAt(LocalDateTime.now());

        Set<String> roles = new HashSet<>();
        roles.add("ROLE_CUSTOMER");
        testUserDto.setRoles(roles);

        createRequestDto = new UserCreateRequestDto();
        createRequestDto.setFirstName("John");
        createRequestDto.setLastName("Doe");
        createRequestDto.setEmail("john.doe@example.com");
        createRequestDto.setPhone("+1234567890");
        createRequestDto.setGender("MALE");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createUser_Success() throws Exception {
        // Given
        when(userService.createUser(any(UserDto.class))).thenReturn(testUserDto);

        // When & Then
        mockMvc.perform(post("/api/v1/users")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User created successfully"))
                .andExpect(jsonPath("$.data.referenceId").value(testReferenceId))
                .andExpect(jsonPath("$.data.firstName").value("John"))
                .andExpect(jsonPath("$.data.lastName").value("Doe"))
                .andExpect(jsonPath("$.data.email").value("john.doe@example.com"))
                .andExpect(jsonPath("$.metadata.requestId").exists())
                .andExpect(jsonPath("$.metadata.version").value("v1"))
                .andExpect(jsonPath("$.metadata.processingTimeMs").exists());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createUser_ValidationError() throws Exception {
        // Given - Invalid request with missing required fields
        UserCreateRequestDto invalidRequest = new UserCreateRequestDto();
        invalidRequest.setFirstName(""); // Empty first name
        invalidRequest.setEmail("invalid-email"); // Invalid email format

        // When & Then
        mockMvc.perform(post("/api/v1/users")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createUser_EmailAlreadyExists() throws Exception {
        // Given
        when(userService.createUser(any(UserDto.class)))
                .thenThrow(new UserService.UserValidationException("Email already registered"));

        // When & Then
        mockMvc.perform(post("/api/v1/users")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequestDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.message").value("Email already registered"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getUserByReferenceId_Success() throws Exception {
        // Given
        when(userService.getUserByReferenceId(testReferenceId)).thenReturn(testUserDto);

        // When & Then
        mockMvc.perform(get("/api/v1/users/{referenceId}", testReferenceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User retrieved successfully"))
                .andExpect(jsonPath("$.data.referenceId").value(testReferenceId))
                .andExpect(jsonPath("$.data.firstName").value("John"))
                .andExpect(jsonPath("$.data.email").value("john.doe@example.com"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getUserByReferenceId_UserNotFound() throws Exception {
        // Given
        when(userService.getUserByReferenceId(testReferenceId))
                .thenThrow(new UserService.UserNotFoundException("User not found"));

        // When & Then
        mockMvc.perform(get("/api/v1/users/{referenceId}", testReferenceId))
                .andExpected(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("USER_NOT_FOUND"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getUserByReferenceId_InvalidReferenceIdFormat() throws Exception {
        // Given - Invalid reference ID format
        String invalidReferenceId = "INVALID123";

        // When & Then
        mockMvc.perform(get("/api/v1/users/{referenceId}", invalidReferenceId))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateUser_Success() throws Exception {
        // Given
        UserDto updateDto = new UserDto();
        updateDto.setFirstName("Jane");
        updateDto.setLastName("Smith");

        UserDto updatedUser = new UserDto();
        updatedUser.setReferenceId(testReferenceId);
        updatedUser.setFirstName("Jane");
        updatedUser.setLastName("Smith");
        updatedUser.setEmail("john.doe@example.com");

        when(userService.updateUser(eq(testReferenceId), any(UserDto.class))).thenReturn(updatedUser);

        // When & Then
        mockMvc.perform(put("/api/v1/users/{referenceId}", testReferenceId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User updated successfully"))
                .andExpect(jsonPath("$.data.firstName").value("Jane"))
                .andExpect(jsonPath("$.data.lastName").value("Smith"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteUser_Success() throws Exception {
        // Given
        String reason = "User requested deletion";

        // When & Then
        mockMvc.perform(delete("/api/v1/users/{referenceId}", testReferenceId)
                .with(csrf())
                .param("reason", reason))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User deleted successfully"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteUser_LastAdmin() throws Exception {
        // Given
        when(userService.deleteUser(eq(testReferenceId), anyString()))
                .thenThrow(new UserService.UserServiceException("Cannot delete the last admin user"));

        // When & Then
        mockMvc.perform(delete("/api/v1/users/{referenceId}", testReferenceId)
                .with(csrf())
                .param("reason", "test"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("USER_DELETION_FAILED"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void listUsers_Success() throws Exception {
        // Given
        Page<UserDto> userPage = new PageImpl<>(Arrays.asList(testUserDto), PageRequest.of(0, 20), 1);
        when(userService.listUsers(any(), eq(false))).thenReturn(userPage);

        // When & Then
        mockMvc.perform(get("/api/v1/users")
                .param("page", "0")
                .param("size", "20")
                .param("includeInactive", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Users retrieved successfully"))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].referenceId").value(testReferenceId))
                .andExpect(jsonPath("$.metadata.paginationInfo.page").value(0))
                .andExpect(jsonPath("$.metadata.paginationInfo.size").value(20))
                .andExpect(jsonPath("$.metadata.paginationInfo.totalElements").value(1));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void searchUsers_Success() throws Exception {
        // Given
        String query = "USR123";
        Page<UserDto> userPage = new PageImpl<>(Arrays.asList(testUserDto), PageRequest.of(0, 20), 1);
        when(userService.searchUsers(eq(query), any())).thenReturn(userPage);

        // When & Then
        mockMvc.perform(get("/api/v1/users/search")
                .param("query", query)
                .param("page", "0")
                .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Search completed successfully"))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].referenceId").value(testReferenceId));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void reactivateUser_Success() throws Exception {
        // Given
        when(userService.reactivateUser(testReferenceId)).thenReturn(testUserDto);

        // When & Then
        mockMvc.perform(post("/api/v1/users/{referenceId}/reactivate", testReferenceId)
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User reactivated successfully"))
                .andExpect(jsonPath("$.data.referenceId").value(testReferenceId));
    }

    @Test
    void createUser_Unauthorized() throws Exception {
        // When & Then - No authentication
        mockMvc.perform(post("/api/v1/users")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequestDto)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void createUser_InsufficientPermissions() throws Exception {
        // When & Then - Customer role trying to create user
        mockMvc.perform(post("/api/v1/users")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequestDto)))
                .andExpect(status().isForbidden());
    }
}
