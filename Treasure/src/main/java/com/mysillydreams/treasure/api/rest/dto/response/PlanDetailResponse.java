package com.mysillydreams.treasure.api.rest.dto.response;

import com.mysillydreams.treasure.domain.model.TimeWindowType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record PlanDetailResponse(
        UUID id, String title, String summary, String venueText,
        String city, String country, boolean isVirtual,
        TimeWindowType timeWindowType, OffsetDateTime startAt, OffsetDateTime endAt,
        List<RuleResponse> rules, List<TaskResponse> tasks,
        SlotResponse slot, PricingPreview pricingPreview
) {
    public record RuleResponse(String text, int order) {}
    public record TaskResponse(UUID id, String title, String details, boolean crucial) {}
    public record SlotResponse(Integer capacity, int reserved, int availableView) {}
    public record PricingPreview(BigDecimal base, List<PricingComponent> components, BigDecimal total) {}
    public record PricingComponent(String type, String calc, BigDecimal value, boolean enforced) {}
}
