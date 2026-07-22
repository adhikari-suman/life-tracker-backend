package com.lifetracker.infrastructure.persistence.transaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** A flat read view of a transaction — its date, the derived cross-currency rate (nullable), and its postings. */
public record TransactionView(UUID id, LocalDate date, BigDecimal exchangeRate, List<PostingView> postings) {
}
