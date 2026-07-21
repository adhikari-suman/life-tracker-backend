package com.lifetracker.application.user;

/**
 * Thrown when registration is attempted with an email that already belongs to a User. An
 * application-level exception — uniqueness is a property of the store, not of one User — mapped
 * to a 409 at the boundary.
 */
public final class EmailAlreadyRegisteredException extends RuntimeException {

    public EmailAlreadyRegisteredException(String email) {
        super("email already registered: " + email);
    }
}
