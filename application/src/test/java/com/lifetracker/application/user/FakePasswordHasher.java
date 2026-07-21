package com.lifetracker.application.user;

import com.lifetracker.domain.user.PasswordHash;
import com.lifetracker.domain.user.PasswordHasher;
import com.lifetracker.domain.user.RawPassword;

/**
 * A deterministic, reversible stand-in for the real Argon2 hasher — enough to prove the use cases
 * wire hashing and verification correctly, with no crypto. The real adapter arrives in
 * infrastructure.
 */
final class FakePasswordHasher implements PasswordHasher {

    @Override
    public PasswordHash hash(RawPassword raw) {
        return new PasswordHash("hashed:" + raw.value());
    }

    @Override
    public boolean matches(RawPassword raw, PasswordHash hash) {
        return hash.value().equals("hashed:" + raw.value());
    }
}
