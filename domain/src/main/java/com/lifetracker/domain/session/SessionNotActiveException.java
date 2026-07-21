package com.lifetracker.domain.session;

/**
 * Thrown when a revoked or expired {@link Session} is asked to rotate. Maps to a 401 at the
 * boundary.
 */
public final class SessionNotActiveException extends RuntimeException {

    public SessionNotActiveException(SessionId sessionId) {
        super("session is not active (revoked or expired): " + sessionId.value());
    }
}
