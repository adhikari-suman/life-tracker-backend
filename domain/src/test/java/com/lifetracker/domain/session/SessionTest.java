package com.lifetracker.domain.session;

import com.lifetracker.domain.user.UserId;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SessionTest {

    private static final UserId USER = UserId.generate();
    private static final RefreshTokenHash H1 = new RefreshTokenHash("hash-1");
    private static final RefreshTokenHash H2 = new RefreshTokenHash("hash-2");
    private static final RefreshTokenHash H3 = new RefreshTokenHash("hash-3");
    private static final RefreshTokenHash H4 = new RefreshTokenHash("hash-4");
    private static final RefreshTokenHash H5 = new RefreshTokenHash("hash-5");
    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");

    private static Session open() {
        return Session.open(SessionId.generate(), USER, H1, "Chrome on Mac", T0);
    }

    @Test
    void opens_active_and_unrevoked() {
        Session s = open();
        assertTrue(s.isActive(T0));
        assertFalse(s.isRevoked());
    }

    @Test
    void rotating_with_the_current_hash_swaps_it_and_advances() {
        Session s = open();
        Instant later = T0.plus(Duration.ofDays(1));

        s.rotate(H1, H2, later);

        assertEquals(H2, s.refreshTokenHash());
        assertEquals(later, s.lastUsedAt());
        assertTrue(s.isActive(later));
    }

    @Test
    void replaying_a_retired_hash_revokes_the_session() {
        Session s = open();
        s.rotate(H1, H2, T0.plus(Duration.ofDays(1))); // current hash is now H2

        // Someone replays the old H1:
        assertThrows(RefreshTokenReuseException.class,
                () -> s.rotate(H1, new RefreshTokenHash("hash-3"), T0.plus(Duration.ofDays(2))));

        assertTrue(s.isRevoked());
        assertFalse(s.isActive(T0.plus(Duration.ofDays(2))));
    }

    @Test
    void a_revoked_session_cannot_rotate() {
        Session s = open();
        s.revoke();
        assertThrows(SessionNotActiveException.class,
                () -> s.rotate(H1, H2, T0.plus(Duration.ofDays(1))));
    }

    @Test
    void an_expired_session_cannot_rotate() {
        Session s = open();
        Instant afterWindow = T0.plus(Duration.ofDays(31)); // past the 30-day sliding window

        assertFalse(s.isActive(afterWindow));
        assertThrows(SessionNotActiveException.class, () -> s.rotate(H1, H2, afterWindow));
    }

    @Test
    void sliding_expiry_never_exceeds_the_absolute_cap() {
        Session s = open(); // opened at T0, expires T0 + 30 days
        // Keep it alive with rotations inside each sliding window, marching toward the 90-day cap.
        s.rotate(H1, H2, T0.plus(Duration.ofDays(20))); // -> expires day 50
        s.rotate(H2, H3, T0.plus(Duration.ofDays(40))); // -> expires day 70
        s.rotate(H3, H4, T0.plus(Duration.ofDays(60))); // -> expires day 90 (== cap)
        s.rotate(H4, H5, T0.plus(Duration.ofDays(85))); // sliding would be day 115, capped at day 90

        Instant cap = T0.plus(Duration.ofDays(90));
        assertEquals(cap, s.expiresAt());
        assertFalse(s.isActive(cap.plus(Duration.ofDays(1)))); // day 91: inactive, despite being active at 85
    }

    @Test
    void revoke_is_idempotent() {
        Session s = open();
        s.revoke();
        s.revoke();
        assertTrue(s.isRevoked());
    }

    @Test
    void identity_is_the_id_not_the_fields() {
        SessionId id = SessionId.generate();
        Session a = Session.open(id, USER, H1, "A", T0);
        Session b = Session.open(id, UserId.generate(), H2, "B", T0.plus(Duration.ofDays(1)));

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }
}
