package com.lifetracker.domain.user;

/**
 * Thrown when a {@link RawPassword} fails policy. Carries the bounds, never the password itself.
 */
public final class WeakPasswordException extends RuntimeException {

    public WeakPasswordException(int minLength, int maxLength) {
        super("password length must be between " + minLength + " and " + maxLength + " characters");
    }
}
