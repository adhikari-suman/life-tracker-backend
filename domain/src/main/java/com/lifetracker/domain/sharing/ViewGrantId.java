package com.lifetracker.domain.sharing;

import java.util.Objects;
import java.util.UUID;

/** The identity of a {@link ViewGrant}. */
public record ViewGrantId(UUID value) {

    public ViewGrantId {
        Objects.requireNonNull(value, "value");
    }

    public static ViewGrantId generate() {
        return new ViewGrantId(UUID.randomUUID());
    }

    public static ViewGrantId of(UUID value) {
        return new ViewGrantId(value);
    }
}
