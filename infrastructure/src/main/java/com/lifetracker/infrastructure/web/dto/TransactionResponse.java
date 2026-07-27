package com.lifetracker.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

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
 *
 * <p>{@code time} is pinned to {@code HH:mm}. Jackson's ISO default for a LocalTime emits
 * {@code 19:42:00}, which does not match the spec's
 * {@code pattern: ^([01][0-9]|2[0-3]):[0-5][0-9]$} — a generated client validating the response
 * would reject it, and the column stores no seconds worth preserving anyway.
 */
public record TransactionResponse(UUID id, LocalDate date,
                                  @JsonFormat(pattern = "HH:mm") LocalTime time, String exchangeRate,
                                  List<PostingResponse> postings) {
}
