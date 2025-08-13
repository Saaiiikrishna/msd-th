package com.mysillydreams.treasure.api.rest.controller;

import com.mysillydreams.treasure.api.rest.dto.request.*;
import com.mysillydreams.treasure.api.rest.dto.response.*;
import com.mysillydreams.treasure.api.rest.mapper.CategoryMapper;
import com.mysillydreams.treasure.api.rest.mapper.SubcategoryMapper;
import com.mysillydreams.treasure.domain.model.*;
import com.mysillydreams.treasure.domain.repository.*;
import com.mysillydreams.treasure.domain.service.PlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.mapstruct.factory.Mappers;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("/api/treasure/v1/admin")
@RequiredArgsConstructor
@Tag(name = "Admin - Categories", description = "Administrative operations for managing categories, subcategories, and plans")
public class AdminController {

    private final CategoryRepository categoryRepo;
    private final SubcategoryRepository subcategoryRepo;
    private final AgeBandRepository ageBandRepo;
    private final PlanService planService;
    private final PriceProfileSnapshotRepository snapshotRepo;
    private final PlanPriceRepository planPriceRepo;
    private final PlanSlotRepository planSlotRepo;
    private final CategoryMapper categoryMapper;
    private final SubcategoryMapper subcategoryMapper;

    // Category Management
    @Operation(
            summary = "Create a new category",
            description = """
                    Creates a new category for organizing educational content. Categories help users
                    discover and browse content by topic areas.

                    **Required Fields:**
                    - `name`: Unique category name (e.g., "Technology", "Arts", "Science")
                    - `description`: Detailed description of what content this category contains
                    - `audience`: Target audience type (INDIVIDUAL, CORPORATE, EDUCATIONAL)

                    **Optional Fields:**
                    - `tags`: Additional metadata as key-value pairs for enhanced categorization
                    """,
            tags = {"Admin - Categories"}
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Category created successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CategoryResponse.class),
                            examples = @ExampleObject(
                                    name = "Technology Category",
                                    value = """
                                            {
                                              "id": "c2ce06de-7b41-4532-9aa1-22b31c9523e1",
                                              "name": "Technology",
                                              "description": "Technology and programming courses",
                                              "audience": "INDIVIDUAL",
                                              "active": true,
                                              "tags": {
                                                "difficulty": "beginner",
                                                "featured": "true"
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request data",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Validation Error",
                                    value = """
                                            {
                                              "error": "Validation failed",
                                              "message": "Name is required and must be unique"
                                            }
                                            """
                            )
                    )
            )
    })
    @PostMapping("/categories")
    public CategoryResponse createCategory(
            @Parameter(
                    description = "Category creation request with name, description, audience, and optional tags",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CreateCategoryRequest.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Technology Category",
                                            value = """
                                                    {
                                                      "name": "Technology",
                                                      "description": "Technology and programming courses",
                                                      "audience": "INDIVIDUAL",
                                                      "tags": {
                                                        "difficulty": "beginner",
                                                        "featured": "true"
                                                      }
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Corporate Training",
                                            value = """
                                                    {
                                                      "name": "Corporate Training",
                                                      "description": "Professional development courses for corporate teams",
                                                      "audience": "CORPORATE"
                                                    }
                                                    """
                                    )
                            }
                    )
            )
            @Validated @RequestBody CreateCategoryRequest req) {

        // Create category entity
        Category c = Category.builder()
                .name(req.name())
                .description(req.description())
                .audience(req.audience())
                .tags(req.tags())
                .active(true)
                .build();

        Category saved = categoryRepo.save(c);

        // Create response
        return new CategoryResponse(
                saved.getId(),
                saved.getName(),
                saved.getDescription(),
                saved.getAudience(),
                saved.isActive(),
                saved.getTags()
        );
    }

    // Subcategory
    @PostMapping("/subcategories")
    @Transactional
    public SubcategoryResponse createSubcategory(@Validated @RequestBody CreateSubcategoryRequest req) {
        Category cat = categoryRepo.findById(req.categoryId()).orElseThrow();
        Subcategory s = new Subcategory();
        s.setCategory(cat); s.setName(req.name()); s.setDescription(req.description()); s.setActive(true);
        if (req.ageBandIds()!=null && !req.ageBandIds().isEmpty()) {
            var bands = ageBandRepo.findAllById(req.ageBandIds());
            s.setAllowedAgeBands(new HashSet<>(bands));
        }
        return subcategoryMapper.toResponse(subcategoryRepo.save(s));
    }

    // Plan (create minimal, including rules/tasks/pricing)
    @PostMapping("/plans")
    @Transactional
    public PlanDetailResponse createPlan(@Validated @RequestBody CreatePlanRequest req) {
        Subcategory sub = subcategoryRepo.findById(req.subcategoryId()).orElseThrow();
        Plan plan = Plan.builder()
                .subcategory(sub)
                .title(req.title())
                .summary(req.summary())
                .venueText(req.venueText())
                .city(req.city()).country(req.country())
                .isVirtual(req.isVirtual())
                .timeWindowType(req.timeWindowType())
                .startAt(req.startAt()).endAt(req.endAt())
                .maxParticipants(req.maxParticipants())
                .build();

        // children
        List<PlanDifficulty> diffs = req.difficulties()==null ? List.of() :
                req.difficulties().stream()
                        .map(d -> PlanDifficulty.builder().difficulty(d.difficulty()).levelNumber(d.levelNumber()).isCrucial(d.isCrucial()).build())
                        .toList();
        List<PlanRule> rules = req.rules()==null ? List.of() :
                new java.util.ArrayList<>() {{
                    int i=0; for (String r : req.rules()) add(PlanRule.builder().ruleText(r).displayOrder(i++).build());
                }};
        List<Task> tasks = req.tasks()==null ? List.of() :
                req.tasks().stream().map(t -> Task.builder().title(t.title()).details(t.details()).crucial(t.crucial()).build()).toList();

        // pricing
        PriceProfileSnapshot snap = PriceProfileSnapshot.builder()
                .currency(req.basePrice().currency())
                .components( req.priceComponents()==null ? List.of() :
                        req.priceComponents().stream().map(pc ->
                                Map.<String,Object>of("type", pc.type(), "calc", pc.calc(), "value", pc.value(), "isEnforced", pc.enforced())
                        ).toList())
                .isEnforced(true)
                .build();

        PlanPrice price = PlanPrice.builder()
                .currency(req.basePrice().currency())
                .baseAmount(req.basePrice().amount())
                .priceProfileSnapshot(snap)
                .build();

        var saved = planService.createPlan(plan, rules, tasks, diffs, price, snapshotRepo.save(snap));
        BigDecimal priceFrom = planPriceRepo.findMinBaseAmount(saved.getId());
        var pricingPreview = new com.mysillydreams.treasure.api.rest.dto.response.PlanDetailResponse.PricingPreview(priceFrom, List.of(), priceFrom);
        return com.mysillydreams.treasure.api.rest.mapper.PlanMapperImplFactory.INSTANCE
                .toDetail(saved, priceFrom, pricingPreview); // see factory below
    }

    // slot tuning (finite: transactional cap; open: cosmetic only)
    @PostMapping("/plans/{planId}/slot-tuning")
    @Transactional
    public void slotTuning(@PathVariable UUID planId, @RequestParam(required=false) Integer capacity, @RequestParam(required=false) Integer availableView) {
        var slot = planSlotRepo.findByPlanId(planId).orElseThrow();
        if (capacity != null) slot.setCapacityNullMeansOpen(capacity);
        if (availableView != null && slot.getCapacityNullMeansOpen()==null) slot.setAvailableView(availableView);
        planSlotRepo.save(slot);
    }

    // Age bands & geofence & policy endpoints can be added similarly…
}
