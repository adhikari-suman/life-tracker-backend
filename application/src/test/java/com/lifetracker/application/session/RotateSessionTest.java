package com.lifetracker.application.session;

import com.lifetracker.domain.session.RefreshTokenValue;
import com.lifetracker.domain.session.Session;
import com.lifetracker.domain.session.SessionId;
import com.lifetracker.domain.user.UserId;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RotateSessionTest {

    private final InMemorySessionRepository sessions = new InMemorySessionRepository();
    private final FakeRefreshTokens refreshTokens = new FakeRefreshTokens();
    private final FakeAccessTokens accessTokens = new FakeAccessTokens();
    private final Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
    private final OpenSession openSession = new OpenSession(sessions, refreshTokens, accessTokens, clock);
    private final RotateSession rotateSession = new RotateSession(sessions, refreshTokens, accessTokens, clock);

    @Test
    void rotates_with_the_current_refresh_token() {
        IssuedSession opened = openSession.execute(new OpenSessionCommand(UserId.generate(), "d"));

        IssuedSession rotated =
                rotateSession.execute(new RotateSessionCommand(opened.sessionId(), opened.refreshToken()));

        assertEquals(opened.sessionId(), rotated.sessionId());
        assertNotEquals(opened.refreshToken(), rotated.refreshToken()); // single-use: a new secret
    }

    @Test
    void replaying_the_retired_token_after_rotation_fails_and_revokes() {
        IssuedSession opened = openSession.execute(new OpenSessionCommand(UserId.generate(), "d"));
        rotateSession.execute(new RotateSessionCommand(opened.sessionId(), opened.refreshToken())); // rotate once

        // Replay the original (now retired) token:
        assertThrows(InvalidRefreshTokenException.class,
                () -> rotateSession.execute(new RotateSessionCommand(opened.sessionId(), opened.refreshToken())));

        Session s = sessions.findById(opened.sessionId()).orElseThrow();
        assertTrue(s.isRevoked());
    }

    @Test
    void an_unknown_session_is_indistinguishably_invalid() {
        assertThrows(InvalidRefreshTokenException.class,
                () -> rotateSession.execute(
                        new RotateSessionCommand(SessionId.generate(), new RefreshTokenValue("whatever"))));
    }
}
