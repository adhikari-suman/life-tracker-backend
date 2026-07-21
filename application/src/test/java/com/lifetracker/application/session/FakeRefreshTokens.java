package com.lifetracker.application.session;

import com.lifetracker.domain.session.RefreshTokenHash;
import com.lifetracker.domain.session.RefreshTokenValue;
import com.lifetracker.domain.session.RefreshTokens;

/**
 * Deterministic stand-in for the real generator/hasher: each {@code issue()} hands out a new secret
 * "secret-N", and hashing is the reversible "h:" prefix — enough to prove the use cases wire
 * generation, storage and verification correctly, with no crypto.
 */
final class FakeRefreshTokens implements RefreshTokens {

    private int counter = 0;

    @Override
    public Issued issue() {
        RefreshTokenValue secret = new RefreshTokenValue("secret-" + (++counter));
        return new Issued(secret, hash(secret));
    }

    @Override
    public RefreshTokenHash hash(RefreshTokenValue presented) {
        return new RefreshTokenHash("h:" + presented.value());
    }
}
