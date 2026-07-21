package com.lifetracker.infrastructure.persistence.sharing;

import java.time.OffsetDateTime;

/** A flat read view of an owner's Share Link. The wire URL is assembled at the boundary. */
public record ShareLinkView(String token, OffsetDateTime createdAt) {
}
