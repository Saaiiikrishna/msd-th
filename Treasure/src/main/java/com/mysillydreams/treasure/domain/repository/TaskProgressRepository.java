package com.mysillydreams.treasure.domain.repository;

import com.mysillydreams.treasure.domain.model.TaskProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TaskProgressRepository extends JpaRepository<TaskProgress, UUID> {
    Optional<TaskProgress> findByEnrollmentIdAndTaskId(UUID enrollmentId, UUID taskId);
}
