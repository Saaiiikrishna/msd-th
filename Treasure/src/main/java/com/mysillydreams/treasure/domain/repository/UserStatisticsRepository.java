package com.mysillydreams.treasure.domain.repository;

import com.mysillydreams.treasure.domain.model.Difficulty;
import com.mysillydreams.treasure.domain.model.UserStatistics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserStatisticsRepository extends JpaRepository<UserStatistics, UUID> {
    
    /**
     * Find user statistics by user ID and difficulty
     */
    Optional<UserStatistics> findByUserIdAndDifficulty(UUID userId, Difficulty difficulty);
    
    /**
     * Find all statistics for a user across all difficulties
     */
    List<UserStatistics> findByUserIdOrderByDifficulty(UUID userId);
    
    /**
     * Find top performers by difficulty and total score
     */
    @Query("SELECT us FROM UserStatistics us WHERE us.difficulty = :difficulty " +
           "ORDER BY us.totalScore DESC, us.totalPlansCompleted DESC, us.averageCompletionTimeMinutes ASC")
    List<UserStatistics> findTopPerformersByDifficulty(@Param("difficulty") Difficulty difficulty);
    
    /**
     * Find top performers by difficulty with limit
     */
    @Query("SELECT us FROM UserStatistics us WHERE us.difficulty = :difficulty " +
           "ORDER BY us.totalScore DESC, us.totalPlansCompleted DESC, us.averageCompletionTimeMinutes ASC " +
           "LIMIT :limit")
    List<UserStatistics> findTopPerformersByDifficultyWithLimit(@Param("difficulty") Difficulty difficulty, 
                                                                @Param("limit") int limit);
    
    /**
     * Get user rank by difficulty
     */
    @Query("SELECT COUNT(us) + 1 FROM UserStatistics us WHERE us.difficulty = :difficulty " +
           "AND (us.totalScore > :score OR " +
           "(us.totalScore = :score AND us.totalPlansCompleted > :plansCompleted) OR " +
           "(us.totalScore = :score AND us.totalPlansCompleted = :plansCompleted AND us.averageCompletionTimeMinutes < :avgTime))")
    Long getUserRankByDifficulty(@Param("difficulty") Difficulty difficulty, 
                                @Param("score") BigDecimal score,
                                @Param("plansCompleted") Integer plansCompleted,
                                @Param("avgTime") Integer avgTime);
    
    /**
     * Update user statistics atomically
     */
    @Modifying
    @Query("UPDATE UserStatistics us SET " +
           "us.totalPlansEnrolled = us.totalPlansEnrolled + :enrolledIncrement, " +
           "us.totalPlansCompleted = us.totalPlansCompleted + :completedIncrement, " +
           "us.totalTasksCompleted = us.totalTasksCompleted + :tasksIncrement, " +
           "us.totalScore = us.totalScore + :scoreIncrement, " +
           "us.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE us.userId = :userId AND us.difficulty = :difficulty")
    int updateStatistics(@Param("userId") UUID userId, 
                        @Param("difficulty") Difficulty difficulty,
                        @Param("enrolledIncrement") Integer enrolledIncrement,
                        @Param("completedIncrement") Integer completedIncrement,
                        @Param("tasksIncrement") Integer tasksIncrement,
                        @Param("scoreIncrement") BigDecimal scoreIncrement);
    
    /**
     * Find users with active streaks
     */
    @Query("SELECT us FROM UserStatistics us WHERE us.currentStreakDays > 0 " +
           "AND us.lastActivityDate >= :cutoffDate ORDER BY us.currentStreakDays DESC")
    List<UserStatistics> findActiveStreakUsers(@Param("cutoffDate") OffsetDateTime cutoffDate);
    
    /**
     * Find users by completion rate range
     */
    @Query("SELECT us FROM UserStatistics us WHERE us.difficulty = :difficulty " +
           "AND (CASE WHEN us.totalPlansEnrolled = 0 THEN 0 " +
           "ELSE (us.totalPlansCompleted * 100.0 / us.totalPlansEnrolled) END) " +
           "BETWEEN :minRate AND :maxRate")
    List<UserStatistics> findByCompletionRateRange(@Param("difficulty") Difficulty difficulty,
                                                   @Param("minRate") Double minRate,
                                                   @Param("maxRate") Double maxRate);
}
