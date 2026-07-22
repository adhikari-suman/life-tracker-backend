package com.lifetracker.infrastructure.web;

/** Thrown when a wire money amount cannot be parsed — bad decimal, unknown currency, or negative. Maps to 422. */
final class MalformedMoneyException extends RuntimeException {

    MalformedMoneyException(String message) {
        super(message);
    }
}
