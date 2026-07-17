package com.lifetracker.domain.money;

import java.util.Currency;

/**
 * Thrown when an operation combines two {@link Money} of different currencies (for example
 * adding dollars to euros). Combining them needs an exchange rate, which is not this object's
 * responsibility.
 */
public final class CurrencyMismatchException extends RuntimeException {

    public CurrencyMismatchException(Currency left, Currency right) {
        super("cannot operate across currencies: " + left.getCurrencyCode()
                + " and " + right.getCurrencyCode());
    }
}
