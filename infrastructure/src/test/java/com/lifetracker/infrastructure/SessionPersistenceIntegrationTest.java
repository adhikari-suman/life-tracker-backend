package com.lifetracker.infrastructure;

import com.lifetracker.application.user.RegisterUser;
import com.lifetracker.application.user.RegisterUserCommand;
import com.lifetracker.domain.session.RefreshTokenHash;
import com.lifetracker.domain.session.Session;
import com.lifetracker.domain.session.SessionId;
import com.lifetracker.domain.session.SessionRepository;
import com.lifetracker.domain.user.UserId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end persistence for Sessions against real Postgres. The context booting is the drift check
 * — it proves {@code SessionEntity} matches the 002-create-sessions migration. A Session requires a
 * User to exist (the FK), so each test registers one first.
 */
class SessionPersistenceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    RegisterUser registerUser;

    @Autowired
    SessionRepository sessions;

    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void persists_and_finds_a_session_for_its_user() {
        UserId userId = registerUser.execute(new RegisterUserCommand("sess-roundtrip@example.com", "correct horse battery"));
        SessionId sid = SessionId.generate();
        sessions.save(Session.open(sid, userId, new RefreshTokenHash("stored-hash"), "Chrome on Mac", T0));

        Session found = sessions.findById(sid).orElseThrow();
        assertEquals(userId, found.userId());
        assertEquals("Chrome on Mac", found.deviceLabel());
        assertEquals(new RefreshTokenHash("stored-hash"), found.refreshTokenHash());
        assertFalse(found.isRevoked());
        assertEquals(1, sessions.findByUserId(userId).size());
    }

    @Test
    void revocation_is_persisted() {
        UserId userId = registerUser.execute(new RegisterUserCommand("sess-revoke@example.com", "correct horse battery"));
        SessionId sid = SessionId.generate();
        Session s = Session.open(sid, userId, new RefreshTokenHash("h"), "device", T0);
        sessions.save(s);

        s.revoke();
        sessions.save(s);

        assertTrue(sessions.findById(sid).orElseThrow().isRevoked());
    }
}
