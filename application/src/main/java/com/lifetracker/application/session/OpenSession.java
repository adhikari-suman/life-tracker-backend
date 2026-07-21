package com.lifetracker.application.session;

import com.lifetracker.domain.session.AccessToken;
import com.lifetracker.domain.session.AccessTokens;
import com.lifetracker.domain.session.RefreshTokens;
import com.lifetracker.domain.session.Session;
import com.lifetracker.domain.session.SessionId;
import com.lifetracker.domain.session.SessionRepository;
import com.lifetracker.domain.user.UserId;

import java.time.Clock;
import java.time.Instant;

/**
 * Opens a new Session for an already-authenticated User (login, or register's auto-login): mint a
 * refresh secret, store its hash on a fresh Session, and issue an access token. Returns both tokens.
 */
public final class OpenSession {

    private final SessionRepository sessions;
    private final RefreshTokens refreshTokens;
    private final AccessTokens accessTokens;
    private final Clock clock;

    public OpenSession(SessionRepository sessions, RefreshTokens refreshTokens,
                       AccessTokens accessTokens, Clock clock) {
        this.sessions = sessions;
        this.refreshTokens = refreshTokens;
        this.accessTokens = accessTokens;
        this.clock = clock;
    }

    public IssuedSession execute(OpenSessionCommand command) {
        Instant now = clock.instant();
        UserId userId = command.userId();
        SessionId sessionId = SessionId.generate();

        RefreshTokens.Issued issued = refreshTokens.issue();
        Session session = Session.open(sessionId, userId, issued.hash(), command.deviceLabel(), now);
        sessions.save(session);

        AccessToken accessToken = accessTokens.issueFor(userId, sessionId, now);
        return new IssuedSession(sessionId, issued.value(), accessToken);
    }
}
