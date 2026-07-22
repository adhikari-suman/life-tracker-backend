package com.lifetracker.application.transaction;

/**
 * Thrown when a movement spans two currencies — the two real-amounts-with-a-derived-rate recording
 * (ADR-0002) is a later slice. Maps to 422 CROSS_CURRENCY_UNSUPPORTED.
 */
public final class CrossCurrencyUnsupportedException extends RuntimeException {

    public CrossCurrencyUnsupportedException() {
        super("cross-currency movements are not supported yet");
    }
}
