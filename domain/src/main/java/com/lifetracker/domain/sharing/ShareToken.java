package com.lifetracker.domain.sharing;

import java.util.Objects;

/**
 * The secret in an anonymous Share Link URL — a long, unguessable string. Stored as-is so the owner
 * can re-copy the live link (Drive's model, ADR-0008); its security rests on being unguessable and
 * revocable, not on being hashed. Redacted in {@code toString} so it does not leak into logs.
 */
public record ShareToken(String value) {

    public ShareToken {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalStateException("share token must not be blank");
        }
    }

    @Override
    public String toString() {
        return "ShareToken[REDACTED]";
    }
}
