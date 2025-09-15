package com.mysillydreams.treasure.domain.service;

import com.mysillydreams.treasure.domain.model.EnrollmentType;
import com.mysillydreams.treasure.domain.model.RegistrationSequence;
import com.mysillydreams.treasure.domain.repository.RegistrationSequenceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrationIdServiceTest {

    @Mock
    private RegistrationSequenceRepository sequenceRepository;

    @InjectMocks
    private RegistrationIdService registrationIdService;

    private UUID planId;
    private String currentMonthYear;

    @BeforeEach
    void setUp() {
        planId = UUID.randomUUID();
        currentMonthYear = OffsetDateTime.now().format(DateTimeFormatter.ofPattern("MMyy"));
    }

    @Test
    void generateRegistrationId_ShouldCreateNewSequenceForFirstRegistration() {
        // Given
        when(sequenceRepository.findByMonthYearAndEnrollmentTypeAndPlanIdWithLock(
                anyString(), eq(EnrollmentType.INDIVIDUAL), eq(planId)))
                .thenReturn(Optional.empty());
        
        RegistrationSequence newSequence = RegistrationSequence.builder()
                .monthYear(currentMonthYear)
                .enrollmentType(EnrollmentType.INDIVIDUAL)
                .planId(planId)
                .currentSequence(1L)
                .build();
        
        when(sequenceRepository.save(any(RegistrationSequence.class)))
                .thenReturn(newSequence);

        // When
        String registrationId = registrationIdService.generateRegistrationId(EnrollmentType.INDIVIDUAL, planId);

        // Then
        assertThat(registrationId).startsWith("TH-" + currentMonthYear + "-IND-");
        assertThat(registrationId).contains("0001"); // Should contain sequence number
        verify(sequenceRepository).save(any(RegistrationSequence.class));
    }

    @Test
    void generateRegistrationId_ShouldIncrementExistingSequence() {
        // Given
        RegistrationSequence existingSequence = RegistrationSequence.builder()
                .monthYear(currentMonthYear)
                .enrollmentType(EnrollmentType.TEAM)
                .planId(planId)
                .currentSequence(5L)
                .build();
        
        when(sequenceRepository.findByMonthYearAndEnrollmentTypeAndPlanIdWithLock(
                anyString(), eq(EnrollmentType.TEAM), eq(planId)))
                .thenReturn(Optional.of(existingSequence));

        // When
        String registrationId = registrationIdService.generateRegistrationId(EnrollmentType.TEAM, planId);

        // Then
        assertThat(registrationId).startsWith("TH-" + currentMonthYear + "-TEAM-");
        assertThat(registrationId).contains("0006"); // Should contain sequence number
        assertThat(existingSequence.getCurrentSequence()).isEqualTo(6L);
        verify(sequenceRepository).save(existingSequence);
    }

    @Test
    void generateRegistrationId_ShouldHandleDifferentEnrollmentTypes() {
        // Given
        RegistrationSequence individualSequence = RegistrationSequence.builder()
                .currentSequence(0L)
                .build();
        
        when(sequenceRepository.findByMonthYearAndEnrollmentTypeAndPlanIdWithLock(
                anyString(), eq(EnrollmentType.INDIVIDUAL), eq(planId)))
                .thenReturn(Optional.of(individualSequence));

        // When
        String individualId = registrationIdService.generateRegistrationId(EnrollmentType.INDIVIDUAL, planId);

        // Then
        assertThat(individualId).contains("-IND-");
        
        // Given
        RegistrationSequence teamSequence = RegistrationSequence.builder()
                .currentSequence(0L)
                .build();
        
        when(sequenceRepository.findByMonthYearAndEnrollmentTypeAndPlanIdWithLock(
                anyString(), eq(EnrollmentType.TEAM), eq(planId)))
                .thenReturn(Optional.of(teamSequence));

        // When
        String teamId = registrationIdService.generateRegistrationId(EnrollmentType.TEAM, planId);

        // Then
        assertThat(teamId).contains("-TEAM-");
    }

    @Test
    void isValidRegistrationId_ShouldValidateCorrectFormat() {
        // Valid registration IDs
        assertThat(registrationIdService.isValidRegistrationId("TH-0825-INDIVIDUAL-000101")).isTrue();
        assertThat(registrationIdService.isValidRegistrationId("TH-1224-TEAM-000299")).isTrue();
        
        // Invalid registration IDs
        assertThat(registrationIdService.isValidRegistrationId("")).isFalse();
        assertThat(registrationIdService.isValidRegistrationId(null)).isFalse();
        assertThat(registrationIdService.isValidRegistrationId("INVALID-FORMAT")).isFalse();
        assertThat(registrationIdService.isValidRegistrationId("TH-0825-INVALID-000101")).isFalse();
        assertThat(registrationIdService.isValidRegistrationId("TH-0825-INDIVIDUAL-ABC")).isFalse();
    }

    @Test
    void extractEnrollmentType_ShouldReturnCorrectType() {
        // Given
        String individualId = "TH-0825-INDIVIDUAL-000101";
        String teamId = "TH-0825-TEAM-000102";

        // When & Then
        assertThat(registrationIdService.extractEnrollmentType(individualId))
                .isEqualTo(EnrollmentType.INDIVIDUAL);
        assertThat(registrationIdService.extractEnrollmentType(teamId))
                .isEqualTo(EnrollmentType.TEAM);
    }

    @Test
    void extractEnrollmentType_ShouldThrowExceptionForInvalidFormat() {
        // Given
        String invalidId = "INVALID-FORMAT";

        // When & Then
        assertThatThrownBy(() -> registrationIdService.extractEnrollmentType(invalidId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid registration ID format");
    }

    @Test
    void extractMonthYear_ShouldReturnCorrectMonthYear() {
        // Given
        String registrationId = "TH-0825-INDIVIDUAL-000101";

        // When
        String monthYear = registrationIdService.extractMonthYear(registrationId);

        // Then
        assertThat(monthYear).isEqualTo("0825");
    }

    @Test
    void generateRegistrationId_ShouldHandleHighSequenceNumbers() {
        // Given
        RegistrationSequence highSequence = RegistrationSequence.builder()
                .monthYear(currentMonthYear)
                .enrollmentType(EnrollmentType.INDIVIDUAL)
                .planId(planId)
                .currentSequence(9999L)
                .build();
        
        when(sequenceRepository.findByMonthYearAndEnrollmentTypeAndPlanIdWithLock(
                anyString(), eq(EnrollmentType.INDIVIDUAL), eq(planId)))
                .thenReturn(Optional.of(highSequence));

        // When
        String registrationId = registrationIdService.generateRegistrationId(EnrollmentType.INDIVIDUAL, planId);

        // Then
        assertThat(registrationId).endsWith("10000"); // Should end with sequence number
        assertThat(highSequence.getCurrentSequence()).isEqualTo(10000L);
    }
}
