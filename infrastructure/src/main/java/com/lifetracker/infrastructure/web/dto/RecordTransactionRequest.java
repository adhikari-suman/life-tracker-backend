package com.lifetracker.infrastructure.web.dto;

import java.time.LocalDate;
import java.util.UUID;

/** Body of {@code POST /transactions}: a movement — money out of {@code from}, into {@code to}. */
public record RecordTransactionRequest(LocalDate date, UUID from, UUID to, MoneyDto amount) {
}
