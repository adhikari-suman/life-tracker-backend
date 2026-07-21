package com.lifetracker.domain.session;

/**
 * Thrown when a retired refresh token is replayed against a {@link Session} — the theft signal.
 * The Session is revoked as this is thrown; the caller must persist that. Maps to a 401 at the
 * boundary, indistinguishable from an ordinary expired-token failure.
 */
public final class RefreshTokenReuseException extends RuntimeException {

    public RefreshTokenReuseException(SessionId sessionId) {
        super("refresh token reuse detected; session revoked: " + sessionId.value());
    }
}
