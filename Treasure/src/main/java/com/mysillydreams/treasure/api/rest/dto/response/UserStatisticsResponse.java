package com.mysillydreams.treasure.api.rest.dto.response;

import com.mysillydreams.treasure.domain.model.Difficulty;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Builder
public record UserStatisticsResponse(
        UUID userId,
        Difficulty difficulty,
        Integer totalPlansEnrolled,
        Integer totalPlansCompleted,
        Integer totalTasksCompleted,
        Integer highestLevelReached,
        BigDecimal totalScore,
        BigDecimal completionRate,
        Integer averageCompletionTimeMinutes,
        Integer fastestCompletionTimeMinutes,
        Integer currentRank,
        Integer bestRankAchieved,
        Integer totalAchievements,
        Integer currentStreakDays,
        Integer longestStreakDays,
        OffsetDateTime lastActivityDate
) {}
