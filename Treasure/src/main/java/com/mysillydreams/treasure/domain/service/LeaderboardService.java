package com.mysillydreams.treasure.domain.service;

import com.mysillydreams.treasure.domain.model.*;
import com.mysillydreams.treasure.domain.repository.LeaderboardRepository;
import com.mysillydreams.treasure.domain.repository.UserStatisticsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service for managing leaderboards and user rankings
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LeaderboardService {
    
    private final LeaderboardRepository leaderboardRepository;
    private final UserStatisticsRepository userStatisticsRepository;
    
    /**
     * Get leaderboard for a specific type and difficulty
     */
    @Transactional(readOnly = true)
    public List<Leaderboard> getLeaderboard(LeaderboardType type, Difficulty difficulty, int limit) {
        return leaderboardRepository.findTopEntries(type, difficulty, limit);
    }
    
    /**
     * Get leaderboard with enrollment type filter
     */
    @Transactional(readOnly = true)
    public List<Leaderboard> getLeaderboard(LeaderboardType type, Difficulty difficulty, 
                                           EnrollmentType enrollmentType, int limit) {
        List<Leaderboard> entries = leaderboardRepository
                .findByLeaderboardTypeAndDifficultyAndEnrollmentTypeOrderByRankPosition(
                        type, difficulty, enrollmentType);
        return entries.stream().limit(limit).toList();
    }
    
    /**
     * Get user's position in leaderboard
     */
    @Transactional(readOnly = true)
    public Optional<Leaderboard> getUserPosition(LeaderboardType type, Difficulty difficulty, UUID userId) {
        return leaderboardRepository.findByLeaderboardTypeAndDifficultyAndUserId(type, difficulty, userId);
    }
    
    /**
     * Get users around a specific rank (for showing context around user's position)
     */
    @Transactional(readOnly = true)
    public List<Leaderboard> getUsersAroundRank(LeaderboardType type, Difficulty difficulty, 
                                               Integer userRank, int contextSize) {
        int startRank = Math.max(1, userRank - contextSize);
        int endRank = userRank + contextSize;
        return leaderboardRepository.findUsersAroundRank(type, difficulty, startRank, endRank);
    }
    
    /**
     * Regenerate overall leaderboard for all difficulties
     */
    @Transactional
    @Async
    public void regenerateOverallLeaderboards() {
        log.info("Starting overall leaderboard regeneration");
        
        for (Difficulty difficulty : Difficulty.values()) {
            regenerateOverallLeaderboard(difficulty);
        }
        
        log.info("Completed overall leaderboard regeneration");
    }
    
    /**
     * Regenerate overall leaderboard for a specific difficulty
     */
    @Transactional
    public void regenerateOverallLeaderboard(Difficulty difficulty) {
        log.info("Regenerating overall leaderboard for difficulty: {}", difficulty);
        
        // Get all user statistics ordered by performance
        List<UserStatistics> topPerformers = userStatisticsRepository.findTopPerformersByDifficulty(difficulty);
        
        // Clear existing overall leaderboard for this difficulty
        leaderboardRepository.deleteByTypeAndPeriod(LeaderboardType.OVERALL, null, null);
        
        // Create new leaderboard entries
        AtomicInteger rank = new AtomicInteger(1);
        List<Leaderboard> leaderboardEntries = topPerformers.stream()
                .map(stats -> createLeaderboardEntry(stats, LeaderboardType.OVERALL, rank.getAndIncrement(), null, null))
                .toList();
        
        leaderboardRepository.saveAll(leaderboardEntries);
        
        // Update current ranks in user statistics
        updateUserRanks(topPerformers, difficulty);
        
        log.info("Regenerated overall leaderboard for difficulty: {} with {} entries", difficulty, leaderboardEntries.size());
    }
    
    /**
     * Regenerate monthly leaderboard
     */
    @Transactional
    public void regenerateMonthlyLeaderboard(Difficulty difficulty, OffsetDateTime monthStart, OffsetDateTime monthEnd) {
        log.info("Regenerating monthly leaderboard for difficulty: {} for period {} to {}", 
                difficulty, monthStart, monthEnd);
        
        // This would require tracking monthly statistics - for now, use overall stats
        // In a full implementation, you'd have monthly aggregated data
        List<UserStatistics> topPerformers = userStatisticsRepository.findTopPerformersByDifficulty(difficulty);
        
        // Clear existing monthly leaderboard for this period
        leaderboardRepository.deleteByTypeAndPeriod(LeaderboardType.MONTHLY, monthStart, monthEnd);
        
        // Create new monthly leaderboard entries
        AtomicInteger rank = new AtomicInteger(1);
        List<Leaderboard> leaderboardEntries = topPerformers.stream()
                .limit(100) // Limit monthly leaderboards
                .map(stats -> createLeaderboardEntry(stats, LeaderboardType.MONTHLY, rank.getAndIncrement(), monthStart, monthEnd))
                .toList();
        
        leaderboardRepository.saveAll(leaderboardEntries);
        
        log.info("Regenerated monthly leaderboard for difficulty: {} with {} entries", difficulty, leaderboardEntries.size());
    }
    
    /**
     * Get team leaderboard
     */
    @Transactional(readOnly = true)
    public List<Leaderboard> getTeamLeaderboard(LeaderboardType type, Difficulty difficulty, int limit) {
        List<Leaderboard> teamEntries = leaderboardRepository.findTeamLeaderboard(type, difficulty);
        return teamEntries.stream().limit(limit).toList();
    }

    /**
     * Get total number of participants in leaderboard
     */
    @Transactional(readOnly = true)
    public Long getTotalParticipants(LeaderboardType type, Difficulty difficulty) {
        return leaderboardRepository.getTotalParticipants(type, difficulty);
    }
    
    /**
     * Update user statistics and potentially trigger leaderboard updates
     */
    @Transactional
    public void updateUserProgress(UUID userId, Difficulty difficulty, 
                                  Integer enrolledIncrement, Integer completedIncrement, 
                                  Integer tasksIncrement, BigDecimal scoreIncrement) {
        
        // Update or create user statistics
        UserStatistics stats = userStatisticsRepository.findByUserIdAndDifficulty(userId, difficulty)
                .orElse(UserStatistics.builder()
                        .userId(userId)
                        .difficulty(difficulty)
                        .build());
        
        // Update statistics
        stats.setTotalPlansEnrolled(stats.getTotalPlansEnrolled() + enrolledIncrement);
        stats.setTotalPlansCompleted(stats.getTotalPlansCompleted() + completedIncrement);
        stats.setTotalTasksCompleted(stats.getTotalTasksCompleted() + tasksIncrement);
        stats.setTotalScore(stats.getTotalScore().add(scoreIncrement));
        stats.updateActivityStreak();
        
        userStatisticsRepository.save(stats);
        
        // Update user's rank in overall leaderboard
        updateUserRankInLeaderboard(userId, difficulty, stats);
        
        log.info("Updated user progress for userId: {}, difficulty: {}, new score: {}", 
                userId, difficulty, stats.getTotalScore());
    }
    
    /**
     * Create a leaderboard entry from user statistics
     */
    private Leaderboard createLeaderboardEntry(UserStatistics stats, LeaderboardType type, 
                                              Integer rank, OffsetDateTime periodStart, OffsetDateTime periodEnd) {
        return Leaderboard.builder()
                .userId(stats.getUserId())
                .difficulty(stats.getDifficulty())
                .leaderboardType(type)
                .rankPosition(rank)
                .totalScore(stats.getTotalScore())
                .plansCompleted(stats.getTotalPlansCompleted())
                .tasksCompleted(stats.getTotalTasksCompleted())
                .averageCompletionTimeMinutes(stats.getAverageCompletionTimeMinutes())
                .periodStart(periodStart)
                .periodEnd(periodEnd)
                .enrollmentType(EnrollmentType.INDIVIDUAL) // Default, would need to be determined from enrollment data
                .build();
    }
    
    /**
     * Update current ranks in user statistics
     */
    private void updateUserRanks(List<UserStatistics> rankedUsers, Difficulty difficulty) {
        for (int i = 0; i < rankedUsers.size(); i++) {
            UserStatistics stats = rankedUsers.get(i);
            int newRank = i + 1;
            
            stats.setCurrentRank(newRank);
            if (stats.getBestRankAchieved() == null || newRank < stats.getBestRankAchieved()) {
                stats.setBestRankAchieved(newRank);
            }
        }
        
        userStatisticsRepository.saveAll(rankedUsers);
    }
    
    /**
     * Update a single user's rank in leaderboard
     */
    private void updateUserRankInLeaderboard(UUID userId, Difficulty difficulty, UserStatistics stats) {
        // Calculate user's rank based on current statistics
        Long rank = userStatisticsRepository.getUserRankByDifficulty(
                difficulty, 
                stats.getTotalScore(), 
                stats.getTotalPlansCompleted(),
                stats.getAverageCompletionTimeMinutes() != null ? stats.getAverageCompletionTimeMinutes() : Integer.MAX_VALUE
        );
        
        // Update or create leaderboard entry
        Optional<Leaderboard> existingEntry = leaderboardRepository
                .findByLeaderboardTypeAndDifficultyAndUserId(LeaderboardType.OVERALL, difficulty, userId);
        
        if (existingEntry.isPresent()) {
            Leaderboard entry = existingEntry.get();
            entry.setRankPosition(rank.intValue());
            entry.setTotalScore(stats.getTotalScore());
            entry.setPlansCompleted(stats.getTotalPlansCompleted());
            entry.setTasksCompleted(stats.getTotalTasksCompleted());
            entry.setAverageCompletionTimeMinutes(stats.getAverageCompletionTimeMinutes());
            leaderboardRepository.save(entry);
        } else {
            Leaderboard newEntry = createLeaderboardEntry(stats, LeaderboardType.OVERALL, rank.intValue(), null, null);
            leaderboardRepository.save(newEntry);
        }
        
        // Update user statistics with new rank
        stats.setCurrentRank(rank.intValue());
        if (stats.getBestRankAchieved() == null || rank < stats.getBestRankAchieved()) {
            stats.setBestRankAchieved(rank.intValue());
        }
    }
}
