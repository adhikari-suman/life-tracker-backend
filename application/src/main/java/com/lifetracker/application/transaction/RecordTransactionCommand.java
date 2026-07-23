package com.lifetracker.application.transaction;

import com.lifetracker.domain.account.AccountId;
import com.lifetracker.domain.ledger.OwnerId;
import com.lifetracker.domain.money.Money;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Input to {@link RecordTransaction}: the owner, date, the two accounts, the amount leaving {@code
 * from}, and — for a cross-currency movement — the amount arriving in {@code to} ({@code toAmount},
 * null when the accounts share a currency).
 *
 * <p>{@code labelId} is optional metadata (ADR-0014), and deliberately does not name a posting: the
 * use case attaches it to whichever leg is the Income or Expense account, since that is the only leg
 * with a "what was this for" to answer. Null leaves the transaction uncategorized — a valid state,
 * not missing data.
 */
public record RecordTransactionCommand(OwnerId owner, LocalDate date, AccountId from, AccountId to,
                                       Money amount, Money toAmount, UUID labelId) {
}
