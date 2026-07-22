package com.lifetracker.infrastructure.web.dto;

/** Net worth for one currency: total Assets, total Liabilities, and their difference. */
public record CurrencyNetWorthResponse(String currency, MoneyDto assets, MoneyDto liabilities, MoneyDto netWorth) {
}
