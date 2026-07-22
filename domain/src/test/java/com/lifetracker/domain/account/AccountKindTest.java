package com.lifetracker.domain.account;

import com.lifetracker.domain.ledger.EntrySide;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AccountKindTest {

    @Test
    void asset_and_expense_grow_on_debit() {
        assertEquals(EntrySide.DEBIT, AccountKind.ASSET.normalSide());
        assertEquals(EntrySide.DEBIT, AccountKind.EXPENSE.normalSide());
    }

    @Test
    void liability_income_and_equity_grow_on_credit() {
        assertEquals(EntrySide.CREDIT, AccountKind.LIABILITY.normalSide());
        assertEquals(EntrySide.CREDIT, AccountKind.INCOME.normalSide());
        assertEquals(EntrySide.CREDIT, AccountKind.EQUITY.normalSide());
    }
}
