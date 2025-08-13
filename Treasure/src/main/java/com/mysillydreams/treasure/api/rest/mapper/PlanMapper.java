package com.mysillydreams.treasure.api.rest.mapper;

import com.mysillydreams.treasure.api.rest.dto.response.PlanDetailResponse;
import com.mysillydreams.treasure.api.rest.dto.response.PlanSummaryResponse;
import com.mysillydreams.treasure.domain.model.Plan;
import org.mapstruct.Mapper;

import java.math.BigDecimal;

@Mapper(componentModel = "spring", uses = { /* sub-mappers */ })
public interface PlanMapper {
    PlanDetailResponse toDetail(Plan e, BigDecimal priceFrom, PlanDetailResponse.PricingPreview pricing);
    PlanSummaryResponse toSummary(Plan e, String difficultyRange, BigDecimal priceFrom, boolean hasFiniteSlots, int availableView);
}
