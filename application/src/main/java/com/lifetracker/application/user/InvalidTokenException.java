package com.lifetracker.application.user;

/**
 * Thrown when a presented verification or reset token is missing, unknown, of the wrong purpose,
 * expired, or already used. One exception for every failure, so a probe learns nothing. Maps to 400.
 */
public final class InvalidTokenException extends RuntimeException {

    public InvalidTokenException() {
        super("invalid or expired token");
    }
}
