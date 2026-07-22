package com.lifetracker.application.transaction;

/**
 * Thrown when a cross-currency movement omits the amount arriving in the destination account
 * ({@code toAmount}). Each currency's real figure must be given; the rate is derived, not applied
 * (ADR-0002). Maps to 422 CONVERTED_AMOUNT_REQUIRED.
 */
public final class ConvertedAmountRequiredException extends RuntimeException {

    public ConvertedAmountRequiredException() {
        super("a cross-currency movement needs the amount arriving in the destination (toAmount)");
    }
}
