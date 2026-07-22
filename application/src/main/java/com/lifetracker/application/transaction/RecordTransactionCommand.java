package com.lifetracker.application.transaction;

import com.lifetracker.domain.account.AccountId;
import com.lifetracker.domain.ledger.OwnerId;
import com.lifetracker.domain.money.Money;

import java.time.LocalDate;

/**
 * Input to {@link RecordTransaction}: the owner, date, the two accounts, the amount leaving {@code
 * from}, and — for a cross-currency movement — the amount arriving in {@code to} ({@code toAmount},
 * null when the accounts share a currency).
 */
public record RecordTransactionCommand(OwnerId owner, LocalDate date, AccountId from, AccountId to,
                                       Money amount, Money toAmount) {
}
