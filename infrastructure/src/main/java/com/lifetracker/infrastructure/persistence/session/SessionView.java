package com.lifetracker.infrastructure.persistence.session;

import java.time.OffsetDateTime;
import java.util.UUID;

/** A flat read view of a Session for the "active devices" screen. */
public record SessionView(UUID id, String deviceLabel, OffsetDateTime createdAt, OffsetDateTime lastUsedAt) {
}
