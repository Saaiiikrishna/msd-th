package com.mysillydreams.treasure.domain.model;

import java.util.*;
import jakarta.persistence.*;
import lombok.*;

// PlanDifficulty.java
@Entity @Table(name="plan_difficulty")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class PlanDifficulty {
    @Id @GeneratedValue private UUID id;
    @ManyToOne(optional=false) @JoinColumn(name="plan_id")
    private Plan plan;
    @Enumerated(EnumType.STRING) @Column(nullable=false)
    private Difficulty difficulty;
    @Column(nullable=false) private int levelNumber;
    @Column(nullable=false) private boolean isCrucial;
}

