package com.lifetracker.domain.transaction;

import com.lifetracker.domain.ledger.EntrySide;
import com.lifetracker.domain.money.Money;

import java.time.LocalDate;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A balanced set of postings on a single date (ADR-0012). The core invariant lives here: within each
 * currency, debits equal credits (ADR-0002). Identity is the {@link TransactionId}; it holds no owner
 * field (ADR-0006). Balances are not stored on it — they are computed from postings (ADR-0004).
 *
 * <p>A currency that appears on only one side is left to a later slice's cross-currency recording (two
 * real amounts, a derived rate — ADR-0002); it is not checked for equality here. In this slice the use
 * case refuses cross-currency movements, so every transaction reaching the aggregate is single-currency.
 */
public final class Transaction {

    private final TransactionId id;
    private final LocalDate date;
    private final List<Posting> postings;

    private Transaction(TransactionId id, LocalDate date, List<Posting> postings) {
        this.id = Objects.requireNonNull(id, "id");
        this.date = Objects.requireNonNull(date, "date");
        Objects.requireNonNull(postings, "postings");
        if (postings.size() < 2) {
            throw new UnbalancedTransactionException("a transaction needs at least two postings");
        }
        requireBalancedPerCurrency(postings);
        this.postings = List.copyOf(postings);
    }

    /** Record a new transaction, enforcing the balance invariant. */
    public static Transaction record(TransactionId id, LocalDate date, List<Posting> postings) {
        return new Transaction(id, date, postings);
    }

    /** Reconstitute from storage. For the persistence adapter, not business code. */
    public static Transaction rehydrate(TransactionId id, LocalDate date, List<Posting> postings) {
        return new Transaction(id, date, postings);
    }

    private static void requireBalancedPerCurrency(List<Posting> postings) {
        Map<Currency, Money> debits = new HashMap<>();
        Map<Currency, Money> credits = new HashMap<>();
        for (Posting posting : postings) {
            Map<Currency, Money> side = posting.side() == EntrySide.DEBIT ? debits : credits;
            side.merge(posting.amount().currency(), posting.amount(), Money::plus);
        }
        for (Map.Entry<Currency, Money> entry : debits.entrySet()) {
            Money credit = credits.get(entry.getKey());
            if (credit != null && !entry.getValue().equals(credit)) {
                throw new UnbalancedTransactionException(
                        "debits (" + entry.getValue().amount() + ") do not equal credits (" + credit.amount()
                                + ") in " + entry.getKey().getCurrencyCode());
            }
        }
    }

    public TransactionId id() {
        return id;
    }

    public LocalDate date() {
        return date;
    }

    public List<Posting> postings() {
        return postings;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        return o instanceof Transaction other && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Transaction[" + id + ", " + date + ", " + postings.size() + " postings]";
    }
}
