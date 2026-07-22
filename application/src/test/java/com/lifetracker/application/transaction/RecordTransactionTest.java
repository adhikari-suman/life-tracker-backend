package com.lifetracker.application.transaction;

import com.lifetracker.domain.account.Account;
import com.lifetracker.domain.account.AccountId;
import com.lifetracker.domain.account.AccountKind;
import com.lifetracker.domain.account.AccountName;
import com.lifetracker.domain.ledger.OwnerId;
import com.lifetracker.domain.money.CurrencyMismatchException;
import com.lifetracker.domain.money.Money;
import com.lifetracker.domain.transaction.Posting;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecordTransactionTest {

    private static final Currency USD = Currency.getInstance("USD");
    private static final Currency EUR = Currency.getInstance("EUR");
    private static final LocalDate DATE = LocalDate.of(2026, 7, 21);

    private final InMemoryAccountRepository accounts = new InMemoryAccountRepository();
    private final InMemoryTransactionRepository transactions = new InMemoryTransactionRepository();
    private final RecordTransaction record = new RecordTransaction(accounts, transactions);
    private final OwnerId owner = OwnerId.of(UUID.randomUUID());

    private AccountId account(String name, AccountKind kind, Currency currency) {
        Account a = Account.open(AccountId.generate(), new AccountName(name), kind, currency);
        accounts.save(owner, a);
        return a.id();
    }

    private static Money money(String amount, Currency currency) {
        return new Money(new BigDecimal(amount), currency);
    }

    @Test
    void records_a_balanced_movement_crediting_from_and_debiting_to() {
        AccountId bank = account("Bank", AccountKind.ASSET, USD);
        AccountId groceries = account("Groceries", AccountKind.EXPENSE, USD);

        record.execute(new RecordTransactionCommand(owner, DATE, bank, groceries, money("50.00", USD), null));

        assertEquals(1, transactions.saved.size());
        List<Posting> postings = transactions.saved.get(0).postings();
        assertTrue(postings.contains(Posting.credit(bank, money("50.00", USD))));
        assertTrue(postings.contains(Posting.debit(groceries, money("50.00", USD))));
    }

    @Test
    void records_a_cross_currency_movement_with_both_real_amounts() {
        AccountId usd = account("USD Bank", AccountKind.ASSET, USD);
        AccountId eur = account("EUR Bank", AccountKind.ASSET, EUR);

        record.execute(new RecordTransactionCommand(owner, DATE, usd, eur, money("100.00", USD), money("90.00", EUR)));

        List<Posting> postings = transactions.saved.get(0).postings();
        assertTrue(postings.contains(Posting.credit(usd, money("100.00", USD))));   // leaves USD
        assertTrue(postings.contains(Posting.debit(eur, money("90.00", EUR))));      // arrives EUR
        // The derived rate is asserted end-to-end in the HTTP integration test.
    }

    @Test
    void rejects_a_movement_to_the_same_account() {
        AccountId bank = account("Bank", AccountKind.ASSET, USD);
        assertThrows(SameAccountException.class,
                () -> record.execute(new RecordTransactionCommand(owner, DATE, bank, bank, money("10.00", USD), null)));
    }

    @Test
    void rejects_a_reference_to_an_unknown_account() {
        AccountId groceries = account("Groceries", AccountKind.EXPENSE, USD);
        assertThrows(UnknownAccountException.class,
                () -> record.execute(new RecordTransactionCommand(owner, DATE, AccountId.generate(), groceries, money("10.00", USD), null)));
    }

    @Test
    void rejects_a_cross_currency_movement_without_the_second_amount() {
        AccountId usd = account("USD Bank", AccountKind.ASSET, USD);
        AccountId eur = account("EUR Bank", AccountKind.ASSET, EUR);
        assertThrows(ConvertedAmountRequiredException.class,
                () -> record.execute(new RecordTransactionCommand(owner, DATE, usd, eur, money("10.00", USD), null)));
    }

    @Test
    void rejects_an_amount_not_in_the_source_accounts_currency() {
        AccountId bank = account("Bank", AccountKind.ASSET, USD);
        AccountId groceries = account("Groceries", AccountKind.EXPENSE, USD);
        assertThrows(CurrencyMismatchException.class,
                () -> record.execute(new RecordTransactionCommand(owner, DATE, bank, groceries, money("10.00", EUR), null)));
    }
}
