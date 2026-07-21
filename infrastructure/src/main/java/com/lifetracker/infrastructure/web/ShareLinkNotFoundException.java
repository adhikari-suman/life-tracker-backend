package com.lifetracker.infrastructure.web;

/** Thrown by {@code GET /me/share-link} when link sharing is off for the caller. Maps to a 404. */
final class ShareLinkNotFoundException extends RuntimeException {

    ShareLinkNotFoundException() {
        super("link sharing is off");
    }
}
