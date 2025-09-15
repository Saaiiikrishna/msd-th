package com.mysillydreams.treasure.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Tracks registration sequence numbers for generating unique registration IDs
 * Format: TH-MMYY-IND/TEAM-XXXXXX where XXXXXX is the sequence
 */
@Entity 
@Table(name = "registration_sequence", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"month_year", "enrollment_type", "plan_id"}))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class RegistrationSequence {
    
    @Id @GeneratedValue 
    private UUID id;
    
    @Column(name = "month_year", nullable = false, length = 4)
    private String monthYear; // Format: MMYY (e.g., "0825" for Aug 2025)
    
    @Enumerated(EnumType.STRING)
    @Column(name = "enrollment_type", nullable = false)
    private EnrollmentType enrollmentType;
    
    @Column(name = "plan_id", nullable = false)
    private UUID planId;
    
    @Column(name = "current_sequence", nullable = false)
    @Builder.Default
    private Long currentSequence = 0L;

    @Column(nullable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(nullable = false)
    @Builder.Default
    private OffsetDateTime updatedAt = OffsetDateTime.now();
    
    @PreUpdate 
    void touch() { 
        this.updatedAt = OffsetDateTime.now(); 
    }
    
    /**
     * Increments and returns the next sequence number
     */
    public Long getNextSequence() {
        this.currentSequence++;
        return this.currentSequence;
    }
}
