package com.mysillydreams.treasure.api.rest.controller;

import com.mysillydreams.treasure.domain.model.*;
import com.mysillydreams.treasure.domain.repository.LeaderboardRepository;
import com.mysillydreams.treasure.domain.repository.UserStatisticsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
@Transactional
class LeaderboardControllerIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private LeaderboardRepository leaderboardRepository;

    @Autowired
    private UserStatisticsRepository userStatisticsRepository;

    private MockMvc mockMvc;
    private UUID userId1, userId2, userId3;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        
        userId1 = UUID.randomUUID();
        userId2 = UUID.randomUUID();
        userId3 = UUID.randomUUID();

        // Create test user statistics
        UserStatistics stats1 = UserStatistics.builder()
                .userId(userId1)
                .difficulty(Difficulty.BEGINNER)
                .totalScore(BigDecimal.valueOf(1000))
                .totalPlansCompleted(10)
                .totalTasksCompleted(50)
                .averageCompletionTimeMinutes(45)
                .currentRank(2)
                .build();

        UserStatistics stats2 = UserStatistics.builder()
                .userId(userId2)
                .difficulty(Difficulty.BEGINNER)
                .totalScore(BigDecimal.valueOf(1200))
                .totalPlansCompleted(12)
                .totalTasksCompleted(60)
                .averageCompletionTimeMinutes(40)
                .currentRank(1)
                .build();

        UserStatistics stats3 = UserStatistics.builder()
                .userId(userId3)
                .difficulty(Difficulty.BEGINNER)
                .totalScore(BigDecimal.valueOf(800))
                .totalPlansCompleted(8)
                .totalTasksCompleted(40)
                .averageCompletionTimeMinutes(50)
                .currentRank(3)
                .build();

        userStatisticsRepository.saveAll(Arrays.asList(stats1, stats2, stats3));

        // Create test leaderboard entries
        Leaderboard entry1 = Leaderboard.builder()
                .userId(userId2)
                .difficulty(Difficulty.BEGINNER)
                .leaderboardType(LeaderboardType.OVERALL)
                .rankPosition(1)
                .totalScore(BigDecimal.valueOf(1200))
                .plansCompleted(12)
                .tasksCompleted(60)
                .averageCompletionTimeMinutes(40)
                .enrollmentType(EnrollmentType.INDIVIDUAL)
                .build();

        Leaderboard entry2 = Leaderboard.builder()
                .userId(userId1)
                .difficulty(Difficulty.BEGINNER)
                .leaderboardType(LeaderboardType.OVERALL)
                .rankPosition(2)
                .totalScore(BigDecimal.valueOf(1000))
                .plansCompleted(10)
                .tasksCompleted(50)
                .averageCompletionTimeMinutes(45)
                .enrollmentType(EnrollmentType.INDIVIDUAL)
                .build();

        Leaderboard entry3 = Leaderboard.builder()
                .userId(userId3)
                .difficulty(Difficulty.BEGINNER)
                .leaderboardType(LeaderboardType.OVERALL)
                .rankPosition(3)
                .totalScore(BigDecimal.valueOf(800))
                .plansCompleted(8)
                .tasksCompleted(40)
                .averageCompletionTimeMinutes(50)
                .enrollmentType(EnrollmentType.INDIVIDUAL)
                .build();

        leaderboardRepository.saveAll(Arrays.asList(entry1, entry2, entry3));
    }

    @Test
    void getOverallLeaderboard_ShouldReturnRankedEntries() throws Exception {
        mockMvc.perform(get("/api/treasure/v1/leaderboards/overall/{difficulty}", "BEGINNER")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.leaderboardType").value("OVERALL"))
                .andExpect(jsonPath("$.difficulty").value("BEGINNER"))
                .andExpect(jsonPath("$.entries").isArray())
                .andExpect(jsonPath("$.entries.length()").value(3))
                .andExpect(jsonPath("$.entries[0].userId").value(userId2.toString()))
                .andExpect(jsonPath("$.entries[0].rank").value(1))
                .andExpect(jsonPath("$.entries[0].totalScore").value(1200))
                .andExpect(jsonPath("$.entries[1].userId").value(userId1.toString()))
                .andExpect(jsonPath("$.entries[1].rank").value(2))
                .andExpect(jsonPath("$.entries[2].userId").value(userId3.toString()))
                .andExpect(jsonPath("$.entries[2].rank").value(3));
    }

    @Test
    void getOverallLeaderboard_ShouldFilterByEnrollmentType() throws Exception {
        mockMvc.perform(get("/api/treasure/v1/leaderboards/overall/{difficulty}", "BEGINNER")
                        .param("enrollmentType", "INDIVIDUAL")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enrollmentType").value("INDIVIDUAL"))
                .andExpect(jsonPath("$.entries").isArray())
                .andExpect(jsonPath("$.entries.length()").value(3));
    }

    @Test
    void getUserPosition_ShouldReturnUserRank() throws Exception {
        mockMvc.perform(get("/api/treasure/v1/leaderboards/position/{userId}/{difficulty}", 
                        userId1.toString(), "BEGINNER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId1.toString()))
                .andExpect(jsonPath("$.rank").value(2))
                .andExpect(jsonPath("$.totalScore").value(1000));
    }

    @Test
    void getUsersAroundRank_ShouldReturnContextualEntries() throws Exception {
        mockMvc.perform(get("/api/treasure/v1/leaderboards/around/{userId}/{difficulty}", 
                        userId1.toString(), "BEGINNER")
                        .param("contextSize", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userPosition").value(2))
                .andExpect(jsonPath("$.entries").isArray())
                .andExpect(jsonPath("$.entries.length()").value(3)); // Rank 1, 2, 3 (context size 1)
    }

    @Test
    void getUserStatistics_ShouldReturnUserStats() throws Exception {
        mockMvc.perform(get("/api/treasure/v1/leaderboards/statistics/{userId}", userId1.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].userId").value(userId1.toString()))
                .andExpect(jsonPath("$[0].difficulty").value("BEGINNER"))
                .andExpect(jsonPath("$[0].totalScore").value(1000))
                .andExpect(jsonPath("$[0].totalPlansCompleted").value(10))
                .andExpect(jsonPath("$[0].currentRank").value(2))
                .andExpect(jsonPath("$[0].completionRate").exists());
    }

    @Test
    void getMonthlyLeaderboard_ShouldReturnMonthlyRankings() throws Exception {
        // Create monthly leaderboard entries
        Leaderboard monthlyEntry = Leaderboard.builder()
                .userId(userId1)
                .difficulty(Difficulty.BEGINNER)
                .leaderboardType(LeaderboardType.MONTHLY)
                .rankPosition(1)
                .totalScore(BigDecimal.valueOf(500))
                .plansCompleted(5)
                .tasksCompleted(25)
                .periodStart(OffsetDateTime.now().withDayOfMonth(1))
                .periodEnd(OffsetDateTime.now().withDayOfMonth(1).plusMonths(1).minusDays(1))
                .enrollmentType(EnrollmentType.INDIVIDUAL)
                .build();

        leaderboardRepository.save(monthlyEntry);

        mockMvc.perform(get("/api/treasure/v1/leaderboards/monthly/{difficulty}", "BEGINNER")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.leaderboardType").value("MONTHLY"))
                .andExpect(jsonPath("$.difficulty").value("BEGINNER"))
                .andExpect(jsonPath("$.entries").isArray());
    }

    @Test
    void getTeamLeaderboard_ShouldReturnTeamRankings() throws Exception {
        // Create team leaderboard entries
        UUID teamUserId = UUID.randomUUID();
        
        Leaderboard teamEntry = Leaderboard.builder()
                .userId(teamUserId)
                .difficulty(Difficulty.BEGINNER)
                .leaderboardType(LeaderboardType.OVERALL)
                .rankPosition(1)
                .totalScore(BigDecimal.valueOf(2000))
                .plansCompleted(5)
                .tasksCompleted(50)
                .enrollmentType(EnrollmentType.TEAM)
                .teamName("Team Alpha")
                .build();

        leaderboardRepository.save(teamEntry);

        mockMvc.perform(get("/api/treasure/v1/leaderboards/teams/{difficulty}", "BEGINNER")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enrollmentType").value("TEAM"))
                .andExpect(jsonPath("$.entries").isArray())
                .andExpect(jsonPath("$.entries[0].teamName").value("Team Alpha"))
                .andExpect(jsonPath("$.entries[0].enrollmentType").value("TEAM"));
    }

    @Test
    void regenerateLeaderboards_ShouldTriggerRegeneration() throws Exception {
        mockMvc.perform(post("/api/treasure/v1/leaderboards/regenerate"))
                .andExpect(status().isOk());
        
        // Note: In a real test, you might want to verify that the regeneration actually happened
        // by checking the leaderboard entries or using test doubles
    }

    @Test
    void getLeaderboard_ShouldHandleDifferentDifficulties() throws Exception {
        // Create entries for different difficulties
        UserStatistics intermediateStats = UserStatistics.builder()
                .userId(userId1)
                .difficulty(Difficulty.INTERMEDIATE)
                .totalScore(BigDecimal.valueOf(500))
                .totalPlansCompleted(5)
                .currentRank(1)
                .build();

        userStatisticsRepository.save(intermediateStats);

        Leaderboard intermediateEntry = Leaderboard.builder()
                .userId(userId1)
                .difficulty(Difficulty.INTERMEDIATE)
                .leaderboardType(LeaderboardType.OVERALL)
                .rankPosition(1)
                .totalScore(BigDecimal.valueOf(500))
                .plansCompleted(5)
                .tasksCompleted(25)
                .enrollmentType(EnrollmentType.INDIVIDUAL)
                .build();

        leaderboardRepository.save(intermediateEntry);

        // Test BEGINNER difficulty
        mockMvc.perform(get("/api/treasure/v1/leaderboards/overall/{difficulty}", "BEGINNER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.difficulty").value("BEGINNER"))
                .andExpect(jsonPath("$.entries.length()").value(3));

        // Test INTERMEDIATE difficulty
        mockMvc.perform(get("/api/treasure/v1/leaderboards/overall/{difficulty}", "INTERMEDIATE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.difficulty").value("INTERMEDIATE"))
                .andExpect(jsonPath("$.entries.length()").value(1))
                .andExpect(jsonPath("$.entries[0].userId").value(userId1.toString()));
    }

    @Test
    void getUserPosition_ShouldReturnNullForNonExistentUser() throws Exception {
        UUID nonExistentUserId = UUID.randomUUID();
        
        mockMvc.perform(get("/api/treasure/v1/leaderboards/position/{userId}/{difficulty}", 
                        nonExistentUserId.toString(), "BEGINNER"))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
    }
}
