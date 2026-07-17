package com.lifetracker.domain.money;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Plain JUnit. No Spring, no database, no mocks — a value object is testable with `new`.
 */
class MoneyTest {

    private static final Currency USD = Currency.getInstance("USD");
    private static final Currency EUR = Currency.getInstance("EUR");

    private static Money usd(String amount) {
        return new Money(new BigDecimal(amount), USD);
    }

    @Test
    void rejects_a_negative_amount() {
        assertThrows(NegativeAmountException.class, () -> usd("-1.00"));
    }

    @Test
    void normalizes_scale_so_equal_values_are_equal() {
        Money oneDecimal = usd("2.0");
        Money twoDecimals = usd("2.00");

        // The BigDecimal GOTCHA: new BigDecimal("2.0").equals("2.00") is false. Money must
        // normalize scale so these are the same value...
        assertEquals(oneDecimal, twoDecimals);
        // ...AND hashCode must agree, or every HashSet<Money>/HashMap breaks.
        assertEquals(oneDecimal.hashCode(), twoDecimals.hashCode());
    }

    @Test
    void rejects_an_amount_with_more_than_four_decimals() {
        assertThrows(ExcessScaleException.class, () -> usd("1.23456"));
    }

    @Test
    void plus_across_different_currencies_throws() {
        Money dollars = usd("10.00");
        Money euros = new Money(new BigDecimal("10.00"), EUR);

        assertThrows(CurrencyMismatchException.class, () -> dollars.plus(euros));
    }

    @Test
    void plus_within_the_same_currency_sums() {
        assertEquals(usd("3.00"), usd("1.00").plus(usd("2.00")));
    }

    @Test
    void division_uses_the_rounding_mode_it_is_given() {
        Money amount = usd("20.00");

        // 20 / 3 = 6.6666... — the mode decides the 4th decimal, and there is no
        // dividedBy overload without a RoundingMode, so a caller cannot skip the choice.
        assertEquals(usd("6.6667"), amount.dividedBy(new BigDecimal("3"), RoundingMode.HALF_EVEN));
        assertEquals(usd("6.6666"), amount.dividedBy(new BigDecimal("3"), RoundingMode.FLOOR));
    }

    @Test
    void division_by_zero_throws() {
        Money amount = usd("10.00");

        assertThrows(DivisionByZeroException.class,
                () -> amount.dividedBy(BigDecimal.ZERO, RoundingMode.HALF_EVEN));
    }
}
