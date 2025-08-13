package com.mysillydreams.treasure.messaging.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mysillydreams.treasure.domain.model.Enrollment;
import com.mysillydreams.treasure.domain.model.PaymentStatus;
import com.mysillydreams.treasure.domain.repository.EnrollmentRepository;
import com.mysillydreams.treasure.messaging.TopicNames;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentStatusConsumer {

    private final EnrollmentRepository enrollmentRepo;
    private final ObjectMapper om = new ObjectMapper();

    @KafkaListener(topics = TopicNames.PAYMENT_STATUS_UPDATED, groupId = "treasure-service")
    @Transactional
    @CacheEvict(cacheNames = {"plans:detail","plans:search"}, allEntries = true)
    public void onPaymentStatus(String message) {
        try {
            JsonNode root = om.readTree(message);
            String enrollmentId = root.path("enrollmentId").asText();
            String status = root.path("status").asText();  // "PAID"|"REFUNDED"|...

            Enrollment e = enrollmentRepo.findById(UUID.fromString(enrollmentId)).orElse(null);
            if (e == null) return;

            PaymentStatus newStatus = switch (status) {
                case "PAID"     -> PaymentStatus.PAID;
                case "REFUNDED" -> PaymentStatus.REFUNDED;
                case "AWAITING" -> PaymentStatus.AWAITING;
                default         -> PaymentStatus.NONE;
            };
            if (e.getPaymentStatus() != newStatus) {
                e.setPaymentStatus(newStatus);
                enrollmentRepo.save(e);
                log.info("Enrollment {} payment status -> {}", enrollmentId, newStatus);
            }
        } catch (Exception ex) {
            log.error("Failed to process payment status message: {}", message, ex);
            // Let default error handler route to DLQ if configured
        }
    }
}
