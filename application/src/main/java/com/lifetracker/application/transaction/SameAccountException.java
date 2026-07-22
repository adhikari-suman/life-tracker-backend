package com.lifetracker.application.transaction;

/** Thrown when a movement's from and to are the same account. Maps to 422 SAME_ACCOUNT. */
public final class SameAccountException extends RuntimeException {

    public SameAccountException() {
        super("from and to must be different accounts");
    }
}
