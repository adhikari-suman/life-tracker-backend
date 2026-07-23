package com.lifetracker.domain.transaction;

import com.lifetracker.domain.account.AccountId;
import com.lifetracker.domain.ledger.EntrySide;
import com.lifetracker.domain.money.Money;

import java.util.Objects;

/**
 * One leg of a transaction: a debit or credit of a {@link Money} amount to an account. The amount is a
 * domain {@code Money}, so it is always non-negative — the {@link EntrySide} carries the direction,
 * never a sign on the number.
 *
 * <p>It carries a {@link PostingId} so that metadata can be attached to this leg from outside the
 * ledger core (ADR-0014): a label is keyed by that id and is deliberately NOT a field here. The core
 * records money movement and nothing descriptive (ADR-0003), so a posting never learns what it was
 * for — only which account it moved money to, and by how much.
 */
public record Posting(PostingId id, AccountId accountId, EntrySide side, Money amount) {

    public Posting {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(side, "side");
        Objects.requireNonNull(amount, "amount");
    }

    public static Posting debit(AccountId accountId, Money amount) {
        return new Posting(PostingId.generate(), accountId, EntrySide.DEBIT, amount);
    }

    public static Posting credit(AccountId accountId, Money amount) {
        return new Posting(PostingId.generate(), accountId, EntrySide.CREDIT, amount);
    }
}
