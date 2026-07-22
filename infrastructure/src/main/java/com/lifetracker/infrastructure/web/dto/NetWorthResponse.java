package com.lifetracker.infrastructure.web.dto;

import java.util.List;

/** Wire response for {@code GET /reports/net-worth}: net worth broken down per currency. */
public record NetWorthResponse(List<CurrencyNetWorthResponse> byCurrency) {
}
