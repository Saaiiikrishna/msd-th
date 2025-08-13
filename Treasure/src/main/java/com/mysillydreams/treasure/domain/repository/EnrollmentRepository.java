package com.mysillydreams.treasure.domain.repository;

import com.mysillydreams.treasure.domain.model.Enrollment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EnrollmentRepository extends JpaRepository<Enrollment, UUID> {
    @EntityGraph(attributePaths = {"plan"})
    Optional<Enrollment> findWithPlanById(UUID id);
    List<Enrollment> findByPlanId(UUID planId);
}
