package com.lifetracker.domain.account;

import java.util.Currency;
import java.util.Objects;

/**
 * A place a balance lives (ADR-0003): a bank, a card, a friend's IOU. It has a {@link AccountKind}
 * (ADR-0001, fixing its debit/credit behaviour) and a fixed {@link Currency} that every posting to it
 * must match (ADR-0002). It holds NO owner — ownership is enforced around the Ledger, never inside it
 * (CONTEXT-MAP, ADR-0006). Identity is the {@link AccountId}. The balance is not here either: it is
 * computed from postings on demand (ADR-0004).
 */
public final class Account {

    private final AccountId id;
    private final AccountName name;
    private final AccountKind kind;
    private final Currency currency;

    private Account(AccountId id, AccountName name, AccountKind kind, Currency currency) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = Objects.requireNonNull(name, "name");
        this.kind = Objects.requireNonNull(kind, "kind");
        this.currency = Objects.requireNonNull(currency, "currency");
    }

    /** Open a new account. */
    public static Account open(AccountId id, AccountName name, AccountKind kind, Currency currency) {
        return new Account(id, name, kind, currency);
    }

    /** Reconstitute from storage. For the persistence adapter, not business code. */
    public static Account rehydrate(AccountId id, AccountName name, AccountKind kind, Currency currency) {
        return new Account(id, name, kind, currency);
    }

    public AccountId id() {
        return id;
    }

    public AccountName name() {
        return name;
    }

    public AccountKind kind() {
        return kind;
    }

    public Currency currency() {
        return currency;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        return o instanceof Account other && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Account[" + id + ", " + name + ", " + kind + " " + currency.getCurrencyCode() + "]";
    }
}
