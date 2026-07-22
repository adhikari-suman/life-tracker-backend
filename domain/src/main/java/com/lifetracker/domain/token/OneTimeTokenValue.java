package com.lifetracker.domain.token;

import java.util.Objects;

/**
 * The raw secret of a one-time token — high-entropy random bytes, encoded as a string. Held only
 * for the instant it takes to hash or hand to the {@link com.lifetracker.domain.notification.EmailSender};
 * never stored (a {@link OneTimeToken} keeps a {@link OneTimeTokenHash}). Redacted in {@code toString}.
 */
public record OneTimeTokenValue(String value) {

    public OneTimeTokenValue {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalStateException("one-time token value must not be blank");
        }
    }

    @Override
    public String toString() {
        return "OneTimeTokenValue[REDACTED]";
    }
}
