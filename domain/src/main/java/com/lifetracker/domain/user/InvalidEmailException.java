package com.lifetracker.domain.user;

/**
 * Thrown when an {@link Email} is built from a value that is not a plausible address. A named
 * domain exception, not {@code IllegalArgumentException}, so the boundary can map exactly this
 * to a 422.
 */
public final class InvalidEmailException extends RuntimeException {

    public InvalidEmailException(String value) {
        super("not a valid email address: " + value);
    }
}
