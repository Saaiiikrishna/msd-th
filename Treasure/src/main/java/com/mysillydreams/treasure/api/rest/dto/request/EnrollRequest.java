package com.mysillydreams.treasure.api.rest.dto.request;

import com.mysillydreams.treasure.domain.model.EnrollmentType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record EnrollRequest(
    @NotNull UUID userId,
    @NotNull EnrollmentType enrollmentType,
    @Size(min = 2, max = 100, message = "Team name must be between 2 and 100 characters")
    String teamName,
    Integer teamSize
) {
    public EnrollRequest {
        // Validation for team enrollments
        if (enrollmentType == EnrollmentType.TEAM) {
            if (teamName == null || teamName.trim().isEmpty()) {
                throw new IllegalArgumentException("Team name is required for team enrollment");
            }
            if (teamSize == null || teamSize < 2) {
                throw new IllegalArgumentException("Team size must be at least 2 for team enrollment");
            }
        }
    }
}
