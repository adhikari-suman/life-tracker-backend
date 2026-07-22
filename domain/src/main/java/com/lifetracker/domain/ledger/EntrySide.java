package com.lifetracker.domain.ledger;

/**
 * A posting is a debit or a credit. An {@link com.lifetracker.domain.account.AccountKind}'s "normal
 * side" is the one its balance grows on (ADR-0001).
 */
public enum EntrySide {
    DEBIT,
    CREDIT;

    public EntrySide opposite() {
        return this == DEBIT ? CREDIT : DEBIT;
    }
}
