package com.mysillydreams.treasure.domain.model;

import java.util.*;
import jakarta.persistence.*;
import lombok.*;

// Task.java
@Entity @Table(name="task")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Task {
    @Id @GeneratedValue private UUID id;
    @ManyToOne(optional=false) @JoinColumn(name="plan_id")
    private Plan plan;
    @Column(nullable=false) private String title;
    @Column(columnDefinition="text") private String details;
    @Column(nullable=false) private boolean crucial;
}

