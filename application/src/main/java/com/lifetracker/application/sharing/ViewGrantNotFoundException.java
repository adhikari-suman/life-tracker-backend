package com.lifetracker.application.sharing;

/**
 * Thrown when a View Grant to revoke does not exist or is not the caller's. Maps to a 404 — another
 * owner's grants are never revealed.
 */
public final class ViewGrantNotFoundException extends RuntimeException {

    public ViewGrantNotFoundException() {
        super("view grant not found");
    }
}
