package com.lifetracker.domain.money;

/**
 * Thrown when a {@link Money} is constructed with more decimal places than the currency's
 * storage scale allows. Rejected rather than rounded: a value object must not silently drop
 * precision the caller handed it.
 *
 * <p>The amount is carried as a {@code String}, never a {@code BigDecimal} — the architecture
 * test keeps {@code BigDecimal} inside {@code Money} alone.
 */
public final class ExcessScaleException extends RuntimeException {

    public ExcessScaleException(String amount, int maxScale) {
        super("amount has more than " + maxScale + " decimal places: " + amount);
    }
}
