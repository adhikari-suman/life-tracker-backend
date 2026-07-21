package com.lifetracker.application.user;

/**
 * Thrown by {@link Authenticate} when the email or password is wrong. Deliberately one exception
 * for every failure mode — wrong password, unknown email, malformed input — so none can be told
 * apart. Carries no email and no password. Maps to a 401 at the boundary.
 */
public final class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("invalid email or password");
    }
}
