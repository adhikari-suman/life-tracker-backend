package com.lifetracker.infrastructure.web.dto;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Body of {@code POST /transactions}: a movement — money out of {@code from}, into {@code to}.
 * {@code toAmount} is the destination-leg amount for a cross-currency movement (null otherwise).
 */
public record RecordTransactionRequest(LocalDate date, UUID from, UUID to, MoneyDto amount, MoneyDto toAmount) {
}
