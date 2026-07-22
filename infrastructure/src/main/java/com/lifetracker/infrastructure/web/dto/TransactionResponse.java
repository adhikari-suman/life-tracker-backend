package com.lifetracker.infrastructure.web.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Wire response for a transaction, as its balanced postings (the ledger truth). */
public record TransactionResponse(UUID id, LocalDate date, List<PostingResponse> postings) {
}
