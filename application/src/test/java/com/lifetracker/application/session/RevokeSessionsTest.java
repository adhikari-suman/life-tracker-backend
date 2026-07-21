package com.lifetracker.application.session;

import com.lifetracker.domain.user.UserId;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RevokeSessionsTest {

    private final InMemorySessionRepository sessions = new InMemorySessionRepository();
    private final Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
    private final OpenSession openSession =
            new OpenSession(sessions, new FakeRefreshTokens(), new FakeAccessTokens(), clock);
    private final RevokeSession revokeSession = new RevokeSession(sessions);
    private final RevokeAllSessions revokeAll = new RevokeAllSessions(sessions);

    @Test
    void owner_revokes_their_own_session() {
        UserId user = UserId.generate();
        IssuedSession s = openSession.execute(new OpenSessionCommand(user, "d"));

        revokeSession.execute(new RevokeSessionCommand(s.sessionId(), user));

        assertTrue(sessions.findById(s.sessionId()).orElseThrow().isRevoked());
    }

    @Test
    void revoking_someone_elses_session_is_not_found() {
        IssuedSession s = openSession.execute(new OpenSessionCommand(UserId.generate(), "d"));

        assertThrows(SessionNotFoundException.class,
                () -> revokeSession.execute(new RevokeSessionCommand(s.sessionId(), UserId.generate())));
        assertFalse(sessions.findById(s.sessionId()).orElseThrow().isRevoked());
    }

    @Test
    void sign_out_everywhere_revokes_all_of_a_users_sessions_and_no_others() {
        UserId user = UserId.generate();
        IssuedSession a = openSession.execute(new OpenSessionCommand(user, "web"));
        IssuedSession b = openSession.execute(new OpenSessionCommand(user, "mobile"));
        IssuedSession other = openSession.execute(new OpenSessionCommand(UserId.generate(), "someone else"));

        revokeAll.execute(new RevokeAllSessionsCommand(user));

        assertTrue(sessions.findById(a.sessionId()).orElseThrow().isRevoked());
        assertTrue(sessions.findById(b.sessionId()).orElseThrow().isRevoked());
        assertFalse(sessions.findById(other.sessionId()).orElseThrow().isRevoked());
    }
}
