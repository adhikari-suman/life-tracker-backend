package com.lifetracker.application.sharing;

/** Thrown when the grantee already holds a View Grant on this Book. Maps to a 409. */
public final class ViewGrantAlreadyExistsException extends RuntimeException {

    public ViewGrantAlreadyExistsException() {
        super("that user already has a view grant on this book");
    }
}
