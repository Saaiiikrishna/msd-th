package com.mysillydreams.treasure.domain.model;

import java.time.OffsetDateTime;
import java.util.UUID;
import jakarta.persistence.*;
import lombok.*;

// TaskProgress.java
@Entity @Table(name="task_progress",
        uniqueConstraints = @UniqueConstraint(columnNames={"enrollment_id","task_id"}))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class TaskProgress {
    @Id @GeneratedValue private UUID id;
    @ManyToOne(optional=false) @JoinColumn(name="enrollment_id")
    private Enrollment enrollment;
    @ManyToOne(optional=false) @JoinColumn(name="task_id")
    private Task task;
    @Enumerated(EnumType.STRING) @Column(nullable=false)
    private TaskStatus status;
    @Column(nullable=false) private OffsetDateTime updatedAt = OffsetDateTime.now();

    @PreUpdate void touch(){ this.updatedAt = OffsetDateTime.now(); }
}

