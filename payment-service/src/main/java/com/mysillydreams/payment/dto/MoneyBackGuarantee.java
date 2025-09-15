package com.mysillydreams.payment.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Money back guarantee widget configuration
 */
@Data
@Builder
public class MoneyBackGuarantee {
    private boolean enabled;
    private String title;
    private String description;
    private String guaranteePeriod;
    private List<String> conditions;
    private String iconUrl;
    private String badgeColor;
    private String displayPosition;
    private Map<String, String> style;
}
