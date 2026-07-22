package com.lifetracker.application.account;

/** Thrown when an account's kind or currency is not recognized. Maps to 422. */
public final class InvalidAccountException extends RuntimeException {

    public InvalidAccountException(String message) {
        super(message);
    }
}
