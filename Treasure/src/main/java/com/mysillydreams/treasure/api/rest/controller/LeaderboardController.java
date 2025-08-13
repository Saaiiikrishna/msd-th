package com.mysillydreams.treasure.api.rest.controller;

import com.mysillydreams.treasure.api.rest.dto.response.LeaderboardResponse;
import com.mysillydreams.treasure.api.rest.dto.response.UserStatisticsResponse;
import com.mysillydreams.treasure.domain.model.*;
import com.mysillydreams.treasure.domain.service.LeaderboardService;
import com.mysillydreams.treasure.domain.service.UserLevelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * REST controller for leaderboard and statistics functionality
 */
@RestController
@RequestMapping("/api/treasure/v1/leaderboards")
@RequiredArgsConstructor
@Tag(name = "Treasure Hunt - Leaderboards", description = "Leaderboard and user statistics endpoints")
public class LeaderboardController {
    
    private final LeaderboardService leaderboardService;
    private final UserLevelService userLevelService;
    
    /**
     * Get overall leaderboard for a difficulty
     */
    @GetMapping("/overall/{difficulty}")
    public LeaderboardResponse getOverallLeaderboard(
            @PathVariable Difficulty difficulty,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(required = false) EnrollmentType enrollmentType) {
        
        List<Leaderboard> entries;
        if (enrollmentType != null) {
            entries = leaderboardService.getLeaderboard(LeaderboardType.OVERALL, difficulty, enrollmentType, limit);
        } else {
            entries = leaderboardService.getLeaderboard(LeaderboardType.OVERALL, difficulty, limit);
        }
        
        Long totalParticipants = leaderboardService.getTotalParticipants(LeaderboardType.OVERALL, difficulty);
        
        return LeaderboardResponse.builder()
                .leaderboardType(LeaderboardType.OVERALL)
                .difficulty(difficulty)
                .enrollmentType(enrollmentType)
                .entries(entries.stream().map(this::mapToLeaderboardEntry).toList())
                .totalParticipants(totalParticipants)
                .build();
    }
    
    /**
     * Get monthly leaderboard for a difficulty
     */
    @GetMapping("/monthly/{difficulty}")
    public LeaderboardResponse getMonthlyLeaderboard(
            @PathVariable Difficulty difficulty,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(required = false) EnrollmentType enrollmentType) {
        
        List<Leaderboard> entries;
        if (enrollmentType != null) {
            entries = leaderboardService.getLeaderboard(LeaderboardType.MONTHLY, difficulty, enrollmentType, limit);
        } else {
            entries = leaderboardService.getLeaderboard(LeaderboardType.MONTHLY, difficulty, limit);
        }
        
        Long totalParticipants = leaderboardService.getTotalParticipants(LeaderboardType.MONTHLY, difficulty);
        
        return LeaderboardResponse.builder()
                .leaderboardType(LeaderboardType.MONTHLY)
                .difficulty(difficulty)
                .enrollmentType(enrollmentType)
                .entries(entries.stream().map(this::mapToLeaderboardEntry).toList())
                .totalParticipants(totalParticipants)
                .build();
    }
    
    /**
     * Get team leaderboard
     */
    @GetMapping("/teams/{difficulty}")
    public LeaderboardResponse getTeamLeaderboard(
            @PathVariable Difficulty difficulty,
            @RequestParam(defaultValue = "50") int limit) {
        
        List<Leaderboard> entries = leaderboardService.getTeamLeaderboard(LeaderboardType.OVERALL, difficulty, limit);
        Long totalParticipants = leaderboardService.getTotalParticipants(LeaderboardType.OVERALL, difficulty);
        
        return LeaderboardResponse.builder()
                .leaderboardType(LeaderboardType.OVERALL)
                .difficulty(difficulty)
                .enrollmentType(EnrollmentType.TEAM)
                .entries(entries.stream().map(this::mapToLeaderboardEntry).toList())
                .totalParticipants(totalParticipants)
                .build();
    }
    
