package com.lifetracker.infrastructure.web;

/** Thrown by {@code GET /accounts/{id}} when there is no such account in the caller's Book. Maps to 404. */
final class AccountNotFoundException extends RuntimeException {

    AccountNotFoundException() {
        super("account not found");
    }
}
