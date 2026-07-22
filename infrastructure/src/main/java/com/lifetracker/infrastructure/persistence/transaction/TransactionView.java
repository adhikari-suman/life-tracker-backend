package com.lifetracker.infrastructure.persistence.transaction;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** A flat read view of a transaction — its date and its balanced postings (the ledger truth). */
public record TransactionView(UUID id, LocalDate date, List<PostingView> postings) {
}
