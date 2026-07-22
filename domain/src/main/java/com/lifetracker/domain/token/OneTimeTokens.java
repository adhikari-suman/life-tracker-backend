package com.lifetracker.domain.token;

import java.util.Objects;

/**
 * Mints and hashes one-time-token secrets. A port: generating cryptographic randomness and hashing
 * (SHA-256 — the secret is high-entropy, so no salt) are infrastructure concerns. Mirrors the
 * refresh-token design: obtain a fresh secret + hash, and hash a presented secret for lookup.
 */
public interface OneTimeTokens {

    /** A freshly generated secret and the hash to store for it. */
    record Issued(OneTimeTokenValue value, OneTimeTokenHash hash) {
        public Issued {
            Objects.requireNonNull(value, "value");
            Objects.requireNonNull(hash, "hash");
        }
    }

    /** Generate a new token secret and its hash. */
    Issued issue();

    /** Hash a presented secret, to look up the stored {@link OneTimeTokenHash}. */
    OneTimeTokenHash hash(OneTimeTokenValue presented);
}
