package com.lifetracker.domain.user;

import java.util.Objects;
import java.util.UUID;

/**
 * The identity of a {@link User}. A value object wrapping a {@link UUID}; two ids are equal
 * exactly when their UUIDs are. A User compares on this, never on its email or hash.
 */
public record UserId(UUID value) {

    public UserId {
        Objects.requireNonNull(value, "value");
    }

    /** A fresh, random id for a User about to be registered. */
    public static UserId generate() {
        return new UserId(UUID.randomUUID());
    }

    public static UserId of(UUID value) {
        return new UserId(value);
    }

    public static UserId fromString(String value) {
        return new UserId(UUID.fromString(value));
    }
}
