package com.mysillydreams.payment.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Security badge widget configuration
 */
@Data
@Builder
public class SecurityBadge {
    private boolean enabled;
    private String title;
    private String description;
    private List<String> securityFeatures;
    private List<Map<String, Object>> certifications;
    private List<String> trustLogos;
    private String displayPosition;
    private Map<String, String> style;
}
