package com.lifetracker.domain.token;

import java.util.Objects;
import java.util.UUID;

/** The identity of a {@link OneTimeToken}. A value object wrapping a {@link UUID}. */
public record OneTimeTokenId(UUID value) {

    public OneTimeTokenId {
        Objects.requireNonNull(value, "value");
    }

    public static OneTimeTokenId generate() {
        return new OneTimeTokenId(UUID.randomUUID());
    }

    public static OneTimeTokenId of(UUID value) {
        return new OneTimeTokenId(value);
    }
}
