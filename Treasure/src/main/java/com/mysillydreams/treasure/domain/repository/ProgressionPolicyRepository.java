package com.mysillydreams.treasure.domain.repository;

import com.mysillydreams.treasure.domain.model.ProgressionPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ProgressionPolicyRepository extends JpaRepository<ProgressionPolicy, UUID> {
    @Query("select p from ProgressionPolicy p where p.active=true and p.scope='GLOBAL'")
    List<ProgressionPolicy> findActiveGlobal();

    @Query("select p from ProgressionPolicy p where p.active=true and p.scope='COHORT' and p.scopeRef=:cohort")
    List<ProgressionPolicy> findActiveByCohort(@Param("cohort") String cohort);

    @Query("select p from ProgressionPolicy p where p.active=true and p.scope='USER' and p.scopeRef=:userId")
    List<ProgressionPolicy> findActiveByUser(@Param("userId") String userId);
}
