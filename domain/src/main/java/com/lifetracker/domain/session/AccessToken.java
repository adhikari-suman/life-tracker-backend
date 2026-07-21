package com.lifetracker.domain.session;

import java.util.Objects;

/**
 * A signed access token (an RS256 JWT) and how long it stays valid. Opaque to the domain — the
 * signing is an infrastructure port. Redacted in {@code toString}; the token is a bearer credential.
 */
public record AccessToken(String value, long expiresInSeconds) {

    public AccessToken {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalStateException("access token value must not be blank");
        }
        if (expiresInSeconds <= 0) {
            throw new IllegalStateException("access token TTL must be positive");
        }
    }

    @Override
    public String toString() {
        return "AccessToken[REDACTED, expiresInSeconds=" + expiresInSeconds + "]";
    }
}
