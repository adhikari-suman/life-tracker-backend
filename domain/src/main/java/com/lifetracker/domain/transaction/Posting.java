package com.lifetracker.domain.transaction;

import com.lifetracker.domain.account.AccountId;
import com.lifetracker.domain.ledger.EntrySide;
import com.lifetracker.domain.money.Money;

import java.util.Objects;

/**
 * One leg of a transaction: a debit or credit of a {@link Money} amount to an account. The amount is a
 * domain {@code Money}, so it is always non-negative — the {@link EntrySide} carries the direction,
 * never a sign on the number.
 */
public record Posting(AccountId accountId, EntrySide side, Money amount) {

    public Posting {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(side, "side");
        Objects.requireNonNull(amount, "amount");
    }

    public static Posting debit(AccountId accountId, Money amount) {
        return new Posting(accountId, EntrySide.DEBIT, amount);
    }

    public static Posting credit(AccountId accountId, Money amount) {
        return new Posting(accountId, EntrySide.CREDIT, amount);
    }
}
