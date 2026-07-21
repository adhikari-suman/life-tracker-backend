package com.lifetracker.domain.session;

import java.util.Objects;

/**
 * The raw secret of a refresh token — high-entropy random bytes, encoded as a string. Held only
 * for the instant it takes to hash or hand to the client; never stored (a {@link Session} keeps a
 * {@link RefreshTokenHash}, not this). Redacted in {@code toString} so it cannot leak.
 */
public record RefreshTokenValue(String value) {

    public RefreshTokenValue {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalStateException("refresh token value must not be blank");
        }
    }

    @Override
    public String toString() {
        return "RefreshTokenValue[REDACTED]";
    }
}
