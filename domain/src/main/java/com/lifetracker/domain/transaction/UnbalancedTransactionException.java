package com.lifetracker.domain.transaction;

/**
 * Thrown when a {@link Transaction}'s postings do not balance — too few postings, or debits not
 * equal to credits within a currency (ADR-0002). The typo-catch at the heart of double-entry.
 */
public final class UnbalancedTransactionException extends RuntimeException {

    public UnbalancedTransactionException(String message) {
        super(message);
    }
}
