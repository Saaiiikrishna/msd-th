package com.mysillydreams.treasure.api.rest.dto.request;

import java.util.List;
import java.util.UUID;

public record CreateSubcategoryRequest(UUID categoryId, String name, String description, List<UUID> ageBandIds) {}
