package com.lifetracker.infrastructure.web.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Wire response for a transaction — its postings (the ledger truth) and the derived rate (nullable). */
public record TransactionResponse(UUID id, LocalDate date, String exchangeRate, List<PostingResponse> postings) {
}
