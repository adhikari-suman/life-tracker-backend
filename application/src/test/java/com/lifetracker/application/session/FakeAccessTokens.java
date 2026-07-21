package com.lifetracker.application.session;

import com.lifetracker.domain.session.AccessToken;
import com.lifetracker.domain.session.AccessTokens;
import com.lifetracker.domain.session.SessionId;
import com.lifetracker.domain.user.UserId;

import java.time.Instant;

/** Deterministic stand-in for the real RS256 signer. */
final class FakeAccessTokens implements AccessTokens {

    @Override
    public AccessToken issueFor(UserId userId, SessionId sessionId, Instant now) {
        return new AccessToken("access." + userId.value() + "." + sessionId.value(), 900);
    }
}
