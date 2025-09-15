package com.mysillydreams.treasure.domain.repository;

import com.mysillydreams.treasure.domain.model.EnrollmentType;
import com.mysillydreams.treasure.domain.model.RegistrationSequence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;

public interface RegistrationSequenceRepository extends JpaRepository<RegistrationSequence, UUID> {
    
    /**
     * Find registration sequence with pessimistic lock to ensure thread safety
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT rs FROM RegistrationSequence rs WHERE rs.monthYear = :monthYear " +
           "AND rs.enrollmentType = :enrollmentType AND rs.planId = :planId")
    Optional<RegistrationSequence> findByMonthYearAndEnrollmentTypeAndPlanIdWithLock(
            @Param("monthYear") String monthYear,
            @Param("enrollmentType") EnrollmentType enrollmentType,
            @Param("planId") UUID planId);
    
    /**
     * Increment sequence atomically
     */
    @Modifying
    @Query("UPDATE RegistrationSequence rs SET rs.currentSequence = rs.currentSequence + 1, " +
           "rs.updatedAt = CURRENT_TIMESTAMP WHERE rs.id = :id")
    int incrementSequence(@Param("id") UUID id);
}
