package com.lifetracker.application.user;

import com.lifetracker.domain.token.OneTimeToken;
import com.lifetracker.domain.token.OneTimeTokenId;
import com.lifetracker.domain.token.OneTimeTokens;
import com.lifetracker.domain.token.TokenPurpose;
import com.lifetracker.domain.user.Email;
import com.lifetracker.domain.user.RawPassword;
import com.lifetracker.domain.user.User;
import com.lifetracker.domain.user.UserId;
import com.lifetracker.domain.user.WeakPasswordException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResetPasswordTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private final InMemoryUserRepository users = new InMemoryUserRepository();
    private final FakeOneTimeTokens tokens = new FakeOneTimeTokens();
    private final InMemoryOneTimeTokenRepository tokenStore = new InMemoryOneTimeTokenRepository();
    private final FakePasswordHasher hasher = new FakePasswordHasher();
    private final NoSessionsRepository sessions = new NoSessionsRepository();
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final ResetPassword reset = new ResetPassword(tokens, tokenStore, users, hasher, sessions, clock);

    private final User user =
            User.register(UserId.generate(), new Email("sam@example.com"), hasher.hash(new RawPassword("old password here")));

    private String issue(TokenPurpose purpose, Instant expiresAt) {
        users.save(user);
        OneTimeTokens.Issued issued = tokens.issue();
        tokenStore.save(OneTimeToken.issue(OneTimeTokenId.generate(), user.id(), issued.hash(), purpose, expiresAt, NOW));
        return issued.value().value();
    }

    @Test
    void sets_the_new_password_and_consumes_the_token() {
        String token = issue(TokenPurpose.RESET_PASSWORD, NOW.plus(Duration.ofHours(1)));

        reset.execute(new ResetPasswordCommand(token, "brand new password"));

        User updated = users.findById(user.id()).orElseThrow();
        assertTrue(hasher.matches(new RawPassword("brand new password"), updated.passwordHash()));
        assertEquals(0, tokenStore.size());
    }

    @Test
    void rejects_an_unknown_token() {
        assertThrows(InvalidTokenException.class,
                () -> reset.execute(new ResetPasswordCommand("nope", "brand new password")));
    }

    @Test
    void rejects_a_verification_token_used_for_reset() {
        String token = issue(TokenPurpose.VERIFY_EMAIL, NOW.plus(Duration.ofHours(1)));
        assertThrows(InvalidTokenException.class,
                () -> reset.execute(new ResetPasswordCommand(token, "brand new password")));
    }

    @Test
    void rejects_a_weak_new_password_without_consuming_the_token() {
        String token = issue(TokenPurpose.RESET_PASSWORD, NOW.plus(Duration.ofHours(1)));

        assertThrows(WeakPasswordException.class,
                () -> reset.execute(new ResetPasswordCommand(token, "short")));
        assertEquals(1, tokenStore.size()); // the link is still usable
    }

    @Test
    void rejects_an_expired_token() {
        String token = issue(TokenPurpose.RESET_PASSWORD, NOW.minus(Duration.ofSeconds(1)));
        assertThrows(InvalidTokenException.class,
                () -> reset.execute(new ResetPasswordCommand(token, "brand new password")));
    }
}
