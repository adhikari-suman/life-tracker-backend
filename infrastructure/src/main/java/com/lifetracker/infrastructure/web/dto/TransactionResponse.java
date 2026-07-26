package com.lifetracker.infrastructure.web.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * Wire response for a transaction — its postings (the ledger truth) and the derived rate (nullable).
 *
 * <p>{@code date} and {@code time} are the wall clock the money moved on, zoneless (ADR-0018). The
 * instant it was recorded is deliberately not on the wire: it is an audit fact, not an answer to
 * "when did this happen".
 */
public record TransactionResponse(UUID id, LocalDate date, LocalTime time, String exchangeRate,
                                  List<PostingResponse> postings) {
}
