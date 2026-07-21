package com.lifetracker.infrastructure.web.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Wire response for a View Grant. Matches the OpenAPI {@code ViewGrant} schema. */
public record ViewGrantResponse(UUID id, String granteeEmail, UUID granteeUserId, OffsetDateTime createdAt) {
}
