package com.mysillydreams.treasure.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Comprehensive user statistics for treasure hunt performance tracking
 */
@Entity 
@Table(name = "user_statistics", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "difficulty"}))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class UserStatistics {
    
    @Id @GeneratedValue 
    private UUID id;
    
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Difficulty difficulty;
    
    // Progress tracking
    @Column(name = "total_plans_enrolled", nullable = false)
    @Builder.Default
    private Integer totalPlansEnrolled = 0;
    
    @Column(name = "total_plans_completed", nullable = false)
    @Builder.Default
    private Integer totalPlansCompleted = 0;
    
    @Column(name = "total_tasks_completed", nullable = false)
    @Builder.Default
    private Integer totalTasksCompleted = 0;
    
    @Column(name = "highest_level_reached", nullable = false)
    @Builder.Default
    private Integer highestLevelReached = 0;
    
    // Performance metrics
    @Column(name = "total_score", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal totalScore = BigDecimal.ZERO;
    
    @Column(name = "average_completion_time_minutes")
    private Integer averageCompletionTimeMinutes;
    
    @Column(name = "fastest_completion_time_minutes")
    private Integer fastestCompletionTimeMinutes;
    
    // Ranking and achievements
    @Column(name = "current_rank")
    private Integer currentRank;
    
    @Column(name = "best_rank_achieved")
    private Integer bestRankAchieved;
    
    @Column(name = "total_achievements", nullable = false)
    @Builder.Default
    private Integer totalAchievements = 0;
    
    // Streak tracking
    @Column(name = "current_streak_days", nullable = false)
    @Builder.Default
    private Integer currentStreakDays = 0;
    
    @Column(name = "longest_streak_days", nullable = false)
    @Builder.Default
    private Integer longestStreakDays = 0;
    
    @Column(name = "last_activity_date")
    private OffsetDateTime lastActivityDate;
    
    // Timestamps
    @Column(nullable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();
    
    @Column(nullable = false)
    @Builder.Default
    private OffsetDateTime updatedAt = OffsetDateTime.now();
    
    @PreUpdate 
    void touch() { 
        this.updatedAt = OffsetDateTime.now(); 
    }
    
    /**
     * Calculate completion rate as percentage
     */
    public BigDecimal getCompletionRate() {
        if (totalPlansEnrolled == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(totalPlansCompleted)
                .divide(BigDecimal.valueOf(totalPlansEnrolled), 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }
    
    /**
     * Update activity streak
     */
    public void updateActivityStreak() {
        OffsetDateTime now = OffsetDateTime.now();
        
        if (lastActivityDate == null) {
            currentStreakDays = 1;
            longestStreakDays = Math.max(longestStreakDays, currentStreakDays);
        } else {
            long daysBetween = java.time.Duration.between(lastActivityDate, now).toDays();
            
            if (daysBetween == 1) {
                // Consecutive day
                currentStreakDays++;
                longestStreakDays = Math.max(longestStreakDays, currentStreakDays);
            } else if (daysBetween > 1) {
                // Streak broken
                currentStreakDays = 1;
            }
            // If daysBetween == 0, same day activity, no change to streak
        }
        
        lastActivityDate = now;
    }
}
