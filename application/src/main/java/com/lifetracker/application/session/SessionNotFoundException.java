package com.lifetracker.application.session;

/**
 * Thrown when a Session to revoke does not exist or does not belong to the requesting User. Maps to
 * a 404 at the boundary — a User's Sessions are never revealed to anyone else.
 */
public final class SessionNotFoundException extends RuntimeException {

    public SessionNotFoundException() {
        super("session not found");
    }
}
