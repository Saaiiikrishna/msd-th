package com.mysillydreams.treasure.api.rest.dto.request;

import java.util.UUID;

public record ApprovalDecisionRequest(UUID approvedBy, String notes) {}