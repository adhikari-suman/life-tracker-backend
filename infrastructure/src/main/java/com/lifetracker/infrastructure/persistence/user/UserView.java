package com.lifetracker.infrastructure.persistence.user;

import java.time.OffsetDateTime;
import java.util.UUID;

/** A flat read view of a User, returned by {@link UserQueryService} to the read side. */
public record UserView(UUID id, String email, boolean emailVerified, OffsetDateTime createdAt) {
}
