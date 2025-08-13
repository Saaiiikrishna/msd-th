package com.mysillydreams.treasure.api.rest.dto.response;

import java.math.BigDecimal;

public record PriceComponentDto(String type, String calc, BigDecimal value, boolean enforced) {}