package com.mysillydreams.payment.dto;

import com.mysillydreams.payment.domain.EnrollmentType;

import java.util.UUID;

/**
 * Enrollment details for invoice generation
 */
public record EnrollmentDetails(
        UUID enrollmentId,
        String registrationId,
        UUID userId,
        UUID planId,
        String planTitle,
        EnrollmentType enrollmentType,
        String teamName,
        Integer teamSize,
        String billingName,
        String billingEmail,
        String billingPhone,
        String billingAddress
) {}
