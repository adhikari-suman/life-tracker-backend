package com.lifetracker.infrastructure;

import com.lifetracker.application.account.OpenAccount;
import com.lifetracker.application.account.OpenAccountCommand;
import com.lifetracker.application.transaction.RecordTransaction;
import com.lifetracker.application.transaction.RecordTransactionCommand;
import com.lifetracker.domain.account.AccountId;
import com.lifetracker.domain.ledger.OwnerId;
import com.lifetracker.domain.money.Money;
import com.lifetracker.infrastructure.persistence.account.AccountQueryService;
import com.lifetracker.infrastructure.persistence.account.AccountView;
import com.lifetracker.infrastructure.persistence.transaction.TransactionQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Currency;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Ledger persistence against real Postgres. Boot proves AccountEntity / TransactionEntity /
 * PostingEntity match the 007 / 008 migrations (drift check); the round-trip proves accounts,
 * movements, signed balances computed from postings, and owner isolation.
 */
class LedgerPersistenceIntegrationTest extends AbstractIntegrationTest {

    private static final Currency USD = Currency.getInstance("USD");

    @Autowired
    OpenAccount openAccount;

    @Autowired
    RecordTransaction recordTransaction;

    @Autowired
    AccountQueryService accountQuery;

    @Autowired
    TransactionQueryService transactionQuery;

    private static Money usd(String amount) {
        return new Money(new BigDecimal(amount), USD);
    }

    @Test
    void accounts_and_movements_round_trip_with_correct_signed_balances() {
        OwnerId owner = OwnerId.of(UUID.randomUUID());
        AccountId equity = openAccount.execute(new OpenAccountCommand(owner, "Opening", "EQUITY", "USD"));
        AccountId bank = openAccount.execute(new OpenAccountCommand(owner, "Bank", "ASSET", "USD"));
        AccountId groceries = openAccount.execute(new OpenAccountCommand(owner, "Groceries", "EXPENSE", "USD"));

        // Opening balance: $1000 from Equity into the bank. Then spend $50 from the bank.
        recordTransaction.execute(new RecordTransactionCommand(owner, LocalDate.of(2026, 7, 1), LocalTime.of(9, 0), equity, bank, usd("1000.00"), null, null));
        recordTransaction.execute(new RecordTransactionCommand(owner, LocalDate.of(2026, 7, 2), LocalTime.of(12, 30), bank, groceries, usd("50.00"), null, null));

        Map<UUID, BigDecimal> balances = accountQuery.findByOwner(owner).stream()
                .collect(Collectors.toMap(AccountView::id, AccountView::balance));
        assertEquals(new BigDecimal("950.0000"), balances.get(bank.value()));
        assertEquals(new BigDecimal("50.0000"), balances.get(groceries.value()));
        assertEquals(new BigDecimal("1000.0000"), balances.get(equity.value()));

        assertEquals(2, transactionQuery.findByOwner(owner, null).size());
        assertEquals(1, transactionQuery.findByOwner(owner, groceries.value()).size());
    }

    @Test
    void one_owners_book_is_isolated_from_another() {
        OwnerId a = OwnerId.of(UUID.randomUUID());
        OwnerId b = OwnerId.of(UUID.randomUUID());
        openAccount.execute(new OpenAccountCommand(a, "A's Bank", "ASSET", "USD"));

        assertTrue(accountQuery.findByOwner(b).isEmpty());
    }
}
