package com.mysillydreams.treasure.api.rest.dto.response;

import com.mysillydreams.treasure.domain.model.Difficulty;
import com.mysillydreams.treasure.domain.model.EnrollmentType;
import com.mysillydreams.treasure.domain.model.LeaderboardType;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Builder
public record LeaderboardResponse(
        LeaderboardType leaderboardType,
        Difficulty difficulty,
        EnrollmentType enrollmentType,
        List<LeaderboardEntry> entries,
        Long totalParticipants,
        Integer userPosition // User's position in this leaderboard (if applicable)
) {
    
    @Builder
    public record LeaderboardEntry(
            UUID userId,
            Integer rank,
            BigDecimal totalScore,
            Integer plansCompleted,
            Integer tasksCompleted,
            Integer averageCompletionTimeMinutes,
            EnrollmentType enrollmentType,
            String teamName
    ) {}
}
