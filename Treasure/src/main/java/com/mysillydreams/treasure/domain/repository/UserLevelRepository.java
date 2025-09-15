package com.mysillydreams.treasure.domain.repository;

import com.mysillydreams.treasure.domain.model.Difficulty;
import com.mysillydreams.treasure.domain.model.UserLevel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserLevelRepository extends JpaRepository<UserLevel, UUID> {
    Optional<UserLevel> findByUserIdAndDifficulty(UUID userId, Difficulty difficulty);
    List<UserLevel> findByUserId(UUID userId);
}
