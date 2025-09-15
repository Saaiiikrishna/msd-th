package com.mysillydreams.treasure.domain.repository;

import com.mysillydreams.treasure.domain.model.Difficulty;
import com.mysillydreams.treasure.domain.model.EnrollmentType;
import com.mysillydreams.treasure.domain.model.Leaderboard;
import com.mysillydreams.treasure.domain.model.LeaderboardType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LeaderboardRepository extends JpaRepository<Leaderboard, UUID> {
    
    /**
     * Find leaderboard entries by type and difficulty
     */
    List<Leaderboard> findByLeaderboardTypeAndDifficultyOrderByRankPosition(
            LeaderboardType leaderboardType, Difficulty difficulty);
    
    /**
     * Find leaderboard entries with enrollment type filter
     */
    List<Leaderboard> findByLeaderboardTypeAndDifficultyAndEnrollmentTypeOrderByRankPosition(
            LeaderboardType leaderboardType, Difficulty difficulty, EnrollmentType enrollmentType);
    
    /**
     * Find top N entries for a specific leaderboard
     */
    @Query("SELECT l FROM Leaderboard l WHERE l.leaderboardType = :type AND l.difficulty = :difficulty " +
           "ORDER BY l.rankPosition ASC LIMIT :limit")
    List<Leaderboard> findTopEntries(@Param("type") LeaderboardType type, 
                                    @Param("difficulty") Difficulty difficulty,
                                    @Param("limit") int limit);
    
    /**
     * Find user's position in leaderboard
     */
    Optional<Leaderboard> findByLeaderboardTypeAndDifficultyAndUserId(
            LeaderboardType leaderboardType, Difficulty difficulty, UUID userId);
    
    /**
     * Find leaderboard entries for a specific period
     */
    List<Leaderboard> findByLeaderboardTypeAndDifficultyAndPeriodStartAndPeriodEndOrderByRankPosition(
            LeaderboardType leaderboardType, Difficulty difficulty, 
            OffsetDateTime periodStart, OffsetDateTime periodEnd);
    
    /**
     * Delete old leaderboard entries for a specific type and period
     */
    @Modifying
    @Query("DELETE FROM Leaderboard l WHERE l.leaderboardType = :type " +
           "AND l.periodStart = :periodStart AND l.periodEnd = :periodEnd")
    void deleteByTypeAndPeriod(@Param("type") LeaderboardType type,
                              @Param("periodStart") OffsetDateTime periodStart,
                              @Param("periodEnd") OffsetDateTime periodEnd);
    
    /**
     * Find users around a specific rank
     */
    @Query("SELECT l FROM Leaderboard l WHERE l.leaderboardType = :type AND l.difficulty = :difficulty " +
           "AND l.rankPosition BETWEEN :startRank AND :endRank ORDER BY l.rankPosition ASC")
    List<Leaderboard> findUsersAroundRank(@Param("type") LeaderboardType type,
                                         @Param("difficulty") Difficulty difficulty,
                                         @Param("startRank") Integer startRank,
                                         @Param("endRank") Integer endRank);
    
    /**
     * Get total number of participants in leaderboard
     */
    @Query("SELECT COUNT(l) FROM Leaderboard l WHERE l.leaderboardType = :type AND l.difficulty = :difficulty")
    Long getTotalParticipants(@Param("type") LeaderboardType type, @Param("difficulty") Difficulty difficulty);
    
    /**
     * Find team leaderboard entries
     */
    @Query("SELECT l FROM Leaderboard l WHERE l.leaderboardType = :type AND l.difficulty = :difficulty " +
           "AND l.enrollmentType = 'TEAM' AND l.teamName IS NOT NULL " +
           "ORDER BY l.rankPosition ASC")
    List<Leaderboard> findTeamLeaderboard(@Param("type") LeaderboardType type, 
                                         @Param("difficulty") Difficulty difficulty);
}
