package com.mysillydreams.treasure.messaging.producer;

import com.mysillydreams.treasure.domain.model.Enrollment;
import com.mysillydreams.treasure.messaging.TopicNames;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class EnrollmentEventProducer {

    private final KafkaTemplate<String, Object> kafka;

    // unchanged
    public void enrollmentCreated(Enrollment e) {
        kafka.send(TopicNames.ENROLLMENT_CREATED,
                e.getId().toString(),
                Map.of(
                        "event", "enrollment.created",
                        "v", 1,
                        "enrollmentId", e.getId(),
                        "userId", e.getUserId(),
                        "planId", e.getPlan().getId(),
                        "mode", e.getMode().name(),
                        "status", e.getStatus().name(),
                        "ts", Instant.now().toString()
                ));
    }

    // unchanged
    public void approvalRequested(Enrollment e) {
        kafka.send(TopicNames.APPROVAL_REQUESTED,
                e.getId().toString(),
                Map.of(
                        "event", "enrollment.approval_requested",
                        "v", 1,
                        "enrollmentId", e.getId(),
                        "planId", e.getPlan().getId(),
                        "ts", Instant.now().toString()
                ));
    }

    // UPDATED: pass total + currency instead of a Pricing* object
    public void enrollmentApproved(Enrollment e, BigDecimal total, String currency) {
        kafka.send(TopicNames.ENROLLMENT_APPROVED,
                e.getId().toString(),
                Map.of(
                        "event", "enrollment.approved",
                        "v", 1,
                        "enrollmentId", e.getId(),
                        "planId", e.getPlan().getId(),
                        "amount", total,
                        "currency", currency,
                        "ts", Instant.now().toString()
                ));
    }

    // UPDATED: pass total + currency
    public void paymentRequested(Enrollment e, BigDecimal total, String currency) {
        kafka.send(TopicNames.PAYMENT_REQUESTED,
                e.getId().toString(),
                Map.of(
                        "event", "treasure.payment.requested",
                        "v", 1,
                        "enrollmentId", e.getId(),
                        "userId", e.getUserId(),
                        "planId", e.getPlan().getId(),
                        "amount", total,
                        "currency", currency,
                        "ts", Instant.now().toString()
                ));
    }

    // unchanged
    public void taskCompleted(Enrollment e, java.util.UUID taskId) {
        kafka.send(TopicNames.TASK_COMPLETED,
                e.getId().toString(),
                Map.of(
                        "event", "treasure.task.completed",
                        "v", 1,
                        "enrollmentId", e.getId(),
                        "userId", e.getUserId(),
                        "taskId", taskId,
                        "ts", Instant.now().toString()
                ));
    }
}
