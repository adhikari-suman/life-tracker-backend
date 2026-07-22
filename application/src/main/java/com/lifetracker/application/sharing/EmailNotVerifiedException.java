package com.lifetracker.application.sharing;

/**
 * Thrown when an owner tries to share their Book before verifying their email (ADR-0011). Sharing is
 * the one action gated on verification today; writing your own Book stays open. Maps to 403.
 */
public final class EmailNotVerifiedException extends RuntimeException {

    public EmailNotVerifiedException() {
        super("email is not verified");
    }
}
