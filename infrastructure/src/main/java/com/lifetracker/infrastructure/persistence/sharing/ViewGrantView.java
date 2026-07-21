package com.lifetracker.infrastructure.persistence.sharing;

import java.time.OffsetDateTime;
import java.util.UUID;

/** A flat read view of a View Grant, for the owner's grant list. */
public record ViewGrantView(UUID id, String granteeEmail, UUID granteeUserId, OffsetDateTime createdAt) {
}
