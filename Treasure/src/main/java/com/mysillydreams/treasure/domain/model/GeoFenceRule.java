package com.mysillydreams.treasure.domain.model;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.List;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

// GeoFenceRule.java
@Entity @Table(name="geofence_rule")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class GeoFenceRule {
    @Id @GeneratedValue private UUID id;
    @Column(nullable=false) private boolean enabled;
    @Column(nullable=false) private String scope; // CITY|STATE|COUNTRY
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable=false)
    private List<String> values;
    @Column(nullable=false) private OffsetDateTime updatedAt = OffsetDateTime.now();
}

