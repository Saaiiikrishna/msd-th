package com.mysillydreams.treasure.domain.model;

import java.util.*;
import jakarta.persistence.*;
import lombok.*;

// PlanRule.java
@Entity @Table(name="plan_rule")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class PlanRule {
    @Id @GeneratedValue private UUID id;
    @ManyToOne(optional=false) @JoinColumn(name="plan_id")
    private Plan plan;
    @Column(nullable=false, columnDefinition="text")
    private String ruleText;
    @Column(nullable=false) private int displayOrder;
}

