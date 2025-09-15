package com.mysillydreams.treasure.domain.repository;

import com.mysillydreams.treasure.domain.model.AgeBand;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AgeBandRepository extends JpaRepository<AgeBand, UUID> {}
