package com.lifetracker.domain.account;

import com.lifetracker.domain.ledger.EntrySide;

/**
 * The five account kinds (ADR-0001). The kind fixes the <em>normal side</em> — the side its balance
 * grows on — which is what makes internal transfers correct by construction: a transfer touches only
 * Asset accounts, so it is neither income nor spending without anyone saying so. Asset and Expense
 * grow on debit; Liability, Income, and Equity grow on credit.
 */
public enum AccountKind {
    ASSET(EntrySide.DEBIT),
    LIABILITY(EntrySide.CREDIT),
    INCOME(EntrySide.CREDIT),
    EXPENSE(EntrySide.DEBIT),
    EQUITY(EntrySide.CREDIT);

    private final EntrySide normalSide;

    AccountKind(EntrySide normalSide) {
        this.normalSide = normalSide;
    }

    /** The side this kind's balance grows on. */
    public EntrySide normalSide() {
        return normalSide;
    }
}
