package com.mysillydreams.treasure.domain.model;

import java.util.*;
import jakarta.persistence.*;
import lombok.*;

// PlanSlot.java
@Entity @Table(name="plan_slot")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class PlanSlot {
    @Id @GeneratedValue private UUID id;
    @ManyToOne(optional=false) @JoinColumn(name="plan_id")
    private Plan plan;
    private Integer capacityNullMeansOpen;
    @Column(nullable=false) private int reserved;
    @Column(nullable=false) private int availableView;
}

