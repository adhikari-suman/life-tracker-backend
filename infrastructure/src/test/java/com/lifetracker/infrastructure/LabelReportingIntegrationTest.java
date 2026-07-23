package com.lifetracker.infrastructure;

import com.lifetracker.application.account.OpenAccount;
import com.lifetracker.application.account.OpenAccountCommand;
import com.lifetracker.application.labeling.AssignPostingLabel;
import com.lifetracker.application.labeling.CreateLabel;
import com.lifetracker.application.labeling.CreateLabelCommand;
import com.lifetracker.application.labeling.UpdateLabel;
import com.lifetracker.application.labeling.UpdateLabelCommand;
import com.lifetracker.application.transaction.RecordTransaction;
import com.lifetracker.application.transaction.RecordTransactionCommand;
import com.lifetracker.domain.account.AccountId;
import com.lifetracker.domain.labeling.LabelId;
import com.lifetracker.domain.ledger.OwnerId;
import com.lifetracker.domain.money.Money;
import com.lifetracker.domain.transaction.PostingId;
import com.lifetracker.infrastructure.persistence.account.AccountQueryService;
import com.lifetracker.infrastructure.persistence.account.AccountView;
import com.lifetracker.infrastructure.persistence.report.AccountAmountView;
import com.lifetracker.infrastructure.persistence.report.LabelAmountView;
import com.lifetracker.infrastructure.persistence.report.ReportQueryService;
import com.lifetracker.infrastructure.persistence.transaction.PostingView;
import com.lifetracker.infrastructure.persistence.transaction.TransactionQueryService;
import com.lifetracker.infrastructure.persistence.transaction.TransactionView;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Label reporting against real Postgres. Boot proves LabelEntity / PostingLabelEntity match the
 * 010 / 011 migrations (the ADR-0009 drift check); the assertions prove the roll-up, the
 * reconciliation invariant, retroactive reparenting, and that retagging never moves a balance.
 */
class LabelReportingIntegrationTest extends AbstractIntegrationTest {

    private static final Currency USD = Currency.getInstance("USD");
    private static final Currency EUR = Currency.getInstance("EUR");
    private static final LocalDate DAY = LocalDate.of(2026, 7, 10);

    @Autowired
    OpenAccount openAccount;

    @Autowired
    RecordTransaction recordTransaction;

    @Autowired
    CreateLabel createLabel;

    @Autowired
    UpdateLabel updateLabel;

    @Autowired
    AssignPostingLabel assignPostingLabel;

    @Autowired
    ReportQueryService reports;

    @Autowired
    AccountQueryService accountQuery;

    @Autowired
    TransactionQueryService transactionQuery;

    private static Money money(String amount, Currency currency) {
        return new Money(new BigDecimal(amount), currency);
    }

    private static BigDecimal amount(String value) {
        return new BigDecimal(value).setScale(4);
    }

    private AccountId account(OwnerId owner, String name, String kind, String currency) {
        return openAccount.execute(new OpenAccountCommand(owner, name, kind, currency));
    }

    private LabelId label(OwnerId owner, String name, LabelId parent) {
        return createLabel.execute(new CreateLabelCommand(owner, name, parent == null ? null : parent.value()));
    }

    private void spend(OwnerId owner, AccountId from, AccountId to, String amount, Currency currency, LabelId label) {
        spendOn(DAY, owner, from, to, amount, currency, label);
    }

    private void spendOn(LocalDate date, OwnerId owner, AccountId from, AccountId to, String amount,
                         Currency currency, LabelId label) {
        recordTransaction.execute(new RecordTransactionCommand(owner, date, from, to, money(amount, currency), null,
                label == null ? null : label.value()));
    }

    private static Map<String, LabelAmountView> byPath(List<LabelAmountView> rows, String currency) {
        return rows.stream().filter(r -> r.currency().equals(currency))
                .collect(Collectors.toMap(LabelAmountView::path, Function.identity()));
    }

