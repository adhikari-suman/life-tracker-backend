package com.lifetracker.domain.transaction;

import java.util.Objects;
import java.util.UUID;

/** The identity of a {@link Transaction}. A value object wrapping a {@link UUID}. */
public record TransactionId(UUID value) {

    public TransactionId {
        Objects.requireNonNull(value, "value");
    }

    public static TransactionId generate() {
        return new TransactionId(UUID.randomUUID());
    }

    public static TransactionId of(UUID value) {
        return new TransactionId(value);
    }
}
