package com.lifetracker.infrastructure.persistence.report;

import java.math.BigDecimal;
import java.util.UUID;

/** One account's total in a report — the account, its currency, and the summed amount. */
public record AccountAmountView(UUID accountId, String name, String currency, BigDecimal amount) {
}
