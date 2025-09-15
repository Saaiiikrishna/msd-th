package com.mysillydreams.treasure.api.rest.dto.request;

import com.mysillydreams.treasure.domain.model.Difficulty;
import com.mysillydreams.treasure.domain.model.TimeWindowType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record SearchRequest(
        UUID subcategoryId,
        Difficulty difficulty,
        Integer level,
        OffsetDateTime dateFrom,
        OffsetDateTime dateTo,
        TimeWindowType timeWindowType,
        BigDecimal priceMin,
        BigDecimal priceMax,
        String city,
        Integer withinKm,
        Boolean hasSlots,
        Integer age
) {}
