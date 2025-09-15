package com.mysillydreams.treasure.domain.repository;

import com.mysillydreams.treasure.domain.model.Subcategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SubcategoryRepository extends JpaRepository<Subcategory, UUID> {
    List<Subcategory> findByCategoryIdAndActiveTrue(UUID categoryId);
}
