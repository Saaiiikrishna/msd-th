package com.mysillydreams.treasure.domain.model;

import java.time.OffsetDateTime;
import java.util.*;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

// PriceProfileSnapshot.java
@Entity @Table(name="price_profile_snapshot")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class PriceProfileSnapshot {
    @Id @GeneratedValue private UUID id;
    @Column(nullable=false) private String currency;
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable=false)
    private List<Map<String,Object>> components; // [{type,calc,value}]
    @Column(nullable=false) private boolean isEnforced;
    @Column(nullable=false) private OffsetDateTime createdAt = OffsetDateTime.now();
}

