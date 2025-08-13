package com.mysillydreams.treasure.api.rest.dto.response;

import com.mysillydreams.treasure.domain.model.AudienceType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;
import java.util.UUID;

@Schema(
        name = "CategoryResponse",
        description = "Response containing category information"
)
public record CategoryResponse(
        @Schema(
                description = "Unique identifier for the category",
                example = "c2ce06de-7b41-4532-9aa1-22b31c9523e1"
        )
        UUID id,

        @Schema(
                description = "Category name",
                example = "Technology"
        )
        String name,

        @Schema(
                description = "Category description",
                example = "Technology and programming courses for all skill levels"
        )
        String description,

        @Schema(
                description = "Target audience for this category",
                example = "INDIVIDUAL",
                allowableValues = {"INDIVIDUAL", "CORPORATE", "EDUCATIONAL"}
        )
        AudienceType audience,

        @Schema(
                description = "Whether the category is active and visible to users",
                example = "true"
        )
        boolean active,

        @Schema(
                description = "Additional metadata tags",
                example = "{\"difficulty\": \"beginner\", \"featured\": \"true\"}"
        )
        Map<String, Object> tags
) {}
