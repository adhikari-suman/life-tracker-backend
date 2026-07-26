package com.lifetracker.infrastructure.persistence.transaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * A flat read view of a transaction — when it happened, the derived cross-currency rate (nullable),
 * and its postings. Date and time are the wall clock the money moved on, zoneless (ADR-0018).
 */
public record TransactionView(UUID id, LocalDate date, LocalTime time, BigDecimal exchangeRate,
                              List<PostingView> postings) {
}
