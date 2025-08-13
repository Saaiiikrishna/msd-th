package com.mysillydreams.treasure.domain.model;

import java.util.*;
import jakarta.persistence.*;
import lombok.*;

// AgeBand.java
@Entity @Table(name="age_band")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class AgeBand {
    @Id @GeneratedValue private UUID id;
    @Column(nullable=false) private String label;
    @Column(nullable=false) private int minAge;
    @Column(nullable=false) private int maxAge;
}

