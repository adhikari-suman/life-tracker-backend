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

    /**
     * Whether this kind sits on the boundary of your world — where money genuinely enters or leaves.
     * Income and Expense do; Asset, Liability and Equity are all things you hold, so moving between
     * them is neither earning nor spending.
     *
     * <p>This is what makes a transfer P&amp;L-neutral by construction (ADR-0001), and it is also the
     * test for whether a posting can carry a label at all: only a boundary posting has a "what was
     * this for" to answer (ADR-0014).
     */
    public boolean isBoundary() {
        return this == INCOME || this == EXPENSE;
    }
}
