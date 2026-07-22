package com.lifetracker.infrastructure.web.dto;

/** A per-currency total in a report. */
public record CurrencyAmountResponse(String currency, MoneyDto amount) {
}
