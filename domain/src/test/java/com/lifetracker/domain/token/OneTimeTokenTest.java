package com.lifetracker.domain.token;

import com.lifetracker.domain.user.UserId;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OneTimeTokenTest {

    private static final Instant T0 = Instant.parse("2026-07-21T12:00:00Z");

    private OneTimeToken token(TokenPurpose purpose, Instant expiresAt) {
        return OneTimeToken.issue(OneTimeTokenId.generate(), UserId.generate(),
                new OneTimeTokenHash("h"), purpose, expiresAt, T0);
    }

    @Test
    void is_not_expired_before_its_expiry() {
        OneTimeToken t = token(TokenPurpose.VERIFY_EMAIL, T0.plus(Duration.ofHours(1)));
        assertFalse(t.isExpired(T0.plus(Duration.ofMinutes(59))));
    }

    @Test
    void is_expired_at_and_after_its_expiry() {
        Instant expiry = T0.plus(Duration.ofHours(1));
        OneTimeToken t = token(TokenPurpose.VERIFY_EMAIL, expiry);
        assertTrue(t.isExpired(expiry));
        assertTrue(t.isExpired(expiry.plusSeconds(1)));
    }

    @Test
    void knows_its_purpose() {
        OneTimeToken t = token(TokenPurpose.RESET_PASSWORD, T0.plus(Duration.ofHours(1)));
        assertTrue(t.isFor(TokenPurpose.RESET_PASSWORD));
        assertFalse(t.isFor(TokenPurpose.VERIFY_EMAIL));
    }
}
