package com.lifetracker.infrastructure.web;

/** Thrown by {@code GET /transactions/{id}} when there is no such transaction in the caller's Book. Maps to 404. */
final class TransactionNotFoundException extends RuntimeException {

    TransactionNotFoundException() {
        super("transaction not found");
    }
}
