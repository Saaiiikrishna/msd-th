package com.mysillydreams.treasure.api.rest.dto.response;

import java.util.UUID;

public record AgeBandResponse(UUID id, String label, int minAge, int maxAge) {}
