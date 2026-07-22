package com.lifetracker.application.user;

import com.lifetracker.domain.token.OneTimeToken;
import com.lifetracker.domain.token.OneTimeTokenId;
import com.lifetracker.domain.token.OneTimeTokens;
import com.lifetracker.domain.token.TokenPurpose;
import com.lifetracker.domain.user.Email;
import com.lifetracker.domain.user.PasswordHash;
import com.lifetracker.domain.user.User;
import com.lifetracker.domain.user.UserId;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VerifyEmailTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private final InMemoryUserRepository users = new InMemoryUserRepository();
    private final FakeOneTimeTokens tokens = new FakeOneTimeTokens();
    private final InMemoryOneTimeTokenRepository tokenStore = new InMemoryOneTimeTokenRepository();
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final VerifyEmail verifyEmail = new VerifyEmail(tokens, tokenStore, users, clock);

    private final User user = User.register(UserId.generate(), new Email("sam@example.com"), new PasswordHash("hash"));

    private String issue(TokenPurpose purpose, Instant expiresAt) {
        users.save(user);
        OneTimeTokens.Issued issued = tokens.issue();
        tokenStore.save(OneTimeToken.issue(OneTimeTokenId.generate(), user.id(), issued.hash(), purpose, expiresAt, NOW));
        return issued.value().value();
    }

    @Test
    void verifies_the_user_and_consumes_the_token() {
        String token = issue(TokenPurpose.VERIFY_EMAIL, NOW.plus(Duration.ofHours(24)));

        verifyEmail.execute(token);

        assertTrue(users.findById(user.id()).orElseThrow().isEmailVerified());
        assertEquals(0, tokenStore.size());
    }

    @Test
    void rejects_an_unknown_token() {
        assertThrows(InvalidTokenException.class, () -> verifyEmail.execute("nope"));
    }

    @Test
    void rejects_a_blank_token() {
        assertThrows(InvalidTokenException.class, () -> verifyEmail.execute("   "));
    }

    @Test
    void rejects_a_reset_token_used_for_verification() {
        String token = issue(TokenPurpose.RESET_PASSWORD, NOW.plus(Duration.ofHours(1)));
        assertThrows(InvalidTokenException.class, () -> verifyEmail.execute(token));
        assertFalse(users.findById(user.id()).orElseThrow().isEmailVerified());
    }

    @Test
    void rejects_an_expired_token() {
        String token = issue(TokenPurpose.VERIFY_EMAIL, NOW.minus(Duration.ofSeconds(1)));
        assertThrows(InvalidTokenException.class, () -> verifyEmail.execute(token));
    }
}
