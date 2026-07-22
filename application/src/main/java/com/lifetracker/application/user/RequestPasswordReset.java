package com.lifetracker.application.user;

import com.lifetracker.domain.notification.EmailSender;
import com.lifetracker.domain.token.OneTimeToken;
import com.lifetracker.domain.token.OneTimeTokenId;
import com.lifetracker.domain.token.OneTimeTokenRepository;
import com.lifetracker.domain.token.OneTimeTokens;
import com.lifetracker.domain.token.TokenPurpose;
import com.lifetracker.domain.user.Email;
import com.lifetracker.domain.user.InvalidEmailException;
import com.lifetracker.domain.user.User;
import com.lifetracker.domain.user.UserRepository;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Requests a password reset: if a User owns the email, issue and send a reset token, invalidating any
 * prior one. Deliberately non-enumerating — it never signals whether the email exists; the caller
 * always sees the same success (ADR-0011). A blank or malformed email is simply "no such user".
 */
public final class RequestPasswordReset {

    private final UserRepository users;
    private final OneTimeTokens tokens;
    private final OneTimeTokenRepository tokenStore;
    private final EmailSender emailSender;
    private final Duration ttl;
    private final Clock clock;

    public RequestPasswordReset(UserRepository users, OneTimeTokens tokens, OneTimeTokenRepository tokenStore,
                                EmailSender emailSender, Duration ttl, Clock clock) {
        this.users = users;
        this.tokens = tokens;
        this.tokenStore = tokenStore;
        this.emailSender = emailSender;
        this.ttl = ttl;
        this.clock = clock;
    }

    public void execute(String rawEmail) {
        if (rawEmail == null || rawEmail.isBlank()) {
            return;
        }
        Email email;
        try {
            email = new Email(rawEmail);
        } catch (InvalidEmailException malformed) {
            return; // a malformed email can own no account -- say nothing, do nothing
        }
        users.findByEmail(email).ifPresent(this::issueAndSend);
    }

    private void issueAndSend(User user) {
        tokenStore.deleteByUserIdAndPurpose(user.id(), TokenPurpose.RESET_PASSWORD);
        Instant now = clock.instant();
        OneTimeTokens.Issued issued = tokens.issue();
        tokenStore.save(OneTimeToken.issue(
                OneTimeTokenId.generate(), user.id(), issued.hash(), TokenPurpose.RESET_PASSWORD, now.plus(ttl), now));
        emailSender.sendPasswordReset(user.email(), issued.value());
    }
}
