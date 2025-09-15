package com.mysillydreams.treasure.api.rest.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mysillydreams.treasure.api.rest.dto.request.EnrollRequest;
import com.mysillydreams.treasure.domain.model.*;
import com.mysillydreams.treasure.domain.repository.EnrollmentRepository;
import com.mysillydreams.treasure.domain.repository.PlanRepository;
import com.mysillydreams.treasure.domain.service.RegistrationIdService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
@Transactional
class EnrollmentControllerIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private RegistrationIdService registrationIdService;

    private MockMvc mockMvc;
    private Plan testPlan;
    private UUID userId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        
        userId = UUID.randomUUID();
        
        // Create test plan
        testPlan = Plan.builder()
                .title("Test Treasure Hunt")
                .description("A test treasure hunt plan")
                .difficulty(Difficulty.BEGINNER)
                .enrollmentMode(EnrollmentMode.PAY_TO_ENROLL)
                .maxParticipants(100)
                .basePrice(BigDecimal.valueOf(1000))
                .currency("INR")
                .isActive(true)
                .build();
        
        testPlan = planRepository.save(testPlan);
    }

    @Test
    void enrollIndividual_ShouldCreateEnrollmentWithRegistrationId() throws Exception {
        // Given
        EnrollRequest request = new EnrollRequest(
                userId,
                EnrollmentType.INDIVIDUAL,
                null,
                null
        );

        // When & Then
        mockMvc.perform(post("/api/treasure/v1/plans/{planId}/enroll", testPlan.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.registrationId").exists())
                .andExpect(jsonPath("$.enrollmentType").value("INDIVIDUAL"))
                .andExpect(jsonPath("$.teamName").doesNotExist())
                .andExpect(jsonPath("$.teamSize").doesNotExist());

        // Verify enrollment was created
        Enrollment enrollment = enrollmentRepository.findByUserIdAndPlanId(userId, testPlan.getId())
                .orElseThrow();
        
        assertThat(enrollment.getRegistrationId()).isNotNull();
        assertThat(enrollment.getRegistrationId()).startsWith("TH-");
        assertThat(enrollment.getRegistrationId()).contains("-INDIVIDUAL-");
        assertThat(enrollment.getEnrollmentType()).isEqualTo(EnrollmentType.INDIVIDUAL);
        assertThat(enrollment.getTeamName()).isNull();
        assertThat(enrollment.getTeamSize()).isNull();
    }

    @Test
    void enrollTeam_ShouldCreateTeamEnrollmentWithRegistrationId() throws Exception {
        // Given
        EnrollRequest request = new EnrollRequest(
                userId,
                EnrollmentType.TEAM,
                "Team Alpha",
                5
        );

        // When & Then
        mockMvc.perform(post("/api/treasure/v1/plans/{planId}/enroll", testPlan.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.registrationId").exists())
                .andExpect(jsonPath("$.enrollmentType").value("TEAM"))
                .andExpect(jsonPath("$.teamName").value("Team Alpha"))
                .andExpect(jsonPath("$.teamSize").value(5));

        // Verify enrollment was created
        Enrollment enrollment = enrollmentRepository.findByUserIdAndPlanId(userId, testPlan.getId())
                .orElseThrow();
        
        assertThat(enrollment.getRegistrationId()).contains("-TEAM-");
        assertThat(enrollment.getEnrollmentType()).isEqualTo(EnrollmentType.TEAM);
        assertThat(enrollment.getTeamName()).isEqualTo("Team Alpha");
        assertThat(enrollment.getTeamSize()).isEqualTo(5);
    }

    @Test
    void enrollTeam_ShouldValidateTeamParameters() throws Exception {
        // Given - Invalid team enrollment (missing team name)
        EnrollRequest request = new EnrollRequest(
                userId,
                EnrollmentType.TEAM,
                null,
                5
        );

        // When & Then
        mockMvc.perform(post("/api/treasure/v1/plans/{planId}/enroll", testPlan.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void enrollTeam_ShouldValidateMinimumTeamSize() throws Exception {
        // Given - Invalid team size
        EnrollRequest request = new EnrollRequest(
                userId,
                EnrollmentType.TEAM,
                "Team Beta",
                1
        );

        // When & Then
        mockMvc.perform(post("/api/treasure/v1/plans/{planId}/enroll", testPlan.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void enroll_ShouldGenerateUniqueRegistrationIds() throws Exception {
        // Given
        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();
        
        EnrollRequest request1 = new EnrollRequest(userId1, EnrollmentType.INDIVIDUAL, null, null);
        EnrollRequest request2 = new EnrollRequest(userId2, EnrollmentType.INDIVIDUAL, null, null);

        // When - Create two enrollments
        mockMvc.perform(post("/api/treasure/v1/plans/{planId}/enroll", testPlan.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/treasure/v1/plans/{planId}/enroll", testPlan.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isOk());

        // Then - Verify unique registration IDs
        Enrollment enrollment1 = enrollmentRepository.findByUserIdAndPlanId(userId1, testPlan.getId())
                .orElseThrow();
        Enrollment enrollment2 = enrollmentRepository.findByUserIdAndPlanId(userId2, testPlan.getId())
                .orElseThrow();
        
        assertThat(enrollment1.getRegistrationId()).isNotEqualTo(enrollment2.getRegistrationId());
        assertThat(enrollment1.getRegistrationId()).endsWith("01");
        assertThat(enrollment2.getRegistrationId()).endsWith("02");
    }

    @Test
    void enroll_ShouldHandleDifferentEnrollmentTypes() throws Exception {
        // Given
        UUID individualUserId = UUID.randomUUID();
        UUID teamUserId = UUID.randomUUID();
        
        EnrollRequest individualRequest = new EnrollRequest(
                individualUserId, EnrollmentType.INDIVIDUAL, null, null);
        EnrollRequest teamRequest = new EnrollRequest(
                teamUserId, EnrollmentType.TEAM, "Team Gamma", 3);

        // When - Create individual and team enrollments
        mockMvc.perform(post("/api/treasure/v1/plans/{planId}/enroll", testPlan.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(individualRequest)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/treasure/v1/plans/{planId}/enroll", testPlan.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(teamRequest)))
                .andExpect(status().isOk());

        // Then - Verify different registration ID formats
        Enrollment individualEnrollment = enrollmentRepository.findByUserIdAndPlanId(
                individualUserId, testPlan.getId()).orElseThrow();
        Enrollment teamEnrollment = enrollmentRepository.findByUserIdAndPlanId(
                teamUserId, testPlan.getId()).orElseThrow();
        
        assertThat(individualEnrollment.getRegistrationId()).contains("-INDIVIDUAL-");
        assertThat(teamEnrollment.getRegistrationId()).contains("-TEAM-");
        
        // Verify registration ID format validation
        assertThat(registrationIdService.isValidRegistrationId(individualEnrollment.getRegistrationId())).isTrue();
        assertThat(registrationIdService.isValidRegistrationId(teamEnrollment.getRegistrationId())).isTrue();
        
        assertThat(registrationIdService.extractEnrollmentType(individualEnrollment.getRegistrationId()))
                .isEqualTo(EnrollmentType.INDIVIDUAL);
        assertThat(registrationIdService.extractEnrollmentType(teamEnrollment.getRegistrationId()))
                .isEqualTo(EnrollmentType.TEAM);
    }

    @Test
    void enroll_ShouldHandleSlotReservation() throws Exception {
        // Given - Plan with limited slots
        Plan limitedPlan = Plan.builder()
                .title("Limited Treasure Hunt")
                .description("A limited capacity treasure hunt")
                .difficulty(Difficulty.INTERMEDIATE)
                .enrollmentMode(EnrollmentMode.PAY_TO_ENROLL)
                .maxParticipants(2) // Only 2 slots
                .basePrice(BigDecimal.valueOf(1500))
                .currency("INR")
                .isActive(true)
                .build();
        limitedPlan = planRepository.save(limitedPlan);

        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();
        UUID userId3 = UUID.randomUUID();
        
        EnrollRequest request1 = new EnrollRequest(userId1, EnrollmentType.INDIVIDUAL, null, null);
        EnrollRequest request2 = new EnrollRequest(userId2, EnrollmentType.INDIVIDUAL, null, null);
        EnrollRequest request3 = new EnrollRequest(userId3, EnrollmentType.INDIVIDUAL, null, null);

        // When - Try to enroll 3 users in a 2-slot plan
        mockMvc.perform(post("/api/treasure/v1/plans/{planId}/enroll", limitedPlan.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/treasure/v1/plans/{planId}/enroll", limitedPlan.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/treasure/v1/plans/{planId}/enroll", limitedPlan.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request3)))
                .andExpect(status().isInternalServerError()); // Should fail due to no slots

        // Then - Verify only 2 enrollments were created
        assertThat(enrollmentRepository.findByPlanId(limitedPlan.getId())).hasSize(2);
    }
}
