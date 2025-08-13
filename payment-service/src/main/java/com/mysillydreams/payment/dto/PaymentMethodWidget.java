package com.mysillydreams.payment.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Payment Methods widget configuration
 */
@Data
@Builder
public class PaymentMethodWidget {
    private boolean enabled;
    private String title;
    private List<Map<String, Object>> supportedMethods;
    private String displayPosition;
    private Map<String, String> style;
}
