package com.mysillydreams.treasure.domain.service;

import com.mysillydreams.treasure.domain.model.EnrollmentType;
import com.mysillydreams.treasure.domain.model.RegistrationSequence;
import com.mysillydreams.treasure.domain.repository.RegistrationSequenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Service for generating unique registration IDs in the format:
 * TH-MMYY-IND-PPSSSS for Individual registrations
 * TH-MMYY-TEAM-PPSSSS for Team registrations
 *
 * Where:
 * - TH = Treasure Hunt
 * - MMYY = Month and Year (e.g., 0825 for August 2025)
 * - IND/TEAM = Enrollment type (shortened)
 * - PPSSSS = 6-digit number where PP is plan ID (01-99) and SSSS is sequence (0001-9999)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RegistrationIdService {
    
    private final RegistrationSequenceRepository sequenceRepository;
    
    private static final String REGISTRATION_PREFIX = "TH";
    private static final DateTimeFormatter MONTH_YEAR_FORMATTER = DateTimeFormatter.ofPattern("MMyy");
    
    /**
     * Generates a unique registration ID for the given enrollment type and plan
     */
    @Transactional
    public String generateRegistrationId(EnrollmentType enrollmentType, UUID planId) {
        String monthYear = OffsetDateTime.now().format(MONTH_YEAR_FORMATTER);

        // Get or create sequence record with pessimistic lock
        RegistrationSequence sequence = sequenceRepository
                .findByMonthYearAndEnrollmentTypeAndPlanIdWithLock(monthYear, enrollmentType, planId)
                .orElseGet(() -> createNewSequence(monthYear, enrollmentType, planId));

        // Get next sequence number
        Long nextSequence = sequence.getNextSequence();
        sequenceRepository.save(sequence);

        // Extract plan number from plan ID (last 2 digits of UUID)
        String planNumber = extractPlanNumber(planId);

        // Convert enrollment type to shortened form
        String enrollmentTypeShort = enrollmentType == EnrollmentType.INDIVIDUAL ? "IND" : "TEAM";

        // Format: TH-MMYY-TYPE-PPSSSS where PP is plan number and SSSS is sequence
        String registrationId = String.format("%s-%s-%s-%s%04d",
                REGISTRATION_PREFIX,
                monthYear,
                enrollmentTypeShort,
                planNumber,
                nextSequence);

        log.info("Generated registration ID: {} for enrollmentType: {}, planId: {}",
                registrationId, enrollmentType, planId);

        return registrationId;
    }
    
    /**
     * Creates a new sequence record for the given month/year, enrollment type, and plan
     */
    private RegistrationSequence createNewSequence(String monthYear, EnrollmentType enrollmentType, UUID planId) {
        return RegistrationSequence.builder()
                .monthYear(monthYear)
                .enrollmentType(enrollmentType)
                .planId(planId)
                .currentSequence(0L)
                .build();
    }
    
    /**
     * Extracts a 2-digit plan number from the plan UUID
     * Uses the last 2 hex digits of the UUID and converts to decimal (mod 100)
     */
    private String extractPlanNumber(UUID planId) {
        String uuidString = planId.toString().replace("-", "");
        String lastTwoHex = uuidString.substring(uuidString.length() - 2);
        int planNumber = Integer.parseInt(lastTwoHex, 16) % 100;
        return String.format("%02d", planNumber);
    }
    
    /**
     * Validates if a registration ID follows the correct format
     */
    public boolean isValidRegistrationId(String registrationId) {
        if (registrationId == null || registrationId.isEmpty()) {
            return false;
        }
        
        // Pattern: TH-MMYY-IND/TEAM-XXXXXX
        String pattern = "^TH-\\d{4}-(INDIVIDUAL|TEAM)-\\d{6}$";
        return registrationId.matches(pattern);
    }
    
    /**
     * Extracts enrollment type from registration ID
     */
    public EnrollmentType extractEnrollmentType(String registrationId) {
        if (!isValidRegistrationId(registrationId)) {
            throw new IllegalArgumentException("Invalid registration ID format: " + registrationId);
        }
        
        String[] parts = registrationId.split("-");
        return EnrollmentType.valueOf(parts[2]);
    }
    
    /**
     * Extracts month/year from registration ID
     */
    public String extractMonthYear(String registrationId) {
        if (!isValidRegistrationId(registrationId)) {
            throw new IllegalArgumentException("Invalid registration ID format: " + registrationId);
        }
        
        String[] parts = registrationId.split("-");
        return parts[1];
    }
}
