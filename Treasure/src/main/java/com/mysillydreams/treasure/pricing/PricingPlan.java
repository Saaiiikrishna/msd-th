package com.mysillydreams.treasure.pricing;

import java.math.BigDecimal;
import java.util.List;

public record PricingPlan(BigDecimal base,
                          List<PricingComponent> components,
                          BigDecimal total) {
    public record PricingComponent(String type, String calc, BigDecimal value, boolean enforced) {}
}
