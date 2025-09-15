package com.mysillydreams.treasure.domain.service;

import com.mysillydreams.treasure.api.rest.dto.request.SearchRequest;
import com.mysillydreams.treasure.cache.CacheNames;
import com.mysillydreams.treasure.domain.model.GeoFenceRule;
import com.mysillydreams.treasure.domain.model.Plan;
import com.mysillydreams.treasure.domain.repository.GeoFenceRuleRepository;
import com.mysillydreams.treasure.domain.repository.PlanRepository;
import com.mysillydreams.treasure.search.PlanSpecificationFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final PlanRepository planRepo;
    private final PlanSpecificationFactory specFactory;
    private final GeoFenceRuleRepository fenceRepo;

    @Cacheable(cacheNames = CacheNames.PLAN_SEARCH, key = "T(com.mysillydreams.treasure.cache.CacheKeys).searchKey(#req)")
    @Transactional(readOnly = true)
    public Page<Plan> search(SearchRequest req, Pageable pageable, Optional<Integer> userAge) {
        Optional<GeoFenceRule> fence = fenceRepo.findLatest().stream().findFirst();
        return planRepo.findAll(specFactory.build(req, fence, userAge), pageable);
    }
}

