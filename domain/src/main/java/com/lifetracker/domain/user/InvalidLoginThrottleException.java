package com.lifetracker.domain.user;

/**
 * Thrown when a {@link LoginThrottle} is built with a nonsensical policy — fewer than one allowed
 * attempt, or a non-positive window. A misconfiguration surfaced at startup, never a runtime path.
 */
public final class InvalidLoginThrottleException extends RuntimeException {

    public InvalidLoginThrottleException(String message) {
        super(message);
    }
}
