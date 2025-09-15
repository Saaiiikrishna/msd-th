package com.mysillydreams.treasure.api.rest.controller;

import com.mysillydreams.treasure.api.rest.dto.request.ApprovalDecisionRequest;
import com.mysillydreams.treasure.api.rest.dto.request.EnrollRequest;
import com.mysillydreams.treasure.api.rest.dto.response.EnrollmentResponse;
import com.mysillydreams.treasure.domain.model.Enrollment;
import com.mysillydreams.treasure.domain.service.EnrollmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/treasure/v1")
@RequiredArgsConstructor
@Tag(name = "Treasure Hunt - Enrollment", description = "User enrollment operations for treasure hunt plans")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    @Operation(
            summary = "Enroll user in treasure hunt plan",
            description = "Enrolls a user (individual or team) in a treasure hunt plan and generates a unique registration ID"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully enrolled",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = EnrollmentResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
            @ApiResponse(responseCode = "404", description = "Plan not found"),
            @ApiResponse(responseCode = "500", description = "No slots available")
    })
    @PostMapping("/plans/{planId}/enroll")
    public EnrollmentResponse enroll(
            @Parameter(description = "Plan ID to enroll in", required = true)
            @PathVariable UUID planId,
            @Parameter(description = "Enrollment request details", required = true)
            @Validated @RequestBody EnrollRequest req) {
        Enrollment e = enrollmentService.enroll(planId, req.userId(), req.enrollmentType(), req.teamName(), req.teamSize());
        return new EnrollmentResponse(
            e.getId(),
            e.getMode(),
            e.getStatus(),
            e.getPaymentStatus(),
            /*paymentLink*/null,
            e.getRegistrationId(),
            e.getEnrollmentType(),
            e.getTeamName(),
            e.getTeamSize()
        );
    }

    @PostMapping("/enrollments/{id}/approve")
    public EnrollmentResponse approve(@PathVariable UUID id, @Validated @RequestBody ApprovalDecisionRequest req) {
        Enrollment e = enrollmentService.approve(id, req.approvedBy());
        return new EnrollmentResponse(
            e.getId(),
            e.getMode(),
            e.getStatus(),
            e.getPaymentStatus(),
            /*paymentLink*/null,
            e.getRegistrationId(),
            e.getEnrollmentType(),
            e.getTeamName(),
            e.getTeamSize()
        );
    }

    @PostMapping("/enrollments/{id}/reject")
    public EnrollmentResponse reject(@PathVariable UUID id, @Validated @RequestBody ApprovalDecisionRequest req) {
        // simple path: mark rejected; release finite slot if already reserved (not confirmed)
        enrollmentService.cancel(id);
        // Fetch again for response if needed; returning minimal echo:
        return new EnrollmentResponse(id, null, com.mysillydreams.treasure.domain.model.EnrollmentStatus.CANCELLED, null, null, null, null, null, null);
    }

    @PostMapping("/enrollments/{id}/cancel")
    public void cancel(@PathVariable UUID id) {
        enrollmentService.cancel(id);
    }
}
