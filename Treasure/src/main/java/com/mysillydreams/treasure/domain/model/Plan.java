package com.mysillydreams.treasure.domain.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import static jakarta.persistence.CascadeType.ALL;

// Plan.java
@Entity @Table(name="plan")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Plan {
    @Id @GeneratedValue private UUID id;

    @ManyToOne(optional=false) @JoinColumn(name="subcategory_id")
    private Subcategory subcategory;

    @Column(nullable=false) private String title;
    @Column(columnDefinition="text") private String summary;

    private String venueText;
    private String city;
    private String country;
    @Column(nullable=false) private boolean isVirtual;

    @Enumerated(EnumType.STRING) @Column(name="time_window", nullable=false)
    private TimeWindowType timeWindowType;

    private OffsetDateTime startAt;
    private OffsetDateTime endAt;

    @Column(name="max_participants") private Integer maxParticipants; // null => open

    // PostGIS point is mapped via column + converter or custom type; keep raw here:
    @Column(name="geo_point", columnDefinition="geography(Point,4326)")
    private byte[] geoPoint; // or use a helper type like org.locationtech.jts.geom.Point

    @OneToMany(mappedBy="plan", cascade=ALL, orphanRemoval=true)
    @BatchSize(size = 50)
    private List<PlanDifficulty> difficulties = new ArrayList<>();

    @OneToMany(mappedBy="plan", cascade=ALL, orphanRemoval=true)
    @BatchSize(size = 50)
    private List<PlanRule> rules = new ArrayList<>();

    @OneToMany(mappedBy="plan", cascade=ALL, orphanRemoval=true)
    @BatchSize(size = 50)
    private List<Task> tasks = new ArrayList<>();

    @OneToMany(mappedBy="plan", cascade=ALL, orphanRemoval=true)
    @BatchSize(size = 50)
    private List<PlanPrice> prices = new ArrayList<>();

    @OneToMany(mappedBy="plan", cascade=ALL, orphanRemoval=true)
    @BatchSize(size = 50)
    private List<PlanSlot> slots = new ArrayList<>();

    // Vendor assignment for payouts
    @Column(name = "vendor_id")
    private UUID vendorId; // null means no vendor assigned (direct platform plan)

    @Column(name = "vendor_commission_rate", precision = 5, scale = 2)
    private BigDecimal vendorCommissionRate; // Vendor-specific commission rate (overrides default)

    @Column(nullable=false) private OffsetDateTime createdAt = OffsetDateTime.now();
    @Column(nullable=false) private OffsetDateTime updatedAt = OffsetDateTime.now();

    @PreUpdate void touch() { this.updatedAt = OffsetDateTime.now(); }
}

