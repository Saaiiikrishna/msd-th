package com.mysillydreams.treasure.domain.model;

import java.math.BigDecimal;
import java.util.*;
import jakarta.persistence.*;
import lombok.*;

// PlanPrice.java
@Entity @Table(name="plan_price")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class PlanPrice {
    @Id @GeneratedValue private UUID id;
    @ManyToOne(optional=false) @JoinColumn(name="plan_id")
    private Plan plan;
    @Column(nullable=false) private String currency;
    @Column(nullable=false, precision=12, scale=2) private BigDecimal baseAmount;
    @Column(columnDefinition="tsrange") private String validity; // map with custom converter if needed
    @ManyToOne(optional=false) @JoinColumn(name="price_profile_snapshot_id")
    private PriceProfileSnapshot priceProfileSnapshot;
}


