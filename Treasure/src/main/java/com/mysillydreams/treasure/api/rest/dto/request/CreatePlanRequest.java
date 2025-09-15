package com.mysillydreams.treasure.api.rest.dto.request;

import com.mysillydreams.treasure.api.rest.dto.response.PriceComponentDto;
import com.mysillydreams.treasure.domain.model.Difficulty;
import com.mysillydreams.treasure.domain.model.TimeWindowType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record CreatePlanRequest(
        UUID subcategoryId,
        String title,
        String summary,
        String venueText,
        String city,
        String country,
        boolean isVirtual,
        TimeWindowType timeWindowType,
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        Integer maxParticipants,
        List<DifficultyBlock> difficulties,
        List<String> rules,
        List<TaskBlock> tasks,
        PriceBlock basePrice,
        List<PriceComponentDto> priceComponents
) {
    public record DifficultyBlock(Difficulty difficulty, int levelNumber, boolean isCrucial) {}
    public record TaskBlock(String title, String details, boolean crucial) {}
    public record PriceBlock(String currency, BigDecimal amount) {}
}