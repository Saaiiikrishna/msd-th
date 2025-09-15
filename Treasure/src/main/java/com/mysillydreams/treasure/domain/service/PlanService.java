package com.mysillydreams.treasure.domain.service;

import com.mysillydreams.treasure.cache.CacheNames;
import com.mysillydreams.treasure.domain.model.*;
import com.mysillydreams.treasure.domain.repository.PlanPriceRepository;
import com.mysillydreams.treasure.domain.repository.PlanRepository;
import com.mysillydreams.treasure.domain.repository.PlanSlotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PlanService {

    private final PlanRepository planRepo;
    private final PlanPriceRepository priceRepo;
    private final PlanSlotRepository slotRepo;
    private final ApplicationEventPublisher publisher; // if you want Spring events too

    @Transactional
    @CacheEvict(cacheNames = {CacheNames.PLAN_DETAIL, CacheNames.PLAN_SEARCH}, allEntries = true)
    public Plan createPlan(Plan plan, List<PlanRule> rules, List<Task> tasks,
                           List<PlanDifficulty> diffs, PlanPrice price, PriceProfileSnapshot snap) {
        // attach children
        diffs.forEach(d -> d.setPlan(plan));
        rules.forEach(r -> r.setPlan(plan));
        tasks.forEach(t -> t.setPlan(plan));
        plan.setRules(rules);
        plan.setTasks(tasks);
        plan.setDifficulties(diffs);

        Plan saved = planRepo.save(plan);

        // pricing (base + snapshot)
        price.setPlan(saved);
        price.setPriceProfileSnapshot(snap);
        priceRepo.save(price);

        // default slot row
        PlanSlot slot = PlanSlot.builder()
                .plan(saved)
                .capacityNullMeansOpen(plan.getMaxParticipants())
                .reserved(0)
                .availableView(0)
                .build();
        slotRepo.save(slot);

        // emit plan.updated
        // planEventProducer.planUpdated(saved.getId(), "created");
        return saved;
    }

    @Transactional
    @CacheEvict(cacheNames = {CacheNames.PLAN_DETAIL, CacheNames.PLAN_SEARCH}, allEntries = true)
    public Plan updatePlan(Plan updated) {
        Plan existing = planRepo.findById(updated.getId()).orElseThrow();
        // copy mutable fields…
        existing.setTitle(updated.getTitle());
        existing.setSummary(updated.getSummary());
        existing.setVenueText(updated.getVenueText());
        existing.setCity(updated.getCity());
        existing.setCountry(updated.getCountry());
        existing.setVirtual(updated.isVirtual());
        existing.setTimeWindowType(updated.getTimeWindowType());
        existing.setStartAt(updated.getStartAt());
        existing.setEndAt(updated.getEndAt());
        existing.setMaxParticipants(updated.getMaxParticipants());

        // sync capacity on slot (finite plans)
        slotRepo.findByPlanId(existing.getId()).ifPresent(s -> {
            s.setCapacityNullMeansOpen(existing.getMaxParticipants());
        });

        Plan saved = planRepo.save(existing);
        // planEventProducer.planUpdated(saved.getId(), "updated");
        return saved;
    }

    @Cacheable(cacheNames = CacheNames.PLAN_DETAIL, key = "#planId")
    @Transactional(readOnly = true)
    public Plan getPlanDetail(UUID planId) {
        return planRepo.findWithDetailById(planId).orElseThrow();
    }
}

