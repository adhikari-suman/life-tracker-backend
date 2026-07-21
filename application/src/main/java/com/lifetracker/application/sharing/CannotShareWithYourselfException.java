package com.lifetracker.application.sharing;

/** Thrown when an owner tries to grant view of their own Book to themselves. Maps to a 422. */
public final class CannotShareWithYourselfException extends RuntimeException {

    public CannotShareWithYourselfException() {
        super("cannot share a book with its own owner");
    }
}
