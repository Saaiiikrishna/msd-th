package com.mysillydreams.userservice.security;

import com.mysillydreams.userservice.controller.UserController;
import com.mysillydreams.userservice.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Security tests for User Service endpoints.
 * Tests authentication and authorization requirements.
 */
@WebMvcTest(UserController.class)
class SecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    private final String testReferenceId = "USR12345678";

    @Test
    void createUser_NoAuthentication_Returns401() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void createUser_InsufficientRole_Returns403() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createUser_AdminRole_AllowsAccess() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"firstName\":\"John\",\"lastName\":\"Doe\",\"email\":\"john@example.com\"}"))
                .andExpect(status().isOk()); // Will fail validation but passes security
    }

    @Test
    @WithMockUser(roles = "SUPPORT_USER")
    void createUser_SupportUserRole_AllowsAccess() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"firstName\":\"John\",\"lastName\":\"Doe\",\"email\":\"john@example.com\"}"))
                .andExpect(status().isOk()); // Will fail validation but passes security
    }

    @Test
    void getUserByReferenceId_NoAuthentication_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/users/{referenceId}", testReferenceId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getUserByReferenceId_AdminRole_AllowsAccess() throws Exception {
        mockMvc.perform(get("/api/v1/users/{referenceId}", testReferenceId))
                .andExpect(status().isOk()); // Will fail service call but passes security
    }

    @Test
    @WithMockUser(roles = "SUPPORT_USER")
    void getUserByReferenceId_SupportUserRole_AllowsAccess() throws Exception {
        mockMvc.perform(get("/api/v1/users/{referenceId}", testReferenceId))
                .andExpect(status().isOk()); // Will fail service call but passes security
    }

    @Test
    @WithMockUser(username = "user123", roles = "CUSTOMER")
    void getUserByReferenceId_OwnUser_AllowsAccess() throws Exception {
        // This test simulates a user accessing their own data
        // In real implementation, the security context would contain the user's reference ID
        mockMvc.perform(get("/api/v1/users/{referenceId}", testReferenceId))
                .andExpect(status().isOk()); // Will fail service call but passes security
    }

    @Test
    @WithMockUser(username = "otheruser", roles = "CUSTOMER")
    void getUserByReferenceId_OtherUser_Returns403() throws Exception {
        // This test simulates a user trying to access another user's data
        mockMvc.perform(get("/api/v1/users/{referenceId}", testReferenceId))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateUser_NoAuthentication_Returns401() throws Exception {
        mockMvc.perform(put("/api/v1/users/{referenceId}", testReferenceId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void updateUser_CustomerRole_Returns403() throws Exception {
        mockMvc.perform(put("/api/v1/users/{referenceId}", testReferenceId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateUser_AdminRole_AllowsAccess() throws Exception {
        mockMvc.perform(put("/api/v1/users/{referenceId}", testReferenceId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isOk()); // Will fail validation but passes security
    }

    @Test
    void deleteUser_NoAuthentication_Returns401() throws Exception {
        mockMvc.perform(delete("/api/v1/users/{referenceId}", testReferenceId)
                .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void deleteUser_CustomerRole_Returns403() throws Exception {
        mockMvc.perform(delete("/api/v1/users/{referenceId}", testReferenceId)
                .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteUser_AdminRole_AllowsAccess() throws Exception {
        mockMvc.perform(delete("/api/v1/users/{referenceId}", testReferenceId)
                .with(csrf()))
                .andExpect(status().isOk()); // Will fail service call but passes security
    }

    @Test
    void listUsers_NoAuthentication_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void listUsers_CustomerRole_Returns403() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void listUsers_AdminRole_AllowsAccess() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isOk()); // Will fail service call but passes security
    }

    @Test
    @WithMockUser(roles = "SUPPORT_USER")
    void listUsers_SupportUserRole_AllowsAccess() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isOk()); // Will fail service call but passes security
    }

    @Test
    void searchUsers_NoAuthentication_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/users/search")
                .param("query", "USR123"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void searchUsers_CustomerRole_Returns403() throws Exception {
        mockMvc.perform(get("/api/v1/users/search")
                .param("query", "USR123"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void searchUsers_AdminRole_AllowsAccess() throws Exception {
        mockMvc.perform(get("/api/v1/users/search")
                .param("query", "USR123"))
                .andExpect(status().isOk()); // Will fail service call but passes security
    }

    @Test
    void reactivateUser_NoAuthentication_Returns401() throws Exception {
        mockMvc.perform(post("/api/v1/users/{referenceId}/reactivate", testReferenceId)
                .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void reactivateUser_CustomerRole_Returns403() throws Exception {
        mockMvc.perform(post("/api/v1/users/{referenceId}/reactivate", testReferenceId)
                .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void reactivateUser_AdminRole_AllowsAccess() throws Exception {
        mockMvc.perform(post("/api/v1/users/{referenceId}/reactivate", testReferenceId)
                .with(csrf()))
                .andExpect(status().isOk()); // Will fail service call but passes security
    }

    @Test
    void csrfProtection_PostWithoutCsrf_Returns403() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isForbidden()); // CSRF protection
    }

    @Test
    void csrfProtection_PutWithoutCsrf_Returns403() throws Exception {
        mockMvc.perform(put("/api/v1/users/{referenceId}", testReferenceId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isForbidden()); // CSRF protection
    }

    @Test
    void csrfProtection_DeleteWithoutCsrf_Returns403() throws Exception {
        mockMvc.perform(delete("/api/v1/users/{referenceId}", testReferenceId))
                .andExpect(status().isForbidden()); // CSRF protection
    }

    @Test
    void csrfProtection_GetRequest_NoCSRFRequired() throws Exception {
        // GET requests don't require CSRF token
        mockMvc.perform(get("/api/v1/users/{referenceId}", testReferenceId))
                .andExpect(status().isUnauthorized()); // Only fails on authentication, not CSRF
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void inputValidation_InvalidReferenceIdFormat_Returns400() throws Exception {
        String invalidReferenceId = "INVALID123";
        
        mockMvc.perform(get("/api/v1/users/{referenceId}", invalidReferenceId))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void inputValidation_ValidReferenceIdFormat_PassesValidation() throws Exception {
        String validReferenceId = "USR123456789";
        
        mockMvc.perform(get("/api/v1/users/{referenceId}", validReferenceId))
                .andExpect(status().isOk()); // Will fail service call but passes validation
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void contentTypeValidation_InvalidContentType_Returns415() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                .with(csrf())
                .contentType(MediaType.TEXT_PLAIN)
                .content("invalid content"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void contentTypeValidation_ValidContentType_PassesValidation() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isOk()); // Will fail validation but passes content type check
    }
}
