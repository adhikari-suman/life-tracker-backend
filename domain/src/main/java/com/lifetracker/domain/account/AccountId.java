package com.lifetracker.domain.account;

import java.util.Objects;
import java.util.UUID;

/** The identity of an {@link Account}. A value object wrapping a {@link UUID}. */
public record AccountId(UUID value) {

    public AccountId {
        Objects.requireNonNull(value, "value");
    }

    public static AccountId generate() {
        return new AccountId(UUID.randomUUID());
    }

    public static AccountId of(UUID value) {
        return new AccountId(value);
    }
}
