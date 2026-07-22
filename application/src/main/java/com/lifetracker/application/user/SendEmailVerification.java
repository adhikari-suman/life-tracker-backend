package com.lifetracker.application.user;

import com.lifetracker.domain.notification.EmailSender;
import com.lifetracker.domain.token.OneTimeToken;
import com.lifetracker.domain.token.OneTimeTokenId;
import com.lifetracker.domain.token.OneTimeTokenRepository;
import com.lifetracker.domain.token.OneTimeTokens;
import com.lifetracker.domain.token.TokenPurpose;
import com.lifetracker.domain.user.User;
import com.lifetracker.domain.user.UserId;
import com.lifetracker.domain.user.UserRepository;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Issues and sends an email-verification link for a User — called on registration and by the resend
 * endpoint. A no-op if the email is already verified. Issuing a new token invalidates the User's
 * prior verification tokens, so only the latest link works (ADR-0011).
 */
public final class SendEmailVerification {

    private final UserRepository users;
    private final OneTimeTokens tokens;
    private final OneTimeTokenRepository tokenStore;
    private final EmailSender emailSender;
    private final Duration ttl;
    private final Clock clock;

    public SendEmailVerification(UserRepository users, OneTimeTokens tokens, OneTimeTokenRepository tokenStore,
                                 EmailSender emailSender, Duration ttl, Clock clock) {
        this.users = users;
        this.tokens = tokens;
        this.tokenStore = tokenStore;
        this.emailSender = emailSender;
        this.ttl = ttl;
        this.clock = clock;
    }

    public void execute(UserId userId) {
        User user = users.findById(userId)
                .orElseThrow(() -> new IllegalStateException("no user for id " + userId));
        if (user.isEmailVerified()) {
            return; // nothing to verify
        }
        tokenStore.deleteByUserIdAndPurpose(userId, TokenPurpose.VERIFY_EMAIL);

        Instant now = clock.instant();
        OneTimeTokens.Issued issued = tokens.issue();
        tokenStore.save(OneTimeToken.issue(
                OneTimeTokenId.generate(), userId, issued.hash(), TokenPurpose.VERIFY_EMAIL, now.plus(ttl), now));
        emailSender.sendEmailVerification(user.email(), issued.value());
    }
}
