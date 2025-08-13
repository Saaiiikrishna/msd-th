package com.mysillydreams.treasure.domain.service;

import com.mysillydreams.treasure.domain.model.*;
import com.mysillydreams.treasure.domain.repository.EnrollmentRepository;
import com.mysillydreams.treasure.domain.repository.PlanRepository;
import com.mysillydreams.treasure.domain.repository.PlanSlotRepository;
import com.mysillydreams.treasure.integrations.port.NotificationPort;
import com.mysillydreams.treasure.integrations.port.PaymentsPort;
import com.mysillydreams.treasure.messaging.producer.EnrollmentEventProducer;
import com.mysillydreams.treasure.cache.CacheNames;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EnrollmentService {

    private final PlanRepository planRepo;
    private final PlanSlotRepository slotRepo;
    private final EnrollmentRepository enrollRepo;
    private final PricingService pricingService;
    private final EnrollmentEventProducer eventProducer;
    private final RegistrationIdService registrationIdService;

    // Ports (could be NOOP or real gRPC adapters depending on feature flags)
    private final Optional<PaymentsPort> paymentsPort;
    private final Optional<NotificationPort> notificationPort;

    @Transactional
    @CacheEvict(cacheNames = {CacheNames.PLAN_DETAIL, CacheNames.PLAN_SEARCH}, allEntries = true)
    public Enrollment enroll(UUID planId, UUID userId, EnrollmentType enrollmentType) {
        return enroll(planId, userId, enrollmentType, null, null);
    }

    @Transactional
    @CacheEvict(cacheNames = {CacheNames.PLAN_DETAIL, CacheNames.PLAN_SEARCH}, allEntries = true)
    public Enrollment enroll(UUID planId, UUID userId, EnrollmentType enrollmentType, String teamName, Integer teamSize) {
        Plan plan = planRepo.findById(planId).orElseThrow();
        PlanSlot slot = slotRepo.findByPlanId(planId).orElseThrow();

        // Validate team enrollment parameters
        if (enrollmentType == EnrollmentType.TEAM) {
            if (teamName == null || teamName.trim().isEmpty()) {
                throw new IllegalArgumentException("Team name is required for team enrollment");
            }
            if (teamSize == null || teamSize < 2) {
                throw new IllegalArgumentException("Team size must be at least 2 for team enrollment");
            }
        }

        EnrollmentMode mode = deduceMode(plan);

        if (plan.getMaxParticipants() != null) {
            // Finite: try transactional reservation
            int slotsNeeded = enrollmentType == EnrollmentType.TEAM ? teamSize : 1;
            int updated = slotRepo.tryReserve(planId, slotsNeeded);
            if (updated == 0) throw new IllegalStateException("No slots available");
        } // Open: no reservation; scarcity is cosmetic

        // Generate registration ID
        String registrationId = registrationIdService.generateRegistrationId(enrollmentType, planId);

        Enrollment e = Enrollment.builder()
                .plan(plan)
                .userId(userId)
                .mode(mode)
                .status(mode == EnrollmentMode.PAY_TO_ENROLL ? EnrollmentStatus.CONFIRMED : EnrollmentStatus.PENDING)
                .paymentStatus(mode == EnrollmentMode.PAY_TO_ENROLL ? PaymentStatus.AWAITING : PaymentStatus.NONE)
                .enrollmentType(enrollmentType)
                .registrationId(registrationId)
                .teamName(teamName)
                .teamSize(teamSize)
                .build();

        Enrollment saved = enrollRepo.save(e);

        // Emit base event
        eventProducer.enrollmentCreated(saved);

        // Route by mode
        if (mode == EnrollmentMode.PAY_TO_ENROLL) {
            // Create payment link if Payments integration is enabled; otherwise just emit event.
            var price = pricingService.previewForPlan(planId, preferCurrency(plan));
            paymentsPort.ifPresentOrElse(pp -> {
                var link = pp.createPaymentLink(
                        saved.getId().toString(),
                        saved.getUserId().toString(),
                        preferCurrency(plan),
                        price.total().toPlainString(),
                        plan.getId().toString()
                );
                // Notify user if Notifications enabled
                link.ifPresent(l ->
                        notificationPort.ifPresent(np -> np.send(
                                saved.getUserId().toString(),
                                "EMAIL",
                                "enrollment_confirmation",
                                Map.of("planTitle", plan.getTitle(), "paymentLink", l.link())
                        ))
                );
            }, () -> {
                // No payments service yet—still tell downstream that payment is requested
                eventProducer.paymentRequested(saved, price.total(), preferCurrency(plan));

            });
        } else {
            // Approval flow—just tell reviewers/ops
            eventProducer.approvalRequested(saved);
            notificationPort.ifPresent(np -> np.send(
                    saved.getUserId().toString(),
                    "EMAIL",
                    "application_received",
                    Map.of("planTitle", plan.getTitle())
            ));
        }

        return saved;
    }

    @Transactional
    @CacheEvict(cacheNames = {CacheNames.PLAN_DETAIL, CacheNames.PLAN_SEARCH}, allEntries = true)
    public Enrollment approve(UUID enrollmentId, UUID approver) {
        Enrollment e = enrollRepo.findWithPlanById(enrollmentId).orElseThrow();
        if (e.getMode() != EnrollmentMode.APPROVAL_REQUIRED) return e;

        e.setStatus(EnrollmentStatus.CONFIRMED);
        e.setPaymentStatus(PaymentStatus.AWAITING);
        e.setApprovalBy(approver);

        Enrollment saved = enrollRepo.save(e);

        var price = pricingService.previewForPlan(e.getPlan().getId(), preferCurrency(e.getPlan()));
        paymentsPort.ifPresentOrElse(pp -> {
            var link = pp.createPaymentLink(
                    saved.getId().toString(),
                    saved.getUserId().toString(),
                    preferCurrency(e.getPlan()),
                    price.total().toPlainString(),
                    e.getPlan().getId().toString()
            );
            link.ifPresent(l -> notificationPort.ifPresent(np -> np.send(
                    saved.getUserId().toString(),
                    "EMAIL",
                    "approval_confirmed",
                    Map.of("planTitle", e.getPlan().getTitle(), "paymentLink", l.link())
            )));
            eventProducer.enrollmentApproved(saved, price.total(), preferCurrency(e.getPlan()));

        }, () -> {
            // No payments yet—emit event so Payments can pick it up later
            eventProducer.enrollmentApproved(saved, price.total(), preferCurrency(e.getPlan()));
            eventProducer.paymentRequested(saved, price.total(), preferCurrency(e.getPlan()));

        });

        return saved;
    }

    @Transactional
    @CacheEvict(cacheNames = {CacheNames.PLAN_DETAIL, CacheNames.PLAN_SEARCH}, allEntries = true)
    public void cancel(UUID enrollmentId) {
        Enrollment e = enrollRepo.findWithPlanById(enrollmentId).orElseThrow();
        if (e.getPlan().getMaxParticipants() != null && e.getStatus() == EnrollmentStatus.CONFIRMED) {
            slotRepo.release(e.getPlan().getId(), 1);
        }
        e.setStatus(EnrollmentStatus.CANCELLED);
        enrollRepo.save(e);
        // Optionally notify and/or emit cancellation events later
    }

    private EnrollmentMode deduceMode(Plan plan) {
        // TODO: read from plan config; default PAY_TO_ENROLL to keep flow simple in v1
        return EnrollmentMode.PAY_TO_ENROLL;
    }

    private String preferCurrency(Plan plan) {
        // TODO: choose by user region later; INR as default now
        return "INR";
    }
}
