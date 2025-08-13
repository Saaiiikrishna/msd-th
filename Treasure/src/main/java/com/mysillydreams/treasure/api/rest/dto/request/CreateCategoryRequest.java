package com.mysillydreams.treasure.api.rest.dto.request;

import com.mysillydreams.treasure.domain.model.AudienceType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

@Schema(
        name = "CreateCategoryRequest",
        description = "Request payload for creating a new category"
)
public record CreateCategoryRequest(
        @Schema(
                description = "Unique name for the category",
                example = "Technology",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "Category name is required")
        String name,

        @Schema(
                description = "Detailed description of the category",
                example = "Technology and programming courses for all skill levels"
        )
        String description,

        @Schema(
                description = "Target audience for this category",
                example = "INDIVIDUAL",
                allowableValues = {"INDIVIDUAL", "CORPORATE", "EDUCATIONAL"},
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "Audience type is required")
        AudienceType audience,

        @Schema(
                description = "Additional metadata tags as key-value pairs",
                example = "{\"difficulty\": \"beginner\", \"featured\": \"true\"}"
        )
        Map<String, Object> tags
) {}
