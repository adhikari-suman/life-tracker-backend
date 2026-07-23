package com.lifetracker.infrastructure.web.dto;

import java.util.UUID;

/**
 * Wire response for one posting — its id, the account, its direction (DEBIT/CREDIT), the amount, and
 * the label attached to it if any.
 *
 * <p>The id is what {@code /postings/{postingId}/label} addresses: a label is attached to a posting
 * from outside the ledger core rather than stored on it (ADR-0014).
 */
public record PostingResponse(UUID id, UUID accountId, String direction, MoneyDto amount, UUID labelId) {
}
