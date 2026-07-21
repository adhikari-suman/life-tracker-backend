package com.lifetracker.application.user;

import java.time.Duration;

/**
 * Thrown by {@link Authenticate} when an email has too many recent failed logins (ADR-0010). It
 * carries how long the caller should wait, which the boundary renders as a {@code Retry-After}
 * header on the 429. Distinct from {@link InvalidCredentialsException} — this says "not now", not
 * "wrong" — but it is raised before any password check, so, like every failure here, it leaks
 * nothing about the guess or whether the email exists.
 */
public final class TooManyAttemptsException extends RuntimeException {

    private final Duration retryAfter;

    public TooManyAttemptsException(Duration retryAfter) {
        super("too many login attempts");
        this.retryAfter = retryAfter;
    }

    public Duration retryAfter() {
        return retryAfter;
    }
}
