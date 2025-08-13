package com.mysillydreams.treasure.domain.repository;

import com.mysillydreams.treasure.domain.model.PlanDifficulty;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PlanDifficultyRepository extends JpaRepository<PlanDifficulty, UUID> {
    List<PlanDifficulty> findByPlanIdOrderByLevelNumberAsc(UUID planId);
}
