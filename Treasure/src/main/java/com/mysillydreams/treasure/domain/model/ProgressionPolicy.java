package com.mysillydreams.treasure.domain.model;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.Map;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

// ProgressionPolicy.java
@Entity @Table(name="progression_policy")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ProgressionPolicy {
    @Id @GeneratedValue private UUID id;
    @Column(nullable=false) private String name;       // e.g., "default"
    @Column(nullable=false) private String scope;      // GLOBAL|COHORT|USER
    private String scopeRef;                           // cohort name or userId
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable=false)
    private Map<String,Object> policyJson;
    @Column(nullable=false) private boolean active = true;
    @Column(nullable=false) private OffsetDateTime createdAt = OffsetDateTime.now();
}

