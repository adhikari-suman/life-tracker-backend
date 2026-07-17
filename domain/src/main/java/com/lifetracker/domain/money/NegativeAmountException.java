package com.lifetracker.domain.money;

/**
 * Thrown when a {@link Money} is constructed with a negative amount. A named domain exception,
 * not {@code IllegalArgumentException}, so callers can catch exactly this.
 *
 * <p>The offending amount is carried as a {@code String}, never a {@code BigDecimal}: the
 * architecture test keeps {@code BigDecimal} inside {@code Money} alone.
 */
public final class NegativeAmountException extends RuntimeException {

    public NegativeAmountException(String amount) {
        super("amount must not be negative: " + amount);
    }
}
