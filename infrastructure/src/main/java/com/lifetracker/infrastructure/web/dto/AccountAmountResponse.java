package com.lifetracker.infrastructure.web.dto;

import java.util.UUID;

/** One account's total in a report. */
public record AccountAmountResponse(UUID accountId, String name, String currency, MoneyDto amount) {
}
