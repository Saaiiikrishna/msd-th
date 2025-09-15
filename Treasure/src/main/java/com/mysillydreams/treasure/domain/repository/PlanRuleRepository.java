package com.mysillydreams.treasure.domain.repository;

import com.mysillydreams.treasure.domain.model.PlanRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PlanRuleRepository extends JpaRepository<PlanRule, UUID> {
    List<PlanRule> findByPlanIdOrderByDisplayOrderAsc(UUID planId);
}
