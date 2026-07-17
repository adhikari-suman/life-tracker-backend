package com.lifetracker.domain.money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

/**
 * A monetary amount in a single currency. A value object: two {@code Money} with the same
 * amount and currency are equal.
 *
 * <p>The amount is always normalized to {@link #SCALE} decimal places (the same scale as the
 * {@code NUMERIC(19,4)} money columns), so equal values share one representation. That is what
 * makes the record's generated {@code equals}/{@code hashCode} correct despite
 * {@code BigDecimal} equality being scale-sensitive
 * ({@code new BigDecimal("2.0").equals(new BigDecimal("2.00"))} is {@code false}).
 */
public record Money(BigDecimal amount, Currency currency) {

    /** Decimal places every amount is stored at. Matches the DB's {@code NUMERIC(19,4)}. */
    private static final int SCALE = 4;

    public Money {
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(currency, "currency");
        if (amount.signum() < 0) {
            throw new NegativeAmountException(amount.toPlainString());
        }
        if (amount.scale() > SCALE) {
            throw new ExcessScaleException(amount.toPlainString(), SCALE);
        }
        // Only ever pads with zeros (scale <= SCALE was enforced above), so UNNECESSARY
        // never actually rounds — it just proves we are not silently losing precision here.
        amount = amount.setScale(SCALE, RoundingMode.UNNECESSARY);
    }

    /**
     * This amount plus {@code other}. Both must be the same currency — adding across
     * currencies is meaningless without an exchange rate, which is not this object's job.
     */
    public Money plus(Money other) {
        if (!currency.equals(other.currency)) {
            throw new CurrencyMismatchException(currency, other.currency);
        }
        return new Money(amount.add(other.amount), currency);
    }

    /**
     * This amount divided by {@code divisor}, rounded with the given mode. There is
     * deliberately no overload without a {@link RoundingMode}: money division rarely divides
     * evenly, so the caller must always say how to round rather than get a surprise
     * {@link ArithmeticException} or a silent default.
     */
    public Money dividedBy(BigDecimal divisor, RoundingMode roundingMode) {
        Objects.requireNonNull(divisor, "divisor");
        Objects.requireNonNull(roundingMode, "roundingMode");
        if (divisor.signum() == 0) {
            throw new DivisionByZeroException();
        }
        return new Money(amount.divide(divisor, SCALE, roundingMode), currency);
    }
}
