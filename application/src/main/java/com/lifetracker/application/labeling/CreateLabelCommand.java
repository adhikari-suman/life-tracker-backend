package com.lifetracker.application.labeling;

import com.lifetracker.domain.ledger.OwnerId;

import java.util.UUID;

/** Input to {@link CreateLabel}: the owner, the name, and the parent to nest under (null for a root). */
public record CreateLabelCommand(OwnerId owner, String name, UUID parentId) {
}
