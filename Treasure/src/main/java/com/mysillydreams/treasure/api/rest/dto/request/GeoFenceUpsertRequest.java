package com.mysillydreams.treasure.api.rest.dto.request;

import java.util.List;

public record GeoFenceUpsertRequest(boolean enabled, String scope, List<String> values) {}