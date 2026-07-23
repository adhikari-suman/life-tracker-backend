package com.lifetracker.domain.transaction;

import java.util.Objects;
import java.util.UUID;

/**
 * The identity of a {@link Posting}. A posting is still a value in the {@link Transaction} aggregate —
 * you never load one on its own — but it needs a stable id so metadata can attach to it from outside
 * the ledger core (ADR-0014): a label is keyed by this, not held on the posting.
 *
 * <p>This is identity, not metadata. The core still knows nothing of labels; it merely names its legs
 * so something else can point at one.
 */
public record PostingId(UUID value) {

    public PostingId {
        Objects.requireNonNull(value, "value");
    }

    public static PostingId generate() {
        return new PostingId(UUID.randomUUID());
    }

    public static PostingId of(UUID value) {
        return new PostingId(value);
    }
}
