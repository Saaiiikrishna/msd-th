package com.mysillydreams.treasure.domain.repository;

import com.mysillydreams.treasure.domain.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {}
