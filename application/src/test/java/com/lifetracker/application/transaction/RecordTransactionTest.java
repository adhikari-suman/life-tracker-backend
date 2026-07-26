package com.lifetracker.application.transaction;

import com.lifetracker.application.labeling.InMemoryLabelRepository;
import com.lifetracker.application.labeling.InMemoryPostingLabelRepository;
import com.lifetracker.application.labeling.LabelArchivedException;
import com.lifetracker.application.labeling.LabelNotApplicableException;
import com.lifetracker.application.labeling.LabelNotFoundException;
import com.lifetracker.domain.account.Account;
import com.lifetracker.domain.account.AccountId;
import com.lifetracker.domain.account.AccountKind;
import com.lifetracker.domain.account.AccountName;
import com.lifetracker.domain.labeling.Label;
import com.lifetracker.domain.labeling.LabelId;
import com.lifetracker.domain.labeling.LabelName;
import com.lifetracker.domain.ledger.EntrySide;
import com.lifetracker.domain.ledger.OwnerId;
import com.lifetracker.domain.money.CurrencyMismatchException;
import com.lifetracker.domain.money.Money;
import com.lifetracker.domain.transaction.Posting;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecordTransactionTest {

    private static final Currency USD = Currency.getInstance("USD");
    private static final Currency EUR = Currency.getInstance("EUR");
    private static final LocalDate DATE = LocalDate.of(2026, 7, 21);
    private static final LocalTime TIME = LocalTime.of(8, 15);

    private final InMemoryAccountRepository accounts = new InMemoryAccountRepository();
    private final InMemoryTransactionRepository transactions = new InMemoryTransactionRepository();
    private final InMemoryLabelRepository labels = new InMemoryLabelRepository();
    private final InMemoryPostingLabelRepository postingLabels = new InMemoryPostingLabelRepository();
    private final RecordTransaction record = new RecordTransaction(accounts, transactions, labels, postingLabels);
    private final OwnerId owner = OwnerId.of(UUID.randomUUID());

    private AccountId account(String name, AccountKind kind, Currency currency) {
        Account a = Account.open(AccountId.generate(), new AccountName(name), kind, currency);
        accounts.save(owner, a);
        return a.id();
    }

    private LabelId label(String name) {
        Label label = Label.root(LabelId.generate(), new LabelName(name));
        labels.save(owner, label);
        return label.id();
    }

    private static Money money(String amount, Currency currency) {
        return new Money(new BigDecimal(amount), currency);
    }

    /**
     * Postings are compared by their parts, not by object equality: a posting now carries an id, so two
     * legs with identical amounts are correctly NOT equal — which matters the moment a split books two
     * identical £20 legs to the same account.
     */
    private static boolean hasLeg(List<Posting> postings, AccountId account, EntrySide side, Money amount) {
        return postings.stream().anyMatch(p -> p.accountId().equals(account)
                && p.side() == side && p.amount().equals(amount));
    }

    private static Optional<Posting> leg(List<Posting> postings, AccountId account) {
        return postings.stream().filter(p -> p.accountId().equals(account)).findFirst();
    }

    @Test
    void records_a_balanced_movement_crediting_from_and_debiting_to() {
        AccountId bank = account("Bank", AccountKind.ASSET, USD);
        AccountId groceries = account("Groceries", AccountKind.EXPENSE, USD);

        record.execute(new RecordTransactionCommand(owner, DATE, TIME, bank, groceries, money("50.00", USD), null, null));

        assertEquals(1, transactions.saved.size());
        List<Posting> postings = transactions.saved.get(0).postings();
        assertTrue(hasLeg(postings, bank, EntrySide.CREDIT, money("50.00", USD)));
        assertTrue(hasLeg(postings, groceries, EntrySide.DEBIT, money("50.00", USD)));
    }

    @Test
    void records_a_cross_currency_movement_with_both_real_amounts() {
        AccountId usd = account("USD Bank", AccountKind.ASSET, USD);
        AccountId eur = account("EUR Bank", AccountKind.ASSET, EUR);

        record.execute(new RecordTransactionCommand(owner, DATE, TIME, usd, eur, money("100.00", USD), money("90.00", EUR), null));

        List<Posting> postings = transactions.saved.get(0).postings();
        assertTrue(hasLeg(postings, usd, EntrySide.CREDIT, money("100.00", USD)));   // leaves USD
        assertTrue(hasLeg(postings, eur, EntrySide.DEBIT, money("90.00", EUR)));      // arrives EUR
        // The derived rate is asserted end-to-end in the HTTP integration test.
    }

    @Test
    void rejects_a_movement_to_the_same_account() {
        AccountId bank = account("Bank", AccountKind.ASSET, USD);
        assertThrows(SameAccountException.class,
                () -> record.execute(new RecordTransactionCommand(owner, DATE, TIME, bank, bank, money("10.00", USD), null, null)));
    }

    @Test
    void rejects_a_reference_to_an_unknown_account() {
        AccountId groceries = account("Groceries", AccountKind.EXPENSE, USD);
        assertThrows(UnknownAccountException.class,
                () -> record.execute(new RecordTransactionCommand(owner, DATE, TIME, AccountId.generate(), groceries, money("10.00", USD), null, null)));
    }

    @Test
    void rejects_a_cross_currency_movement_without_the_second_amount() {
        AccountId usd = account("USD Bank", AccountKind.ASSET, USD);
        AccountId eur = account("EUR Bank", AccountKind.ASSET, EUR);
        assertThrows(ConvertedAmountRequiredException.class,
                () -> record.execute(new RecordTransactionCommand(owner, DATE, TIME, usd, eur, money("10.00", USD), null, null)));
    }

    @Test
    void rejects_an_amount_not_in_the_source_accounts_currency() {
        AccountId bank = account("Bank", AccountKind.ASSET, USD);
        AccountId groceries = account("Groceries", AccountKind.EXPENSE, USD);
        assertThrows(CurrencyMismatchException.class,
                () -> record.execute(new RecordTransactionCommand(owner, DATE, TIME, bank, groceries, money("10.00", EUR), null, null)));
    }

    // ---------- Labels (ADR-0014) ----------

    @Test
    void a_label_lands_on_the_expense_leg_not_the_bank_leg() {
        AccountId bank = account("Bank", AccountKind.ASSET, USD);
        AccountId groceries = account("Groceries", AccountKind.EXPENSE, USD);
        LabelId food = label("food");

        record.execute(new RecordTransactionCommand(owner, DATE, TIME, bank, groceries, money("50.00", USD), null, food.value()));

        List<Posting> postings = transactions.saved.get(0).postings();
        Posting expenseLeg = leg(postings, groceries).orElseThrow();
        Posting bankLeg = leg(postings, bank).orElseThrow();
        assertEquals(Optional.of(food), postingLabels.findByPosting(owner, expenseLeg.id()));
        assertEquals(Optional.empty(), postingLabels.findByPosting(owner, bankLeg.id()),
                "the asset leg has no 'what was this for' to answer");
    }

    @Test
    void a_label_lands_on_the_income_leg_when_money_arrives() {
        AccountId salary = account("Salary", AccountKind.INCOME, USD);
        AccountId bank = account("Bank", AccountKind.ASSET, USD);
        LabelId pay = label("salary");

        // Income is the `from` here -- the boundary leg is the credit, not the debit.
        record.execute(new RecordTransactionCommand(owner, DATE, TIME, salary, bank, money("2000.00", USD), null, pay.value()));

        List<Posting> postings = transactions.saved.get(0).postings();
        assertEquals(Optional.of(pay), postingLabels.findByPosting(owner, leg(postings, salary).orElseThrow().id()));
        assertEquals(Optional.empty(), postingLabels.findByPosting(owner, leg(postings, bank).orElseThrow().id()));
    }

    @Test
    void refuses_a_label_on_an_internal_transfer() {
        AccountId bank = account("Bank", AccountKind.ASSET, USD);
        AccountId cash = account("Cash", AccountKind.ASSET, USD);
        LabelId food = label("food");

        assertThrows(LabelNotApplicableException.class, () -> record.execute(
                new RecordTransactionCommand(owner, DATE, TIME, bank, cash, money("200.00", USD), null, food.value())));
        assertEquals(0, transactions.saved.size(), "a refused label must not leave the transaction recorded");
    }

    @Test
    void refuses_a_label_on_a_debt_payment() {
        AccountId bank = account("Bank", AccountKind.ASSET, USD);
        AccountId card = account("Credit Card", AccountKind.LIABILITY, USD);
        LabelId food = label("food");

        assertThrows(LabelNotApplicableException.class, () -> record.execute(
                new RecordTransactionCommand(owner, DATE, TIME, bank, card, money("300.00", USD), null, food.value())));
    }

    @Test
    void refuses_a_label_on_an_opening_balance() {
        AccountId equity = account("Opening Equity", AccountKind.EQUITY, USD);
        AccountId bank = account("Bank", AccountKind.ASSET, USD);
        LabelId food = label("food");

        assertThrows(LabelNotApplicableException.class, () -> record.execute(
                new RecordTransactionCommand(owner, DATE, TIME, equity, bank, money("3000.00", USD), null, food.value())));
    }

    @Test
    void refuses_a_label_when_both_legs_could_hold_it() {
        AccountId salary = account("Salary", AccountKind.INCOME, USD);
        AccountId fees = account("Fees", AccountKind.EXPENSE, USD);
        LabelId food = label("food");

        // Legal double-entry, but one label cannot say which of the two boundary legs it describes.
        assertThrows(LabelNotApplicableException.class, () -> record.execute(
                new RecordTransactionCommand(owner, DATE, TIME, salary, fees, money("10.00", USD), null, food.value())));
    }

    @Test
    void refuses_an_unknown_or_archived_label() {
        AccountId bank = account("Bank", AccountKind.ASSET, USD);
        AccountId groceries = account("Groceries", AccountKind.EXPENSE, USD);

        assertThrows(LabelNotFoundException.class, () -> record.execute(new RecordTransactionCommand(
                owner, DATE, TIME, bank, groceries, money("5.00", USD), null, UUID.randomUUID())));

        Label retired = Label.root(LabelId.generate(), new LabelName("wedding")).archivedLabel();
        labels.save(owner, retired);
        assertThrows(LabelArchivedException.class, () -> record.execute(new RecordTransactionCommand(
                owner, DATE, TIME, bank, groceries, money("5.00", USD), null, retired.id().value())));
    }

    @Test
    void a_movement_with_no_label_records_normally_and_tags_nothing() {
        AccountId bank = account("Bank", AccountKind.ASSET, USD);
        AccountId groceries = account("Groceries", AccountKind.EXPENSE, USD);

        record.execute(new RecordTransactionCommand(owner, DATE, TIME, bank, groceries, money("4.00", USD), null, null));

        assertEquals(1, transactions.saved.size());
        assertEquals(0, postingLabels.size(), "no label means uncategorized, not an error");
    }
}
