package com.mysillydreams.treasure.domain.service;

import com.mysillydreams.treasure.domain.model.Difficulty;
import com.mysillydreams.treasure.domain.model.UserLevel;
import com.mysillydreams.treasure.domain.model.UserStatistics;
import com.mysillydreams.treasure.domain.repository.UserLevelRepository;
import com.mysillydreams.treasure.domain.repository.UserStatisticsRepository;
import com.mysillydreams.treasure.domain.service.PolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserLevelService {
    private final UserLevelRepository repo;
    private final PolicyService policyService;
    private final LeaderboardService leaderboardService;
    private final UserStatisticsRepository userStatisticsRepository;

    @Transactional
    public void evaluateOnTaskCompletion(UUID userId) {
        // Get user's current levels across all difficulties
        Map<Difficulty, Integer> currentLevels = getSummary(userId);

        // For each difficulty, check if user can advance
        for (Difficulty difficulty : Difficulty.values()) {
            int currentLevel = currentLevels.getOrDefault(difficulty, 0);

            // Check if user can unlock next level based on policy
            if (canUnlockNextLevel(userId, difficulty, currentLevel)) {
                updateUserLevel(userId, difficulty, currentLevel + 1);

                // Update leaderboard statistics
                leaderboardService.updateUserProgress(userId, difficulty, 0, 0, 1,
                    calculateScoreForTaskCompletion(difficulty));
            }
        }
    }

    @Transactional(readOnly = true)
    public Map<Difficulty,Integer> getSummary(UUID userId) {
        return repo.findByUserId(userId).stream()
                .collect(Collectors.toMap(UserLevel::getDifficulty, UserLevel::getHighestLevelReached));
    }

    @Transactional(readOnly = true)
    public List<UserStatistics> getUserStatistics(UUID userId) {
        return userStatisticsRepository.findByUserIdOrderByDifficulty(userId);
    }

    @Transactional
    public void onPlanEnrollment(UUID userId, Difficulty difficulty) {
        // Update statistics for plan enrollment
        leaderboardService.updateUserProgress(userId, difficulty, 1, 0, 0, BigDecimal.ZERO);
    }

    @Transactional
    public void onPlanCompletion(UUID userId, Difficulty difficulty, int tasksCompleted, int completionTimeMinutes) {
        // Calculate score based on difficulty and performance
        BigDecimal score = calculateScoreForPlanCompletion(difficulty, tasksCompleted, completionTimeMinutes);

        // Update statistics for plan completion
        leaderboardService.updateUserProgress(userId, difficulty, 0, 1, tasksCompleted, score);

        // Update average completion time
        updateAverageCompletionTime(userId, difficulty, completionTimeMinutes);
    }

    private boolean canUnlockNextLevel(UUID userId, Difficulty difficulty, int currentLevel) {
        // Apply policy service rules
        return switch (difficulty) {
            case BEGINNER -> true; // Always can progress in beginner
            case INTERMEDIATE -> {
                Map<String, Object> counters = Map.of("currentLevel", currentLevel);
                yield policyService.canUnlockIntermediate(userId, counters);
            }
            case ADVANCED -> {
                Map<String, Object> counters = Map.of("currentLevel", currentLevel);
                yield policyService.canUnlockAdvanced(userId, counters);
            }
        };
    }

    private void updateUserLevel(UUID userId, Difficulty difficulty, int newLevel) {
        UserLevel userLevel = repo.findByUserIdAndDifficulty(userId, difficulty)
                .orElse(UserLevel.builder()
                        .userId(userId)
                        .difficulty(difficulty)
                        .highestLevelReached(0)
                        .build());

        if (newLevel > userLevel.getHighestLevelReached()) {
            userLevel.setHighestLevelReached(newLevel);
            userLevel.setUpdatedAt(OffsetDateTime.now());
            repo.save(userLevel);
        }
    }

    private BigDecimal calculateScoreForTaskCompletion(Difficulty difficulty) {
        return switch (difficulty) {
            case BEGINNER -> BigDecimal.valueOf(10);
            case INTERMEDIATE -> BigDecimal.valueOf(25);
            case ADVANCED -> BigDecimal.valueOf(50);
        };
    }

    private BigDecimal calculateScoreForPlanCompletion(Difficulty difficulty, int tasksCompleted, int completionTimeMinutes) {
        BigDecimal baseScore = switch (difficulty) {
            case BEGINNER -> BigDecimal.valueOf(100);
            case INTERMEDIATE -> BigDecimal.valueOf(250);
            case ADVANCED -> BigDecimal.valueOf(500);
        };

        // Bonus for number of tasks completed
        BigDecimal taskBonus = BigDecimal.valueOf(tasksCompleted * 5);

        // Time bonus (faster completion gets more points)
        BigDecimal timeBonus = BigDecimal.ZERO;
        if (completionTimeMinutes > 0) {
            // Bonus decreases as time increases (max 50% bonus for very fast completion)
            double timeFactor = Math.max(0, 1.0 - (completionTimeMinutes / 120.0)); // 2 hours baseline
            timeBonus = baseScore.multiply(BigDecimal.valueOf(timeFactor * 0.5));
        }

        return baseScore.add(taskBonus).add(timeBonus);
    }

    private void updateAverageCompletionTime(UUID userId, Difficulty difficulty, int completionTimeMinutes) {
        UserStatistics stats = userStatisticsRepository.findByUserIdAndDifficulty(userId, difficulty)
                .orElse(null);

        if (stats != null && completionTimeMinutes > 0) {
            if (stats.getAverageCompletionTimeMinutes() == null) {
                stats.setAverageCompletionTimeMinutes(completionTimeMinutes);
            } else {
                // Calculate running average
                int totalCompleted = stats.getTotalPlansCompleted();
                int newAverage = ((stats.getAverageCompletionTimeMinutes() * (totalCompleted - 1)) + completionTimeMinutes) / totalCompleted;
                stats.setAverageCompletionTimeMinutes(newAverage);
            }

            // Update fastest time
            if (stats.getFastestCompletionTimeMinutes() == null || completionTimeMinutes < stats.getFastestCompletionTimeMinutes()) {
                stats.setFastestCompletionTimeMinutes(completionTimeMinutes);
            }

            userStatisticsRepository.save(stats);
        }
    }
}
