package com.mysillydreams.treasure.api.rest.dto.response;

import java.util.List;
import java.util.UUID;

public record SubcategoryResponse(UUID id, UUID categoryId, String name, String description, boolean active, List<AgeBandResponse> ageBands) {}

