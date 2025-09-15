package com.mysillydreams.treasure.domain.service;

import com.mysillydreams.treasure.domain.model.*;
import com.mysillydreams.treasure.domain.repository.LeaderboardRepository;
import com.mysillydreams.treasure.domain.repository.UserStatisticsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeaderboardServiceTest {

    @Mock
    private LeaderboardRepository leaderboardRepository;

    @Mock
    private UserStatisticsRepository userStatisticsRepository;

    @InjectMocks
    private LeaderboardService leaderboardService;

    private UUID userId1, userId2, userId3;
    private UserStatistics stats1, stats2, stats3;

    @BeforeEach
    void setUp() {
        userId1 = UUID.randomUUID();
        userId2 = UUID.randomUUID();
        userId3 = UUID.randomUUID();

        stats1 = UserStatistics.builder()
                .userId(userId1)
                .difficulty(Difficulty.BEGINNER)
                .totalScore(BigDecimal.valueOf(1000))
                .totalPlansCompleted(10)
                .averageCompletionTimeMinutes(45)
                .build();

        stats2 = UserStatistics.builder()
                .userId(userId2)
                .difficulty(Difficulty.BEGINNER)
                .totalScore(BigDecimal.valueOf(800))
                .totalPlansCompleted(8)
                .averageCompletionTimeMinutes(50)
                .build();

        stats3 = UserStatistics.builder()
                .userId(userId3)
                .difficulty(Difficulty.BEGINNER)
                .totalScore(BigDecimal.valueOf(1200))
                .totalPlansCompleted(12)
                .averageCompletionTimeMinutes(40)
                .build();
    }

    @Test
    void getLeaderboard_ShouldReturnTopEntries() {
        // Given
        List<Leaderboard> expectedEntries = Arrays.asList(
                createLeaderboardEntry(userId3, 1, BigDecimal.valueOf(1200)),
                createLeaderboardEntry(userId1, 2, BigDecimal.valueOf(1000)),
                createLeaderboardEntry(userId2, 3, BigDecimal.valueOf(800))
        );

        when(leaderboardRepository.findTopEntries(LeaderboardType.OVERALL, Difficulty.BEGINNER, 10))
                .thenReturn(expectedEntries);

        // When
        List<Leaderboard> result = leaderboardService.getLeaderboard(
                LeaderboardType.OVERALL, Difficulty.BEGINNER, 10);

        // Then
        assertThat(result).hasSize(3);
        assertThat(result.get(0).getUserId()).isEqualTo(userId3);
        assertThat(result.get(0).getRankPosition()).isEqualTo(1);
        assertThat(result.get(1).getUserId()).isEqualTo(userId1);
        assertThat(result.get(1).getRankPosition()).isEqualTo(2);
    }

    @Test
    void getUserPosition_ShouldReturnUserLeaderboardEntry() {
        // Given
        Leaderboard userEntry = createLeaderboardEntry(userId1, 5, BigDecimal.valueOf(1000));
        
        when(leaderboardRepository.findByLeaderboardTypeAndDifficultyAndUserId(
                LeaderboardType.OVERALL, Difficulty.BEGINNER, userId1))
                .thenReturn(Optional.of(userEntry));

        // When
        Optional<Leaderboard> result = leaderboardService.getUserPosition(
                LeaderboardType.OVERALL, Difficulty.BEGINNER, userId1);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getUserId()).isEqualTo(userId1);
        assertThat(result.get().getRankPosition()).isEqualTo(5);
    }

    @Test
    void regenerateOverallLeaderboard_ShouldCreateNewLeaderboardEntries() {
        // Given
        List<UserStatistics> topPerformers = Arrays.asList(stats3, stats1, stats2);
        
        when(userStatisticsRepository.findTopPerformersByDifficulty(Difficulty.BEGINNER))
                .thenReturn(topPerformers);
        
        doNothing().when(leaderboardRepository).deleteByTypeAndPeriod(any(), any(), any());
        when(leaderboardRepository.saveAll(anyList())).thenReturn(Arrays.asList());
        when(userStatisticsRepository.saveAll(anyList())).thenReturn(topPerformers);

        // When
        leaderboardService.regenerateOverallLeaderboard(Difficulty.BEGINNER);

        // Then
        verify(leaderboardRepository).deleteByTypeAndPeriod(LeaderboardType.OVERALL, null, null);
        verify(leaderboardRepository).saveAll(argThat(entries -> {
            List<Leaderboard> leaderboardEntries = (List<Leaderboard>) entries;
            return leaderboardEntries.size() == 3 &&
                   leaderboardEntries.get(0).getUserId().equals(userId3) &&
                   leaderboardEntries.get(0).getRankPosition() == 1 &&
                   leaderboardEntries.get(1).getUserId().equals(userId1) &&
                   leaderboardEntries.get(1).getRankPosition() == 2;
        }));
    }

    @Test
    void updateUserProgress_ShouldUpdateStatisticsAndRank() {
        // Given
        when(userStatisticsRepository.findByUserIdAndDifficulty(userId1, Difficulty.BEGINNER))
                .thenReturn(Optional.of(stats1));
        
        when(userStatisticsRepository.save(any(UserStatistics.class)))
                .thenReturn(stats1);
        
        when(userStatisticsRepository.getUserRankByDifficulty(
                eq(Difficulty.BEGINNER), any(BigDecimal.class), anyInt(), anyInt()))
                .thenReturn(3L);
        
        when(leaderboardRepository.findByLeaderboardTypeAndDifficultyAndUserId(
                LeaderboardType.OVERALL, Difficulty.BEGINNER, userId1))
                .thenReturn(Optional.empty());
        
        when(leaderboardRepository.save(any(Leaderboard.class)))
                .thenReturn(new Leaderboard());

        // When
        leaderboardService.updateUserProgress(
                userId1, Difficulty.BEGINNER, 1, 1, 5, BigDecimal.valueOf(100));

        // Then
        verify(userStatisticsRepository).save(argThat(stats -> {
            return stats.getTotalPlansEnrolled() == 11 && // 10 + 1
                   stats.getTotalPlansCompleted() == 11 && // 10 + 1
                   stats.getTotalTasksCompleted() == 5 &&  // 0 + 5
                   stats.getTotalScore().equals(BigDecimal.valueOf(1100)); // 1000 + 100
        }));
        
        verify(leaderboardRepository).save(any(Leaderboard.class));
    }

    @Test
    void updateUserProgress_ShouldCreateNewStatisticsIfNotExists() {
        // Given
        when(userStatisticsRepository.findByUserIdAndDifficulty(userId1, Difficulty.INTERMEDIATE))
                .thenReturn(Optional.empty());
        
        when(userStatisticsRepository.save(any(UserStatistics.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        
        when(userStatisticsRepository.getUserRankByDifficulty(
                eq(Difficulty.INTERMEDIATE), any(BigDecimal.class), anyInt(), anyInt()))
                .thenReturn(1L);
        
        when(leaderboardRepository.findByLeaderboardTypeAndDifficultyAndUserId(
                LeaderboardType.OVERALL, Difficulty.INTERMEDIATE, userId1))
                .thenReturn(Optional.empty());
        
        when(leaderboardRepository.save(any(Leaderboard.class)))
                .thenReturn(new Leaderboard());

        // When
        leaderboardService.updateUserProgress(
                userId1, Difficulty.INTERMEDIATE, 1, 0, 3, BigDecimal.valueOf(50));

        // Then
        verify(userStatisticsRepository).save(argThat(stats -> {
            return stats.getUserId().equals(userId1) &&
                   stats.getDifficulty() == Difficulty.INTERMEDIATE &&
                   stats.getTotalPlansEnrolled() == 1 &&
                   stats.getTotalPlansCompleted() == 0 &&
                   stats.getTotalTasksCompleted() == 3 &&
                   stats.getTotalScore().equals(BigDecimal.valueOf(50));
        }));
    }

    @Test
    void getUsersAroundRank_ShouldReturnContextualEntries() {
        // Given
        Integer userRank = 5;
        int contextSize = 2;
        List<Leaderboard> contextEntries = Arrays.asList(
                createLeaderboardEntry(UUID.randomUUID(), 3, BigDecimal.valueOf(1500)),
                createLeaderboardEntry(UUID.randomUUID(), 4, BigDecimal.valueOf(1200)),
                createLeaderboardEntry(userId1, 5, BigDecimal.valueOf(1000)),
                createLeaderboardEntry(UUID.randomUUID(), 6, BigDecimal.valueOf(800)),
                createLeaderboardEntry(UUID.randomUUID(), 7, BigDecimal.valueOf(600))
        );

        when(leaderboardRepository.findUsersAroundRank(
                LeaderboardType.OVERALL, Difficulty.BEGINNER, 3, 7))
                .thenReturn(contextEntries);

        // When
        List<Leaderboard> result = leaderboardService.getUsersAroundRank(
                LeaderboardType.OVERALL, Difficulty.BEGINNER, userRank, contextSize);

        // Then
        assertThat(result).hasSize(5);
        assertThat(result.get(2).getUserId()).isEqualTo(userId1);
        assertThat(result.get(2).getRankPosition()).isEqualTo(5);
    }

    @Test
    void getTeamLeaderboard_ShouldReturnTeamEntries() {
        // Given
        List<Leaderboard> teamEntries = Arrays.asList(
                createTeamLeaderboardEntry(userId1, 1, "Team Alpha"),
                createTeamLeaderboardEntry(userId2, 2, "Team Beta")
        );

        when(leaderboardRepository.findTeamLeaderboard(LeaderboardType.OVERALL, Difficulty.BEGINNER))
                .thenReturn(teamEntries);

        // When
        List<Leaderboard> result = leaderboardService.getTeamLeaderboard(
                LeaderboardType.OVERALL, Difficulty.BEGINNER, 10);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getTeamName()).isEqualTo("Team Alpha");
        assertThat(result.get(1).getTeamName()).isEqualTo("Team Beta");
    }

    private Leaderboard createLeaderboardEntry(UUID userId, Integer rank, BigDecimal score) {
        return Leaderboard.builder()
                .userId(userId)
                .difficulty(Difficulty.BEGINNER)
                .leaderboardType(LeaderboardType.OVERALL)
                .rankPosition(rank)
                .totalScore(score)
                .plansCompleted(10)
                .tasksCompleted(50)
                .enrollmentType(EnrollmentType.INDIVIDUAL)
                .build();
    }

    private Leaderboard createTeamLeaderboardEntry(UUID userId, Integer rank, String teamName) {
        return Leaderboard.builder()
                .userId(userId)
                .difficulty(Difficulty.BEGINNER)
                .leaderboardType(LeaderboardType.OVERALL)
                .rankPosition(rank)
                .totalScore(BigDecimal.valueOf(1000))
                .plansCompleted(10)
                .tasksCompleted(50)
                .enrollmentType(EnrollmentType.TEAM)
                .teamName(teamName)
                .build();
    }
}
