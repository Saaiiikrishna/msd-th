package com.mysillydreams.treasure.api.rest.dto.response;

import java.util.Map;
import java.util.UUID;

public record PolicyResponse(UUID id, String scope, String scopeRef, Map<String,Object> policyJson, boolean active) {}

