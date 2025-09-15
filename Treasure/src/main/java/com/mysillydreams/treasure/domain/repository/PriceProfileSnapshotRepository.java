package com.mysillydreams.treasure.domain.repository;

import com.mysillydreams.treasure.domain.model.PriceProfileSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PriceProfileSnapshotRepository extends JpaRepository<PriceProfileSnapshot, UUID> {}
