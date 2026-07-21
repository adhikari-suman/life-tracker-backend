package com.lifetracker.domain.user;

import java.util.Objects;

/**
 * A plaintext password, as typed at registration or login. Ephemeral: it is checked against
 * policy, handed straight to the {@link PasswordHasher}, and never stored — a {@link User} holds
 * a {@link PasswordHash}, never this. Keeping plaintext in its own short-lived type is what makes
 * "never store a plaintext password" a matter of types rather than vigilance.
 */
public record RawPassword(String value) {

    /** Provisional policy — see ADR-0007. Long enough to matter; bounded to cap hashing cost. */
    private static final int MIN_LENGTH = 12;
    private static final int MAX_LENGTH = 128;

    public RawPassword {
        Objects.requireNonNull(value, "value");
        if (value.length() < MIN_LENGTH || value.length() > MAX_LENGTH) {
            throw new WeakPasswordException(MIN_LENGTH, MAX_LENGTH);
        }
    }

    /** Never render the plaintext — a stray log line must not leak it. */
    @Override
    public String toString() {
        return "RawPassword[REDACTED]";
    }
}
