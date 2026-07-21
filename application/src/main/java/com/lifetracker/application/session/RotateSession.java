package com.lifetracker.application.session;

import com.lifetracker.domain.session.AccessToken;
import com.lifetracker.domain.session.AccessTokens;
import com.lifetracker.domain.session.RefreshTokenHash;
import com.lifetracker.domain.session.RefreshTokenReuseException;
import com.lifetracker.domain.session.RefreshTokens;
import com.lifetracker.domain.session.Session;
import com.lifetracker.domain.session.SessionNotActiveException;
import com.lifetracker.domain.session.SessionRepository;

import java.time.Clock;
import java.time.Instant;

/**
 * Rotates a Session's refresh token: verify the presented secret against the stored hash, mint a
 * new secret, and issue a fresh access token. Single-use — the old secret is retired. Every failure
 * (unknown session, expired, or a replayed retired token) raises the same
 * {@link InvalidRefreshTokenException}; on a replay the Session is revoked and that is persisted.
 */
public final class RotateSession {

    private final SessionRepository sessions;
    private final RefreshTokens refreshTokens;
    private final AccessTokens accessTokens;
    private final Clock clock;

    public RotateSession(SessionRepository sessions, RefreshTokens refreshTokens,
                         AccessTokens accessTokens, Clock clock) {
        this.sessions = sessions;
        this.refreshTokens = refreshTokens;
        this.accessTokens = accessTokens;
        this.clock = clock;
    }

    public IssuedSession execute(RotateSessionCommand command) {
        Instant now = clock.instant();
        Session session = sessions.findById(command.sessionId())
                .orElseThrow(InvalidRefreshTokenException::new);

        RefreshTokenHash presentedHash = refreshTokens.hash(command.presentedRefreshToken());
        RefreshTokens.Issued issued = refreshTokens.issue();
        try {
            session.rotate(presentedHash, issued.hash(), now);
        } catch (RefreshTokenReuseException | SessionNotActiveException failed) {
            sessions.save(session); // persist the revoke on reuse; a no-op if merely inactive
            throw new InvalidRefreshTokenException();
        }
        sessions.save(session);

        AccessToken accessToken = accessTokens.issueFor(session.userId(), session.id(), now);
        return new IssuedSession(session.id(), issued.value(), accessToken);
    }
}
