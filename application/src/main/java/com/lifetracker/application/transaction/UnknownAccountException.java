package com.lifetracker.application.transaction;

/** Thrown when a transaction references an account that is not in the caller's Book. Maps to 422 ACCOUNT_NOT_FOUND. */
public final class UnknownAccountException extends RuntimeException {

    public UnknownAccountException() {
        super("account not found");
    }
}