    @Test
    void rolls_up_through_the_tree_and_reconciles_with_the_account_breakdown() {
        OwnerId owner = OwnerId.of(UUID.randomUUID());
        AccountId bank = account(owner, "Bank", "ASSET", "USD");
        AccountId groceries = account(owner, "Groceries", "EXPENSE", "USD");
        AccountId takeaway = account(owner, "Takeaway", "EXPENSE", "USD");
        AccountId sundries = account(owner, "Sundries", "EXPENSE", "USD");

        LabelId food = label(owner, "food", null);
        LabelId restaurants = label(owner, "restaurants", food);
        LabelId fastFood = label(owner, "fast food", restaurants);

        spend(owner, bank, groceries, "100.00", USD, food);        // tagged on the PARENT directly
        spend(owner, bank, takeaway, "30.00", USD, fastFood);       // tagged on a leaf, three deep
        spend(owner, bank, sundries, "20.00", USD, null);           // deliberately uncategorized

        List<LabelAmountView> rows = reports.spendingByLabel(owner, null, null);
        Map<String, LabelAmountView> byPath = byPath(rows, "USD");

        // own counts only what is tagged with THIS label; rolledUp adds the descendants.
        assertEquals(amount("100.00"), byPath.get("food").own());
        assertEquals(amount("130.00"), byPath.get("food").rolledUp());

        // An intermediate label with no direct spending still appears, carrying its subtree's total.
        assertEquals(amount("0.00"), byPath.get("food / restaurants").own());
        assertEquals(amount("30.00"), byPath.get("food / restaurants").rolledUp());

        assertEquals(amount("30.00"), byPath.get("food / restaurants / fast food").own());
        assertEquals(amount("30.00"), byPath.get("food / restaurants / fast food").rolledUp());

        // Untagged spending is never dropped -- it lands in Uncategorized, which nests under nothing.
        LabelAmountView uncategorized = byPath.get("Uncategorized");
        assertEquals(amount("20.00"), uncategorized.own());
        assertEquals(uncategorized.own(), uncategorized.rolledUp());
        assertEquals(Optional.empty(), Optional.ofNullable(uncategorized.labelId()));

        // THE INVARIANT: own reconciles with the account breakdown and the totals.
        BigDecimal sumOwn = rows.stream().filter(r -> r.currency().equals("USD"))
                .map(LabelAmountView::own).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal sumByAccount = reports.spending(owner, null, null).stream()
                .map(AccountAmountView::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(amount("150.00"), sumOwn);
        assertEquals(sumByAccount, sumOwn);

        // And the trap it guards: summing rolledUp double-counts, silently. The £30 of fast food is
        // counted three times over -- in its own row, in restaurants', and in food's -- so 150 of real
        // spending reads as 210 (130 + 30 + 30 + 20) with no error anywhere to signal it.
        BigDecimal sumRolledUp = rows.stream().filter(r -> r.currency().equals("USD"))
                .map(LabelAmountView::rolledUp).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(amount("210.00"), sumRolledUp);
        assertNotEquals(sumOwn, sumRolledUp, "rolledUp must never be summed across rows");
    }

    @Test
    void a_refund_nets_against_its_label_and_rolls_the_reduced_total_up_the_tree() {
        OwnerId owner = OwnerId.of(UUID.randomUUID());
        AccountId bank = account(owner, "Bank", "ASSET", "USD");
        AccountId dining = account(owner, "Dining", "EXPENSE", "USD");
        LabelId food = label(owner, "food", null);
        LabelId eatingOut = label(owner, "eating out", food);

        // A refund is NOT income (ADR-0013, and the glossary): it is a new transaction crediting the
        // SAME expense account and the same label the purchase debited. So the netting is just the
        // report's Sdebit - Scredit, with the label carried on the credit's expense leg.
        spend(owner, bank, dining, "100.00", USD, eatingOut);        // buy: expense debited  (+100)
        spend(owner, dining, bank, "30.00", USD, eatingOut);         // refund: same expense credited (-30)

        Map<String, LabelAmountView> byPath = byPath(reports.spendingByLabel(owner, null, null), "USD");

        // The label's own is the NET 70, not the gross 100 -- and that reduced figure is what rolls up.
        assertEquals(amount("70.00"), byPath.get("food / eating out").own());
        assertEquals(amount("70.00"), byPath.get("food").rolledUp());

        // The reconciliation invariant survives the refund: netted own == netted per-account == totals.
        BigDecimal sumOwn = reports.spendingByLabel(owner, null, null).stream()
                .filter(r -> r.currency().equals("USD")).map(LabelAmountView::own)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal sumByAccount = reports.spending(owner, null, null).stream()
                .map(AccountAmountView::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(amount("70.00"), sumOwn);
        assertEquals(sumByAccount, sumOwn);
    }

    @Test
    void a_refund_in_a_later_month_can_make_that_month_read_negative() {
        OwnerId owner = OwnerId.of(UUID.randomUUID());
        AccountId bank = account(owner, "Bank", "ASSET", "USD");
        AccountId shop = account(owner, "Shop", "EXPENSE", "USD");
        LabelId clothes = label(owner, "clothes", null);

        spendOn(LocalDate.of(2026, 7, 15), owner, bank, shop, "50.00", USD, clothes);   // bought in July
        spendOn(LocalDate.of(2026, 8, 15), owner, shop, bank, "30.00", USD, clothes);    // returned in August

        // Over the whole span the label nets to an ordinary 20.
        assertEquals(amount("20.00"),
                byPath(reports.spendingByLabel(owner, null, null), "USD").get("clothes").own());

        // But August alone contains only the refund, so that month reads NEGATIVE. That is correct,
        // not a bug: the spend was recognised in July, and the ledger is append-only -- the refund is
        // negative spending against its own category in the month it actually landed (glossary: Refund).
        Map<String, LabelAmountView> august = byPath(
                reports.spendingByLabel(owner, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31)), "USD");
        assertEquals(amount("-30.00"), august.get("clothes").own());
    }

    @Test
    void a_label_used_in_two_currencies_gets_a_row_per_currency() {
        OwnerId owner = OwnerId.of(UUID.randomUUID());
        AccountId usdBank = account(owner, "USD Bank", "ASSET", "USD");
        AccountId eurBank = account(owner, "EUR Bank", "ASSET", "EUR");
        AccountId usdFood = account(owner, "USD Food", "EXPENSE", "USD");
        AccountId eurFood = account(owner, "EUR Food", "EXPENSE", "EUR");
        LabelId food = label(owner, "food", null);

        spend(owner, usdBank, usdFood, "10.00", USD, food);
        spend(owner, eurBank, eurFood, "25.00", EUR, food);

        List<LabelAmountView> rows = reports.spendingByLabel(owner, null, null);
        assertEquals(amount("10.00"), byPath(rows, "USD").get("food").own());
        assertEquals(amount("25.00"), byPath(rows, "EUR").get("food").own());
    }

    @Test
    void reparenting_reshapes_past_summaries_but_moves_no_balance() {
        OwnerId owner = OwnerId.of(UUID.randomUUID());
        AccountId bank = account(owner, "Bank", "ASSET", "USD");
        AccountId takeaway = account(owner, "Takeaway", "EXPENSE", "USD");

        LabelId food = label(owner, "food", null);
        LabelId fastFood = label(owner, "fast food", food);
        spend(owner, bank, takeaway, "40.00", USD, fastFood);

        assertEquals(amount("40.00"), byPath(reports.spendingByLabel(owner, null, null), "USD")
                .get("food").rolledUp());

        BigDecimal bankBefore = balanceOf(owner, bank);

        // Move it out to the root: history follows the tree as it stands now (ADR-0015).
        updateLabel.execute(new UpdateLabelCommand(owner, fastFood.value(), null, true, null, null));

        Map<String, LabelAmountView> after = byPath(reports.spendingByLabel(owner, null, null), "USD");
        assertFalse(after.containsKey("food"), "food no longer has anything beneath it");
        assertEquals(amount("40.00"), after.get("fast food").own());
        assertEquals(bankBefore, balanceOf(owner, bank), "reparenting a label never touches a balance");
    }

    @Test
    void retagging_an_old_posting_changes_only_the_label_breakdown() {
        OwnerId owner = OwnerId.of(UUID.randomUUID());
        AccountId bank = account(owner, "Bank", "ASSET", "USD");
        AccountId shop = account(owner, "Shop", "EXPENSE", "USD");
        LabelId food = label(owner, "food", null);
        LabelId household = label(owner, "household", null);

        spend(owner, bank, shop, "60.00", USD, food);

        BigDecimal bankBefore = balanceOf(owner, bank);
        BigDecimal shopBefore = balanceOf(owner, shop);
        BigDecimal spendBefore = reports.spending(owner, null, null).stream()
                .map(AccountAmountView::amount).reduce(BigDecimal.ZERO, BigDecimal::add);

        assignPostingLabel.execute(owner, expenseLegOf(owner, shop), household);

        Map<String, LabelAmountView> after = byPath(reports.spendingByLabel(owner, null, null), "USD");
        assertEquals(amount("60.00"), after.get("household").own());
        assertFalse(after.containsKey("food"), "the old label no longer carries it");

        assertEquals(bankBefore, balanceOf(owner, bank));
        assertEquals(shopBefore, balanceOf(owner, shop));
        assertEquals(spendBefore, reports.spending(owner, null, null).stream()
                .map(AccountAmountView::amount).reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    @Test
    void an_archived_label_still_reports_the_history_already_tagged_with_it() {
        OwnerId owner = OwnerId.of(UUID.randomUUID());
        AccountId bank = account(owner, "Bank", "ASSET", "USD");
        AccountId venue = account(owner, "Venue", "EXPENSE", "USD");
        LabelId wedding = label(owner, "wedding", null);
        spend(owner, bank, venue, "5000.00", USD, wedding);

        updateLabel.execute(new UpdateLabelCommand(owner, wedding.value(), null, false, null, true));

        assertEquals(amount("5000.00"), byPath(reports.spendingByLabel(owner, null, null), "USD")
                .get("wedding").own(), "archiving hides it from the picker, never from history");
    }

    @Test
    void a_transfer_never_reaches_the_label_breakdown_at_all() {
        OwnerId owner = OwnerId.of(UUID.randomUUID());
        AccountId bank = account(owner, "Bank", "ASSET", "USD");
        AccountId cash = account(owner, "Cash", "ASSET", "USD");
        AccountId shop = account(owner, "Shop", "EXPENSE", "USD");
        LabelId food = label(owner, "food", null);

        spend(owner, bank, cash, "200.00", USD, null);      // moving your own money
        spend(owner, bank, shop, "15.00", USD, food);

        List<LabelAmountView> rows = reports.spendingByLabel(owner, null, null);
        BigDecimal sumOwn = rows.stream().filter(r -> r.currency().equals("USD"))
                .map(LabelAmountView::own).reduce(BigDecimal.ZERO, BigDecimal::add);

        // The £200 is absent by construction -- it touches no Expense account, so no filter was needed
        // and no Uncategorized row appears for it either (ADR-0013).
        assertEquals(amount("15.00"), sumOwn);
        assertTrue(rows.stream().noneMatch(r -> "Uncategorized".equals(r.path())));
    }

    @Test
    void income_is_labelled_the_same_way_as_spending() {
        OwnerId owner = OwnerId.of(UUID.randomUUID());
        AccountId salary = account(owner, "Salary", "INCOME", "USD");
        AccountId bank = account(owner, "Bank", "ASSET", "USD");
        LabelId pay = label(owner, "salary", null);

        spend(owner, salary, bank, "2000.00", USD, pay);

        assertEquals(amount("2000.00"), byPath(reports.incomeByLabel(owner, null, null), "USD")
                .get("salary").own());
    }

    @Test
    void one_owners_labels_never_appear_in_anothers_report() {
        OwnerId a = OwnerId.of(UUID.randomUUID());
        OwnerId b = OwnerId.of(UUID.randomUUID());
        AccountId bank = account(a, "Bank", "ASSET", "USD");
        AccountId shop = account(a, "Shop", "EXPENSE", "USD");
        spend(a, bank, shop, "10.00", USD, label(a, "food", null));

        assertTrue(reports.spendingByLabel(b, null, null).isEmpty());
    }

    private BigDecimal balanceOf(OwnerId owner, AccountId account) {
        return accountQuery.findByOwner(owner).stream()
                .filter(view -> view.id().equals(account.value()))
                .map(AccountView::balance)
                .findFirst()
                .orElseThrow();
    }

    /** The posting on the given expense account — what a retag addresses. */
    private PostingId expenseLegOf(OwnerId owner, AccountId expenseAccount) {
        return transactionQuery.findByOwner(owner, expenseAccount.value()).stream()
                .map(TransactionView::postings)
                .flatMap(List::stream)
                .filter(posting -> posting.accountId().equals(expenseAccount.value()))
                .map(PostingView::id)
                .map(PostingId::of)
                .findFirst()
                .orElseThrow();
    }
}
