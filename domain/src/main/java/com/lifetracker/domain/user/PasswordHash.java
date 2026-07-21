package com.lifetracker.domain.user;

import java.util.Objects;

/**
 * A hashed password, as produced by the {@link PasswordHasher}. Opaque to the domain: a stored
 * credential, never reversed to plaintext and never compared by hand (use
 * {@link PasswordHasher#matches}). A {@link User} holds one of these; {@link RawPassword} — the
 * plaintext — exists only for the instant it takes to hash or verify.
 */
public record PasswordHash(String value) {

    public PasswordHash {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            // Should never happen: the hasher is contracted to return an encoded hash. Guarded
            // so a broken adapter fails loudly here rather than storing an empty credential.
            throw new IllegalStateException("password hash must not be blank");
        }
    }
}
