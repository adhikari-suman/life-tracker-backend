package com.lifetracker.infrastructure.web.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Body of {@code POST /transactions}: a movement — money out of {@code from}, into {@code to}.
 * {@code toAmount} is the destination-leg amount for a cross-currency movement (null otherwise).
 *
 * <p>{@code date} and {@code time} are <em>Occurred At</em> (ADR-0018) — a wall-clock reading the
 * client supplies, zoneless, never converted. {@code time} is required for the same reason it has
 * no default: this server runs in UTC and cannot know what the clock on the caller's wall said.
 * Jackson parses the spec's `HH:mm` into a LocalTime, which carries no offset by construction.
 *
 * <p>{@code labelId} is optional and names no posting: the server attaches it to whichever leg is the
 * Income or Expense account, since that is the only leg with a "what was this for" to answer
 * (ADR-0014).
 */
public record RecordTransactionRequest(LocalDate date, LocalTime time, UUID from, UUID to,
                                       MoneyDto amount, MoneyDto toAmount, UUID labelId) {
}
