package com.mysillydreams.treasure.api.rest.dto.request;

import java.util.Map;

public record PolicyUpsertRequest(String scope, String scopeRef, Map<String,Object> policyJson) {}