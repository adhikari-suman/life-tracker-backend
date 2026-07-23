package com.lifetracker.infrastructure.web.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Wire response for spending or income over a range — the same money sliced two ways, by account
 * (where it was booked) and by label (what it was for), and totalled per currency.
 *
 * <p>The two breakdowns reconcile: summing every {@code byLabel} row's {@code own}, including the
 * Uncategorized row, gives the same per-currency figure as {@code byAccount} and as {@code totals}.
 */
public record ActivityReportResponse(LocalDate from, LocalDate to,
                                     List<AccountAmountResponse> byAccount,
                                     List<LabelAmountResponse> byLabel,
                                     List<CurrencyAmountResponse> totals) {
}
