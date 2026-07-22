package com.lifetracker.infrastructure.web;

import com.lifetracker.domain.ledger.OwnerId;
import com.lifetracker.domain.session.SessionId;
import com.lifetracker.domain.user.UserId;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

/**
 * Reads the identity out of a validated access token: {@code sub} is the {@link UserId} (the owner
 * per ADR-0006), {@code sid} is the {@link SessionId} the token was minted for.
 */
final class AuthPrincipal {

    private AuthPrincipal() {
    }

    static UserId userId(Jwt jwt) {
        return UserId.of(UUID.fromString(jwt.getSubject()));
    }

    /**
     * The Ledger's owner key for this caller — the same id as {@link #userId}, but as the Ledger's own
     * {@link OwnerId} type, so the Ledger context never references a User (CONTEXT-MAP, ADR-0006).
     */
    static OwnerId ownerId(Jwt jwt) {
        return OwnerId.of(userId(jwt).value());
    }

    static SessionId currentSession(Jwt jwt) {
        return SessionId.of(UUID.fromString(jwt.getClaimAsString("sid")));
    }
}
