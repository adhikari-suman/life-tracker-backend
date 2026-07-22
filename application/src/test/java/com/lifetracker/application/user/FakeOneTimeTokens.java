package com.lifetracker.application.user;

import com.lifetracker.domain.token.OneTimeTokenHash;
import com.lifetracker.domain.token.OneTimeTokenValue;
import com.lifetracker.domain.token.OneTimeTokens;

/**
 * Deterministic {@link OneTimeTokens} for tests: each issue() returns a fresh incrementing secret,
 * and the hash is a stable transform, so re-hashing a presented value matches the stored one.
 */
final class FakeOneTimeTokens implements OneTimeTokens {

    private int counter;

    @Override
    public Issued issue() {
        OneTimeTokenValue value = new OneTimeTokenValue("secret-" + counter++);
        return new Issued(value, hash(value));
    }

    @Override
    public OneTimeTokenHash hash(OneTimeTokenValue presented) {
        return new OneTimeTokenHash("hash:" + presented.value());
    }
}
