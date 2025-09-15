package com.mysillydreams.treasure.domain.model;

import java.time.OffsetDateTime;
import java.util.UUID;
import jakarta.persistence.*;
import lombok.*;

// UserLevel.java
@Entity @Table(name="user_level",
        uniqueConstraints = @UniqueConstraint(columnNames={"user_id","difficulty"}))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class UserLevel {
    @Id @GeneratedValue private UUID id;
    @Column(nullable=false) private UUID userId;
    @Enumerated(EnumType.STRING) @Column(nullable=false)
    private Difficulty difficulty;
    @Column(nullable=false) private int highestLevelReached;
    @Column(nullable=false) private OffsetDateTime updatedAt = OffsetDateTime.now();
}

