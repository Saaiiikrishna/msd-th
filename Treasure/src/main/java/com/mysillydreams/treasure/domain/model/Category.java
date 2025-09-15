package com.mysillydreams.treasure.domain.model;

import java.util.*;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.*;
import com.mysillydreams.treasure.config.AudienceTypeConverter;

// Category.java
@Entity
@Table(name="category")
@Getter
@Setter
@Builder @NoArgsConstructor
@AllArgsConstructor
public class Category {
    @Id @GeneratedValue private UUID id;
    @Column(nullable=false) private String name;
    private String description;
    @org.hibernate.annotations.Type(AudienceTypeConverter.class)
    @Column(nullable=false, columnDefinition = "audience_type")
    private AudienceType audience;
    @Column(nullable=false) private boolean active = true;
    @JdbcTypeCode(SqlTypes.JSON) private Map<String,Object> tags;
    @OneToMany(mappedBy="category", cascade=CascadeType.ALL, orphanRemoval=true)
    private List<Subcategory> subcategories = new ArrayList<>();
}

