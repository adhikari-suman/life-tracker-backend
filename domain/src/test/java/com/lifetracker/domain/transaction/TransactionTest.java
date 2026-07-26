package com.lifetracker.domain.transaction;

import com.lifetracker.domain.account.AccountId;
import com.lifetracker.domain.money.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Currency;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TransactionTest {

    private static final Currency USD = Currency.getInstance("USD");
    private static final LocalDate DATE = LocalDate.of(2026, 7, 21);
    private static final LocalTime TIME = LocalTime.of(19, 42);

    private final AccountId bank = AccountId.generate();
    private final AccountId groceries = AccountId.generate();

    private static Money usd(String amount) {
        return new Money(new BigDecimal(amount), USD);
    }

    @Test
    void a_balanced_movement_is_recorded() {
        assertDoesNotThrow(() -> Transaction.record(TransactionId.generate(), DATE, TIME, List.of(
                Posting.credit(bank, usd("50.00")),
                Posting.debit(groceries, usd("50.00")))));
    }

    @Test
    void unequal_debits_and_credits_are_rejected() {
        assertThrows(UnbalancedTransactionException.class,
                () -> Transaction.record(TransactionId.generate(), DATE, TIME, List.of(
                        Posting.credit(bank, usd("50.00")),
                        Posting.debit(groceries, usd("40.00")))));
    }

    @Test
    void fewer_than_two_postings_is_rejected() {
        assertThrows(UnbalancedTransactionException.class,
                () -> Transaction.record(TransactionId.generate(), DATE, TIME, List.of(
                        Posting.credit(bank, usd("50.00")))));
    }

    @Test
    void a_split_with_one_credit_and_many_debits_balances() {
        // The shared-bill split: $80 out of bank, $20 my food and $20 to each of three receivables.
        AccountId food = AccountId.generate();
        AccountId f1 = AccountId.generate();
        AccountId f2 = AccountId.generate();
        AccountId f3 = AccountId.generate();
        assertDoesNotThrow(() -> Transaction.record(TransactionId.generate(), DATE, TIME, List.of(
                Posting.credit(bank, usd("80.00")),
                Posting.debit(food, usd("20.00")),
                Posting.debit(f1, usd("20.00")),
                Posting.debit(f2, usd("20.00")),
                Posting.debit(f3, usd("20.00")))));
    }
}
