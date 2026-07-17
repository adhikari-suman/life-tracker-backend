package com.lifetracker.domain.money;

/**
 * Thrown when a {@link Money} is divided by zero. A named domain exception rather than a raw
 * {@link ArithmeticException} bubbling up from {@code BigDecimal}.
 */
public final class DivisionByZeroException extends RuntimeException {

    public DivisionByZeroException() {
        super("cannot divide money by zero");
    }
}
