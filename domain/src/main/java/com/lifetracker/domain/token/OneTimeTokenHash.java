package com.lifetracker.domain.token;

import java.util.Objects;

/**
 * The hash of a one-time token's secret, as stored. The raw token is never stored — only this.
 * The secret is high-entropy, so a fast, unsalted SHA-256 is enough: a presented token is verified
 * by re-hashing and looking this value up. Opaque to the domain; the hashing is an infrastructure port.
 */
public record OneTimeTokenHash(String value) {

    public OneTimeTokenHash {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalStateException("one-time token hash must not be blank");
        }
    }
}
