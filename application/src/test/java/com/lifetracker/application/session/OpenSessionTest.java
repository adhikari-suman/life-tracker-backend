package com.lifetracker.application.session;

import com.lifetracker.domain.session.Session;
import com.lifetracker.domain.user.UserId;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenSessionTest {

    private final InMemorySessionRepository sessions = new InMemorySessionRepository();
    private final FakeRefreshTokens refreshTokens = new FakeRefreshTokens();
    private final Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
    private final OpenSession openSession = new OpenSession(sessions, refreshTokens, new FakeAccessTokens(), clock);

    @Test
    void opens_a_session_and_returns_both_tokens() {
        UserId user = UserId.generate();

        IssuedSession issued = openSession.execute(new OpenSessionCommand(user, "Chrome on Mac"));

        Session stored = sessions.findById(issued.sessionId()).orElseThrow();
        assertEquals(user, stored.userId());
        // The stored credential is the HASH of the returned secret, never the raw secret.
        assertEquals(refreshTokens.hash(issued.refreshToken()), stored.refreshTokenHash());
        assertTrue(issued.accessToken().value().contains(user.value().toString()));
    }
}
