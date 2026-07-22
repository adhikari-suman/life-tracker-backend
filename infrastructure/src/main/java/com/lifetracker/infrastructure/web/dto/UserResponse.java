package com.lifetracker.infrastructure.web.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Wire response for {@code GET /me}. Matches the OpenAPI {@code User} schema. */
public record UserResponse(UUID id, String email, boolean emailVerified, OffsetDateTime createdAt) {
}
