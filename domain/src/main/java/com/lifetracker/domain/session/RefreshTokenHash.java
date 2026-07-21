package com.lifetracker.domain.session;

import java.util.Objects;

/**
 * The hash of a refresh token's secret, as stored on a {@link Session}. The raw token is never
 * stored — only this. Because a refresh secret is high-entropy (unlike a password), a fast,
 * deterministic hash is enough and there is no salt: a presented token is verified by re-hashing
 * its secret and comparing this value for equality. Opaque to the domain; the hashing itself is
 * an infrastructure port.
 */
public record RefreshTokenHash(String value) {

    public RefreshTokenHash {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalStateException("refresh token hash must not be blank");
        }
    }
}
