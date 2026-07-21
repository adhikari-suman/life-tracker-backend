package com.lifetracker.application.session;

/**
 * Thrown when a refresh fails — unknown session, expired, or a replayed retired token — as one
 * indistinguishable exception, so refresh reveals nothing about which. Maps to a 401 at the boundary.
 */
public final class InvalidRefreshTokenException extends RuntimeException {

    public InvalidRefreshTokenException() {
        super("invalid or expired refresh token");
    }
}
