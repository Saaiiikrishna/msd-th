package com.mysillydreams.treasure.api.rest.dto.response;

import com.mysillydreams.treasure.domain.model.EnrollmentMode;
import com.mysillydreams.treasure.domain.model.EnrollmentStatus;
import com.mysillydreams.treasure.domain.model.EnrollmentType;
import com.mysillydreams.treasure.domain.model.PaymentStatus;

import java.util.UUID;

public record EnrollmentResponse(
        UUID id,
        EnrollmentMode mode,
        EnrollmentStatus status,
        PaymentStatus paymentStatus,
        String paymentLink,
        String registrationId,
        EnrollmentType enrollmentType,
        String teamName,
        Integer teamSize
) {}
