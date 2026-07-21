package com.lifetracker.domain.session;

import java.util.Objects;

/**
 * Mints and hashes refresh-token secrets. A port: generating cryptographic randomness and hashing
 * (SHA-256 — the secret is high-entropy, so no salt) are infrastructure concerns. The domain uses
 * this to obtain a fresh secret + hash, and to hash a presented secret for comparison.
 */
public interface RefreshTokens {

    /** A freshly generated secret and the hash to store for it. */
    record Issued(RefreshTokenValue value, RefreshTokenHash hash) {
        public Issued {
            Objects.requireNonNull(value, "value");
            Objects.requireNonNull(hash, "hash");
        }
    }

    /** Generate a new refresh secret and its hash. */
    Issued issue();

    /** Hash a presented secret, to compare against a stored {@link RefreshTokenHash}. */
    RefreshTokenHash hash(RefreshTokenValue presented);
}
