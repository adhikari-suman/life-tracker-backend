package com.lifetracker.application.user;

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
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequestPasswordResetTest {

    private final InMemoryUserRepository users = new InMemoryUserRepository();
    private final FakeOneTimeTokens tokens = new FakeOneTimeTokens();
    private final InMemoryOneTimeTokenRepository tokenStore = new InMemoryOneTimeTokenRepository();
    private final RecordingEmailSender emailSender = new RecordingEmailSender();
    private final Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
    private final RequestPasswordReset request =
            new RequestPasswordReset(users, tokens, tokenStore, emailSender, Duration.ofHours(1), clock);

    private void register(String email) {
        users.save(User.register(UserId.generate(), new Email(email), new PasswordHash("hash")));
    }

    @Test
    void issues_and_sends_a_reset_token_for_a_known_email() {
        register("sam@example.com");

        request.execute("sam@example.com");

        assertEquals(1, tokenStore.size());
        assertEquals("RESET", emailSender.last().kind());
        assertEquals("sam@example.com", emailSender.last().email());
    }

    @Test
    void does_nothing_for_an_unknown_email_but_does_not_reveal_it() {
        request.execute("nobody@example.com");

        assertEquals(0, tokenStore.size());
        assertTrue(emailSender.sent.isEmpty());
    }

    @Test
    void ignores_a_malformed_email() {
        request.execute("not-an-email");

        assertEquals(0, tokenStore.size());
        assertTrue(emailSender.sent.isEmpty());
    }
}
