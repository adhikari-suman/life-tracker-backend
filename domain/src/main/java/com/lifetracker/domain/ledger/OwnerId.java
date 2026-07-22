package com.lifetracker.domain.ledger;

import java.util.Objects;
import java.util.UUID;

/**
 * The tenant key for a Book — who owns a Ledger aggregate. Stamped at the application boundary from
 * the authenticated User (a User owns one Book, so it carries the User's id), but the Ledger
 * deliberately does NOT reference the User: this is its own type so the Ledger context stays free of
 * Identity (CONTEXT-MAP, ADR-0006). Ledger aggregates hold no owner field; the owner is passed to
 * every use case and query instead.
 */
public record OwnerId(UUID value) {

    public OwnerId {
        Objects.requireNonNull(value, "value");
    }

    public static OwnerId of(UUID value) {
        return new OwnerId(value);
    }
}
