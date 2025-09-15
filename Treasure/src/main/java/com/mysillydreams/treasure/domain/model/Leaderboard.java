package com.mysillydreams.treasure.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Leaderboard entries for treasure hunt rankings
 */
@Entity 
@Table(name = "leaderboard",
       indexes = {
           @Index(name = "idx_leaderboard_difficulty_rank", columnList = "difficulty, rank_position"),
           @Index(name = "idx_leaderboard_period", columnList = "leaderboard_type, period_start, period_end"),
           @Index(name = "idx_leaderboard_user", columnList = "user_id")
       })
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Leaderboard {
    
    @Id @GeneratedValue 
    private UUID id;
    
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Difficulty difficulty;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "leaderboard_type", nullable = false)
    private LeaderboardType leaderboardType;
    
    @Column(name = "rank_position", nullable = false)
    private Integer rankPosition;
    
    @Column(name = "total_score", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalScore;
    
    @Column(name = "plans_completed", nullable = false)
    private Integer plansCompleted;
    
    @Column(name = "tasks_completed", nullable = false)
    private Integer tasksCompleted;
    
    @Column(name = "average_completion_time_minutes")
    private Integer averageCompletionTimeMinutes;
    
    // Period tracking for time-based leaderboards
    @Column(name = "period_start")
    private OffsetDateTime periodStart;
    
    @Column(name = "period_end")
    private OffsetDateTime periodEnd;
    
    // Metadata
    @Column(name = "enrollment_type")
    @Enumerated(EnumType.STRING)
    private EnrollmentType enrollmentType; // For separate individual/team rankings
    
    @Column(name = "team_name")
    private String teamName; // For team leaderboards
    
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
}
