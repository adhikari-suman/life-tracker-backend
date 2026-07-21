package com.lifetracker.domain.session;

import com.lifetracker.domain.user.UserId;

import java.time.Instant;

/**
 * Issues access tokens. A port: signing (RS256) is an infrastructure concern. The token names the
 * User (its subject) and the Session it was minted for, so the perimeter can authorize by User and
 * revoke by Session.
 */
public interface AccessTokens {

    AccessToken issueFor(UserId userId, SessionId sessionId, Instant now);
}
