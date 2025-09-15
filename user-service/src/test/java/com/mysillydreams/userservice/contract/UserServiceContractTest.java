package com.mysillydreams.userservice.contract;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mysillydreams.userservice.controller.UserController;
import com.mysillydreams.userservice.dto.UserDto;
import com.mysillydreams.userservice.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Contract tests for User Service API
 * Validates API contracts and response formats
 */
@WebMvcTest(UserController.class)
@ActiveProfiles("test")
class UserServiceContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @Test
    void createUser_ShouldReturnValidContract() throws Exception {
        // Given
        UserDto inputUser = UserDto.builder()
                .email("test@example.com")
                .firstName("John")
                .lastName("Doe")
                .build();

        UserDto createdUser = UserDto.builder()
                .referenceId("01234567-89ab-cdef-0123-456789abcdef")
                .email("test@example.com")
                .firstName("John")
                .lastName("Doe")
                .status("ACTIVE")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(userService.createUser(any(UserDto.class))).thenReturn(createdUser);

        // When & Then
        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(inputUser)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.referenceId").exists())
                .andExpect(jsonPath("$.referenceId").value("01234567-89ab-cdef-0123-456789abcdef"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());
    }

    @Test
    void getUserByReferenceId_ShouldReturnValidContract() throws Exception {
        // Given
        String userRef = "01234567-89ab-cdef-0123-456789abcdef";
        UserDto user = UserDto.builder()
                .referenceId(userRef)
                .email("test@example.com")
                .firstName("John")
                .lastName("Doe")
                .status("ACTIVE")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(userService.getUserByReferenceId(userRef)).thenReturn(Optional.of(user));

        // When & Then
        mockMvc.perform(get("/api/v1/users/{referenceId}", userRef))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.referenceId").value(userRef))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void getUserByReferenceId_NotFound_ShouldReturn404() throws Exception {
        // Given
        String userRef = "01234567-89ab-cdef-0123-456789abcdef";
        when(userService.getUserByReferenceId(userRef)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/v1/users/{referenceId}", userRef))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateUser_ShouldReturnValidContract() throws Exception {
        // Given
        String userRef = "01234567-89ab-cdef-0123-456789abcdef";
        UserDto updateRequest = UserDto.builder()
                .firstName("Jane")
                .lastName("Smith")
                .build();

        UserDto updatedUser = UserDto.builder()
                .referenceId(userRef)
                .email("test@example.com")
                .firstName("Jane")
                .lastName("Smith")
                .status("ACTIVE")
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now())
                .build();

        when(userService.updateUser(anyString(), any(UserDto.class))).thenReturn(updatedUser);

        // When & Then
        mockMvc.perform(put("/api/v1/users/{referenceId}", userRef)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.referenceId").value(userRef))
                .andExpect(jsonPath("$.firstName").value("Jane"))
                .andExpect(jsonPath("$.lastName").value("Smith"))
                .andExpect(jsonPath("$.updatedAt").exists());
    }

    @Test
    void createUser_InvalidEmail_ShouldReturn400() throws Exception {
        // Given
        UserDto invalidUser = UserDto.builder()
                .email("invalid-email")
                .firstName("John")
                .lastName("Doe")
                .build();

        // When & Then
        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidUser)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createUser_MissingRequiredFields_ShouldReturn400() throws Exception {
        // Given
        UserDto incompleteUser = UserDto.builder()
                .email("test@example.com")
                // Missing firstName and lastName
                .build();

        // When & Then
        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(incompleteUser)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void healthCheck_ShouldReturnValidContract() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/users/health"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void getUserProfile_WithValidUserRef_ShouldReturnProfile() throws Exception {
        // Given
        String userRef = "01234567-89ab-cdef-0123-456789abcdef";
        UserDto user = UserDto.builder()
                .referenceId(userRef)
                .email("test@example.com")
                .firstName("John")
                .lastName("Doe")
                .status("ACTIVE")
                .build();

        when(userService.getUserByReferenceId(userRef)).thenReturn(Optional.of(user));

        // When & Then
        mockMvc.perform(get("/api/v1/users/profile")
                .header("X-User-Ref", userRef))
                .andExpect(status().isOk())
                .andExpected(jsonPath("$.referenceId").value(userRef))
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    void getUserProfile_WithoutUserRefHeader_ShouldReturn400() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/users/profile"))
                .andExpect(status().isBadRequest());
    }
}
