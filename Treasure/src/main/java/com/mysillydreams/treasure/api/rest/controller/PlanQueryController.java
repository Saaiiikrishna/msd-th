package com.mysillydreams.treasure.api.rest.controller;

import com.mysillydreams.treasure.api.rest.dto.request.SearchRequest;
import com.mysillydreams.treasure.api.rest.dto.response.PlanDetailResponse;
import com.mysillydreams.treasure.api.rest.dto.response.PlanSummaryResponse;
import com.mysillydreams.treasure.api.rest.mapper.PlanMapper;
import com.mysillydreams.treasure.domain.model.Plan;
import com.mysillydreams.treasure.domain.repository.PlanPriceRepository;
import com.mysillydreams.treasure.domain.service.SearchService;
import com.mysillydreams.treasure.domain.service.PlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/treasure/v1")
@RequiredArgsConstructor
public class PlanQueryController {

    private final SearchService searchService;
    private final PlanService planService;
    private final PlanPriceRepository priceRepo;
    private final PlanMapper planMapper;

    @GetMapping("/plans")
    public Page<PlanSummaryResponse> search(@Validated SearchRequest req,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "12") int size) {
        Page<Plan> result = searchService.search(req, PageRequest.of(page, size, Sort.by("startAt").ascending()), /*userAge*/req.age()==null?java.util.Optional.empty():java.util.Optional.of(req.age()));
        return result.map(p -> {
            BigDecimal priceFrom = priceRepo.findMinBaseAmount(p.getId());
            String difficultyRange = p.getDifficulties().isEmpty() ? "N/A" :
                    p.getDifficulties().stream().map(d -> d.getDifficulty().name() + " L" + d.getLevelNumber())
                            .sorted().findFirst().orElse("N/A");
            boolean hasFinite = p.getMaxParticipants()!=null;
            int availableView = p.getSlots().isEmpty() ? 0 : p.getSlots().get(0).getAvailableView();
            return planMapper.toSummary(p, difficultyRange, priceFrom, hasFinite, availableView);
        });
    }

    @GetMapping("/plans/{planId}")
    public PlanDetailResponse get(@PathVariable UUID planId) {
        Plan p = planService.getPlanDetail(planId);
        BigDecimal priceFrom = priceRepo.findMinBaseAmount(planId);
        var slot = p.getSlots().isEmpty() ? null : p.getSlots().get(0);
        var pricingPreview = new PlanDetailResponse.PricingPreview(
                priceFrom, java.util.Collections.emptyList(), priceFrom);
        return planMapper.toDetail(
                p, priceFrom, pricingPreview
        );
    }
}
