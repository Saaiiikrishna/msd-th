package com.mysillydreams.treasure.domain.repository;

import com.mysillydreams.treasure.domain.model.Plan;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface PlanRepository extends JpaRepository<Plan, UUID>, JpaSpecificationExecutor<Plan> {
    @EntityGraph(attributePaths = {"prices","rules","tasks","slots","difficulties"})
    Optional<Plan> findWithDetailById(UUID id);
}
