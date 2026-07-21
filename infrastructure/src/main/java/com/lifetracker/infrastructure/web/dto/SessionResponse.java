package com.lifetracker.infrastructure.web.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Wire response for {@code GET /auth/sessions}. Matches the OpenAPI {@code Session} schema;
 * {@code current} marks the Session that made the request.
 */
public record SessionResponse(UUID id, String deviceLabel, OffsetDateTime createdAt,
                              OffsetDateTime lastActiveAt, boolean current) {
}
