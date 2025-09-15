package com.mysillydreams.treasure.api.rest.dto.response;

import com.mysillydreams.treasure.domain.model.TimeWindowType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PlanSummaryResponse(
        UUID id, String title, String subcategoryName,
        String city, boolean isVirtual, TimeWindowType timeWindowType,
        OffsetDateTime startAt, OffsetDateTime endAt,
        String difficultyRange, BigDecimal priceFrom,
        boolean hasFiniteSlots, int availableView
) {}
