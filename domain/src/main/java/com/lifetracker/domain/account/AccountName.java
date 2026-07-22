package com.lifetracker.domain.account;

import java.util.Objects;

/** An account's display name — trimmed, non-blank, and bounded. */
public record AccountName(String value) {

    private static final int MAX_LENGTH = 100;

    public AccountName {
        Objects.requireNonNull(value, "value");
        value = value.trim();
        if (value.isEmpty()) {
            throw new InvalidAccountNameException("account name must not be blank");
        }
        if (value.length() > MAX_LENGTH) {
            throw new InvalidAccountNameException("account name must be at most " + MAX_LENGTH + " characters");
        }
    }
}
