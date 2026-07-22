package com.lifetracker.infrastructure.web.dto;

import java.time.LocalDate;
import java.util.List;

/** Wire response for spending or income over a range — per account, and totalled per currency. */
public record ActivityReportResponse(LocalDate from, LocalDate to,
                                     List<AccountAmountResponse> byAccount, List<CurrencyAmountResponse> totals) {
}
