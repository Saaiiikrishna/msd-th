package com.mysillydreams.treasure.domain.service;

import com.mysillydreams.treasure.domain.model.PlanSlot;
import com.mysillydreams.treasure.domain.repository.PlanSlotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ScarcityService {

    private final PlanSlotRepository slotRepo;
    private final StringRedisTemplate redis;

    // Called by consumer on enrollment.created and by a scheduler
    @Transactional
    public void recomputeAvailableView(UUID planId) {
        PlanSlot slot = slotRepo.findByPlanId(planId).orElseThrow();
        if (slot.getCapacityNullMeansOpen() != null) return; // finite → ignore cosmetic

        // Example heuristic: base 20 ± velocity(last hour)
        int velocity = Optional.ofNullable(redis.opsForValue().get("metrics:enroll/hour:"+planId))
                .map(Integer::valueOf).orElse(0);
        int display = Math.max(5, 20 + (velocity / 3));
        slot.setAvailableView(display);
        slotRepo.save(slot);
    }
}

