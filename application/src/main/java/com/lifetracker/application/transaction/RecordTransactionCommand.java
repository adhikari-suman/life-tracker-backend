package com.lifetracker.application.transaction;

import com.lifetracker.domain.account.AccountId;
import com.lifetracker.domain.ledger.OwnerId;
import com.lifetracker.domain.money.Money;

import java.time.LocalDate;

/** Input to {@link RecordTransaction}: the owner, date, the two accounts, and the amount moved. */
public record RecordTransactionCommand(OwnerId owner, LocalDate date, AccountId from, AccountId to, Money amount) {
}
