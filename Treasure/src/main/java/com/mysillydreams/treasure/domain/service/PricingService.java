package com.mysillydreams.treasure.domain.service;

import com.mysillydreams.treasure.domain.model.PlanPrice;
import com.mysillydreams.treasure.domain.repository.PlanPriceRepository;
import com.mysillydreams.treasure.pricing.PricingPlan;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PricingService {
    private final PlanPriceRepository priceRepo;

    @Transactional(readOnly = true)
    public PricingPlan previewForPlan(UUID planId, String currency) {
        List<PlanPrice> prices = priceRepo.findActiveByPlanAndCurrency(planId, currency);
        if (prices.isEmpty()) throw new IllegalStateException("No price configured for currency " + currency);
        PlanPrice p = prices.get(0);

        BigDecimal total = p.getBaseAmount();
        List<PricingPlan.PricingComponent> comps = new ArrayList<>();
        for (Map<String,Object> c : p.getPriceProfileSnapshot().getComponents()) {
            var type = String.valueOf(c.get("type"));
            var calc = String.valueOf(c.get("calc")); // PCT|FLAT
            var val  = new BigDecimal(String.valueOf(c.get("value")));
            BigDecimal add = "PCT".equalsIgnoreCase(calc)
                    ? p.getBaseAmount().multiply(val).divide(BigDecimal.valueOf(100))
                    : val;
            total = total.add(add);
            comps.add(new PricingPlan.PricingComponent(type, calc, val, Boolean.TRUE.equals(c.get("isEnforced"))));
        }
        return new PricingPlan(p.getBaseAmount(), comps, total);
    }
}
