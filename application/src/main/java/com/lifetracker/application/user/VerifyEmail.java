package com.lifetracker.application.user;

import com.lifetracker.domain.token.OneTimeToken;
import com.lifetracker.domain.token.OneTimeTokenHash;
import com.lifetracker.domain.token.OneTimeTokenRepository;
import com.lifetracker.domain.token.OneTimeTokenValue;
import com.lifetracker.domain.token.OneTimeTokens;
import com.lifetracker.domain.token.TokenPurpose;
import com.lifetracker.domain.user.User;
import com.lifetracker.domain.user.UserRepository;

import java.time.Clock;

/**
 * Verifies an email from a token: hash the presented secret, find the matching VERIFY_EMAIL token,
 * reject it if unknown, wrong-purpose, or expired ({@link InvalidTokenException}), then mark the
 * User verified and consume the token. Public — the token is the credential.
 */
public final class VerifyEmail {

    private final OneTimeTokens tokens;
    private final OneTimeTokenRepository tokenStore;
    private final UserRepository users;
    private final Clock clock;

    public VerifyEmail(OneTimeTokens tokens, OneTimeTokenRepository tokenStore, UserRepository users, Clock clock) {
        this.tokens = tokens;
        this.tokenStore = tokenStore;
        this.users = users;
        this.clock = clock;
    }

    public void execute(String presentedToken) {
        OneTimeToken token = tokenStore.findByHash(hash(presentedToken))
                .filter(t -> t.isFor(TokenPurpose.VERIFY_EMAIL))
                .filter(t -> !t.isExpired(clock.instant()))
                .orElseThrow(InvalidTokenException::new);

        User user = users.findById(token.userId())
                .orElseThrow(() -> new IllegalStateException("token references a missing user"));
        user.verifyEmail();
        users.save(user);
        tokenStore.delete(token);
    }

    private OneTimeTokenHash hash(String presented) {
        if (presented == null || presented.isBlank()) {
            throw new InvalidTokenException();
        }
        return tokens.hash(new OneTimeTokenValue(presented));
    }
}
