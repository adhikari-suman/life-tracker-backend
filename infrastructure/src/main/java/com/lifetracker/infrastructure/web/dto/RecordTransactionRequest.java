package com.lifetracker.infrastructure.web.dto;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Body of {@code POST /transactions}: a movement — money out of {@code from}, into {@code to}.
 * {@code toAmount} is the destination-leg amount for a cross-currency movement (null otherwise).
 *
 * <p>{@code labelId} is optional and names no posting: the server attaches it to whichever leg is the
 * Income or Expense account, since that is the only leg with a "what was this for" to answer
 * (ADR-0014).
 */
public record RecordTransactionRequest(LocalDate date, UUID from, UUID to, MoneyDto amount, MoneyDto toAmount,
                                       UUID labelId) {
}
