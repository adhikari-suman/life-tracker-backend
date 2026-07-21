package com.lifetracker.domain.session;

import java.util.Objects;
import java.util.UUID;

/**
 * The identity of a {@link Session}. A value object wrapping a {@link UUID}; a Session compares on
 * this, never on its fields.
 */
public record SessionId(UUID value) {

    public SessionId {
        Objects.requireNonNull(value, "value");
    }

    /** A fresh, random id for a Session about to be opened. */
    public static SessionId generate() {
        return new SessionId(UUID.randomUUID());
    }

    public static SessionId of(UUID value) {
        return new SessionId(value);
    }

    public static SessionId fromString(String value) {
        return new SessionId(UUID.fromString(value));
    }
}
