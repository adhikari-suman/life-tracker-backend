package com.lifetracker.application.user;

import com.lifetracker.domain.session.Session;
import com.lifetracker.domain.session.SessionRepository;
import com.lifetracker.domain.token.OneTimeToken;
import com.lifetracker.domain.token.OneTimeTokenHash;
import com.lifetracker.domain.token.OneTimeTokenRepository;
import com.lifetracker.domain.token.OneTimeTokenValue;
import com.lifetracker.domain.token.OneTimeTokens;
import com.lifetracker.domain.token.TokenPurpose;
import com.lifetracker.domain.user.PasswordHasher;
import com.lifetracker.domain.user.RawPassword;
import com.lifetracker.domain.user.User;
import com.lifetracker.domain.user.UserRepository;

import java.time.Clock;

/**
 * Completes a password reset: validate the token and the new password, set the new credential,
 * consume the token, and revoke EVERY Session (a reset answers a possible takeover, ADR-0011). The
 * new password is validated BEFORE the token is consumed, so a weak one lets the same link be retried.
 */
public final class ResetPassword {

    private final OneTimeTokens tokens;
    private final OneTimeTokenRepository tokenStore;
    private final UserRepository users;
    private final PasswordHasher passwordHasher;
    private final SessionRepository sessions;
    private final Clock clock;

    public ResetPassword(OneTimeTokens tokens, OneTimeTokenRepository tokenStore, UserRepository users,
                         PasswordHasher passwordHasher, SessionRepository sessions, Clock clock) {
        this.tokens = tokens;
        this.tokenStore = tokenStore;
        this.users = users;
        this.passwordHasher = passwordHasher;
        this.sessions = sessions;
        this.clock = clock;
    }

    public void execute(ResetPasswordCommand command) {
        OneTimeToken token = tokenStore.findByHash(hash(command.token()))
                .filter(t -> t.isFor(TokenPurpose.RESET_PASSWORD))
                .filter(t -> !t.isExpired(clock.instant()))
                .orElseThrow(InvalidTokenException::new);

        RawPassword newPassword = new RawPassword(command.newPassword()); // WeakPasswordException -> 422

        User user = users.findById(token.userId())
                .orElseThrow(() -> new IllegalStateException("token references a missing user"));
        user.changePassword(passwordHasher.hash(newPassword));
        users.save(user);
        tokenStore.delete(token);

        // A reset is a takeover response: kill every existing login.
        for (Session session : sessions.findByUserId(user.id())) {
            session.revoke();
            sessions.save(session);
        }
    }

    private OneTimeTokenHash hash(String presented) {
        if (presented == null || presented.isBlank()) {
            throw new InvalidTokenException();
        }
        return tokens.hash(new OneTimeTokenValue(presented));
    }
}
