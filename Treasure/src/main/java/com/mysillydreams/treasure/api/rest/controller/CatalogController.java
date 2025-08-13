package com.mysillydreams.treasure.api.rest.controller;

import com.mysillydreams.treasure.api.rest.dto.response.*;
import com.mysillydreams.treasure.api.rest.mapper.CategoryMapper;
import com.mysillydreams.treasure.api.rest.mapper.SubcategoryMapper;
import com.mysillydreams.treasure.domain.model.Subcategory;
import com.mysillydreams.treasure.domain.repository.CategoryRepository;
import com.mysillydreams.treasure.domain.repository.SubcategoryRepository;
import com.mysillydreams.treasure.domain.repository.AgeBandRepository;
import com.mysillydreams.treasure.api.rest.dto.response.AgeBandResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/treasure/v1/catalog")
@RequiredArgsConstructor
public class CatalogController {

    private final CategoryRepository categoryRepo;
    private final SubcategoryRepository subcategoryRepo;
    private final AgeBandRepository ageBandRepo;
    private final CategoryMapper categoryMapper;
    private final SubcategoryMapper subcategoryMapper;

    @GetMapping("/categories")
    public List<CategoryResponse> categories() {
        return categoryRepo.findAll().stream().map(categoryMapper::toResponse).toList();
    }

    @GetMapping("/categories/{categoryId}/subcategories")
    public List<SubcategoryResponse> subcategories(@PathVariable UUID categoryId) {
        List<Subcategory> subs = subcategoryRepo.findByCategoryIdAndActiveTrue(categoryId);
        return subs.stream().map(subcategoryMapper::toResponse).toList();
    }

    @GetMapping("/filters")
    public SearchFiltersResponse filters() {
        List<String> cities = List.of("Hyderabad","Bengaluru","Mumbai"); // TODO: fetch from distinct plans
        var timeWindowTypes = java.util.Arrays.asList(
                com.mysillydreams.treasure.domain.model.TimeWindowType.values());
        var difficulties = java.util.Arrays.asList(
                com.mysillydreams.treasure.domain.model.Difficulty.values());
        List<Integer> levels = java.util.stream.IntStream.rangeClosed(0, 200).boxed().toList();
        List<AgeBandResponse> ageBands = ageBandRepo.findAll().stream()
                .map(a -> new AgeBandResponse(a.getId(), a.getLabel(), a.getMinAge(), a.getMaxAge()))
                .toList();
        return new SearchFiltersResponse(cities, timeWindowTypes, levels, difficulties, ageBands);
    }
}

