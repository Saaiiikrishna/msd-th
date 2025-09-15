package com.mysillydreams.treasure.api.rest.dto.response;

import com.mysillydreams.treasure.domain.model.Difficulty;
import com.mysillydreams.treasure.domain.model.TimeWindowType;
import java.util.List;

public record SearchFiltersResponse(List<String> cities, List<TimeWindowType> timeWindowTypes, List<Integer> levels, List<Difficulty> difficulties, List<AgeBandResponse> ageBands) {}