    /**
     * Get user's position in leaderboard
     */
    @GetMapping("/position/{userId}/{difficulty}")
    public LeaderboardResponse.LeaderboardEntry getUserPosition(
            @PathVariable UUID userId,
            @PathVariable Difficulty difficulty,
            @RequestParam(defaultValue = "OVERALL") LeaderboardType leaderboardType) {
        
        Optional<Leaderboard> userPosition = leaderboardService.getUserPosition(leaderboardType, difficulty, userId);
        
        return userPosition.map(this::mapToLeaderboardEntry).orElse(null);
    }
    
    /**
     * Get users around a specific user's rank
     */
    @GetMapping("/around/{userId}/{difficulty}")
    public LeaderboardResponse getUsersAroundRank(
            @PathVariable UUID userId,
            @PathVariable Difficulty difficulty,
            @RequestParam(defaultValue = "OVERALL") LeaderboardType leaderboardType,
            @RequestParam(defaultValue = "5") int contextSize) {
        
        Optional<Leaderboard> userPosition = leaderboardService.getUserPosition(leaderboardType, difficulty, userId);
        
        if (userPosition.isEmpty()) {
            return LeaderboardResponse.builder()
                    .leaderboardType(leaderboardType)
                    .difficulty(difficulty)
                    .entries(List.of())
                    .totalParticipants(0L)
                    .build();
        }
        
        List<Leaderboard> entries = leaderboardService.getUsersAroundRank(
                leaderboardType, difficulty, userPosition.get().getRankPosition(), contextSize);
        
        Long totalParticipants = leaderboardService.getTotalParticipants(leaderboardType, difficulty);
        
        return LeaderboardResponse.builder()
                .leaderboardType(leaderboardType)
                .difficulty(difficulty)
                .entries(entries.stream().map(this::mapToLeaderboardEntry).toList())
                .totalParticipants(totalParticipants)
                .userPosition(userPosition.get().getRankPosition())
                .build();
    }
    
    /**
     * Get user statistics across all difficulties
     */
    @GetMapping("/statistics/{userId}")
    public List<UserStatisticsResponse> getUserStatistics(@PathVariable UUID userId) {
        List<UserStatistics> statistics = userLevelService.getUserStatistics(userId);
        
        return statistics.stream()
                .map(this::mapToUserStatisticsResponse)
                .toList();
    }
    
    /**
     * Trigger leaderboard regeneration (admin endpoint)
     */
    @PostMapping("/regenerate")
    public void regenerateLeaderboards() {
        leaderboardService.regenerateOverallLeaderboards();
    }
    
    private LeaderboardResponse.LeaderboardEntry mapToLeaderboardEntry(Leaderboard leaderboard) {
        return LeaderboardResponse.LeaderboardEntry.builder()
                .userId(leaderboard.getUserId())
                .rank(leaderboard.getRankPosition())
                .totalScore(leaderboard.getTotalScore())
                .plansCompleted(leaderboard.getPlansCompleted())
                .tasksCompleted(leaderboard.getTasksCompleted())
                .averageCompletionTimeMinutes(leaderboard.getAverageCompletionTimeMinutes())
                .enrollmentType(leaderboard.getEnrollmentType())
                .teamName(leaderboard.getTeamName())
                .build();
    }
    
    private UserStatisticsResponse mapToUserStatisticsResponse(UserStatistics stats) {
        return UserStatisticsResponse.builder()
                .userId(stats.getUserId())
                .difficulty(stats.getDifficulty())
                .totalPlansEnrolled(stats.getTotalPlansEnrolled())
                .totalPlansCompleted(stats.getTotalPlansCompleted())
                .totalTasksCompleted(stats.getTotalTasksCompleted())
                .highestLevelReached(stats.getHighestLevelReached())
                .totalScore(stats.getTotalScore())
                .completionRate(stats.getCompletionRate())
                .averageCompletionTimeMinutes(stats.getAverageCompletionTimeMinutes())
                .fastestCompletionTimeMinutes(stats.getFastestCompletionTimeMinutes())
                .currentRank(stats.getCurrentRank())
                .bestRankAchieved(stats.getBestRankAchieved())
                .totalAchievements(stats.getTotalAchievements())
                .currentStreakDays(stats.getCurrentStreakDays())
                .longestStreakDays(stats.getLongestStreakDays())
                .lastActivityDate(stats.getLastActivityDate())
                .build();
    }
}
