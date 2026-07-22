package com.lifetracker.application.account;

import com.lifetracker.domain.ledger.OwnerId;

/** Input to {@link OpenAccount}: the owner and the account's name, kind, and currency (wire strings). */
public record OpenAccountCommand(OwnerId owner, String name, String kind, String currency) {
}
