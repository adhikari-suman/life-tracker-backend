package com.lifetracker.application.user;

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
import static org.junit.jupiter.api.Assertions.assertTrue;

class SendEmailVerificationTest {

    private final InMemoryUserRepository users = new InMemoryUserRepository();
    private final FakeOneTimeTokens tokens = new FakeOneTimeTokens();
    private final InMemoryOneTimeTokenRepository tokenStore = new InMemoryOneTimeTokenRepository();
    private final RecordingEmailSender emailSender = new RecordingEmailSender();
    private final Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
    private final SendEmailVerification send =
            new SendEmailVerification(users, tokens, tokenStore, emailSender, Duration.ofHours(24), clock);

    private User unverified(String email) {
        User user = User.register(UserId.generate(), new Email(email), new PasswordHash("hash"));
        users.save(user);
        return user;
    }

    @Test
    void issues_a_token_and_sends_a_verification_email() {
        User user = unverified("sam@example.com");

        send.execute(user.id());

        assertEquals(1, tokenStore.size());
        RecordingEmailSender.Sent sent = emailSender.last();
        assertEquals("VERIFY", sent.kind());
        assertEquals("sam@example.com", sent.email());
    }

    @Test
    void resending_invalidates_the_previous_token() {
        User user = unverified("sam@example.com");

        send.execute(user.id());
        send.execute(user.id());

        assertEquals(1, tokenStore.countFor(user.id(), TokenPurpose.VERIFY_EMAIL));
        assertEquals(2, emailSender.sent.size());
    }

    @Test
    void does_nothing_for_an_already_verified_user() {
        User user = unverified("sam@example.com");
        user.verifyEmail();
        users.save(user);

        send.execute(user.id());

        assertEquals(0, tokenStore.size());
        assertTrue(emailSender.sent.isEmpty());
    }
}
