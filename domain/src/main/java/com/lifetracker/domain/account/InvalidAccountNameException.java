package com.lifetracker.domain.account;

/** Thrown when an {@link AccountName} is blank or too long. Maps to a 422. */
public final class InvalidAccountNameException extends RuntimeException {

    public InvalidAccountNameException(String message) {
        super(message);
    }
}
