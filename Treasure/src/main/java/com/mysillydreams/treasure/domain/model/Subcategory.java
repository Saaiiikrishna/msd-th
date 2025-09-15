package com.mysillydreams.treasure.domain.model;

import java.util.*;
import jakarta.persistence.*;
import lombok.*;

// Subcategory.java
@Entity @Table(name="subcategory")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Subcategory {
    @Id @GeneratedValue private UUID id;
    @ManyToOne(optional=false) @JoinColumn(name="category_id")
    private Category category;
    @Column(nullable=false) private String name;
    private String description;
    @Column(nullable=false) private boolean active = true;

    @ManyToMany
    @JoinTable(
            name="subcategory_age_band",
            joinColumns=@JoinColumn(name="subcategory_id"),
            inverseJoinColumns=@JoinColumn(name="age_band_id")
    )
    private Set<AgeBand> allowedAgeBands = new HashSet<>();
}

