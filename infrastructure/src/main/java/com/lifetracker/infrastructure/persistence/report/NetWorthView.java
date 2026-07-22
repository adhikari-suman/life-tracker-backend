package com.lifetracker.infrastructure.persistence.report;

import java.math.BigDecimal;

/** Net worth for one currency: total Assets, total Liabilities, and their difference (signed). */
public record NetWorthView(String currency, BigDecimal assets, BigDecimal liabilities, BigDecimal netWorth) {
}
