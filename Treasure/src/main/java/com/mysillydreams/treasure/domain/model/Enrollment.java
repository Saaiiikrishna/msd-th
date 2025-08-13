package com.mysillydreams.treasure.domain.model;

import java.time.OffsetDateTime;
import java.util.*;
import jakarta.persistence.*;
import lombok.*;

// Enrollment.java
@Entity @Table(name="enrollment")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Enrollment {
    @Id @GeneratedValue private UUID id;
    @Column(nullable=false) private UUID userId;
    @ManyToOne(optional=false) @JoinColumn(name="plan_id")
    private Plan plan;
    @Enumerated(EnumType.STRING) @Column(nullable=false)
    private EnrollmentMode mode;
    @Enumerated(EnumType.STRING) @Column(nullable=false)
    private EnrollmentStatus status;
    @Enumerated(EnumType.STRING) @Column(nullable=false)
    private PaymentStatus paymentStatus;
    @Enumerated(EnumType.STRING) @Column(nullable=false)
    private EnrollmentType enrollmentType;
    @Column(name = "registration_id", unique = true, length = 50)
    private String registrationId;
    @Column(name = "team_name")
    private String teamName; // For team enrollments
    @Column(name = "team_size")
    private Integer teamSize; // Number of team members
    private UUID approvalBy;
    @Column(nullable=false)
    @Builder.Default
    private OffsetDateTime enrolledAt = OffsetDateTime.now();
}

