package com.mysillydreams.treasure.domain.repository;

import com.mysillydreams.treasure.domain.model.GeoFenceRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface GeoFenceRuleRepository extends JpaRepository<GeoFenceRule, UUID> {
    @Query("select g from GeoFenceRule g order by g.updatedAt desc")
    List<GeoFenceRule> findLatest();
}
