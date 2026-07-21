package com.lifetracker.application.sharing;

/**
 * Thrown when a View Grant is attempted for an email that belongs to no registered User. Maps to a
 * 404 — "ask them to sign up first" (pending-by-email invites are deferred, ADR-0008).
 */
public final class GranteeNotFoundException extends RuntimeException {

    public GranteeNotFoundException() {
        super("no registered user owns that email");
    }
}
