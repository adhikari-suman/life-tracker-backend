package com.lifetracker.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;

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
 *
 * <p>The five required fields carry {@code @NotNull} so an omitted one is answered with 422
 * VALIDATION rather than reaching the use case as a null. The spec's {@code required} list is
 * [date, time, from, to, amount]; {@code toAmount} and {@code labelId} are genuinely optional.
 *
 * <p>{@code time} is pinned to {@code HH:mm} rather than left to Jackson's ISO default, which
 * accepts and emits seconds. The spec constrains it with
 * {@code pattern: ^([01][0-9]|2[0-3]):[0-5][0-9]$} in BOTH directions, so a trailing {@code :00}
 * is off-contract even though it parses cleanly.
 */
public record RecordTransactionRequest(@NotNull LocalDate date,
                                       @NotNull @JsonFormat(pattern = "HH:mm") LocalTime time,
                                       @NotNull UUID from, @NotNull UUID to,
                                       @NotNull MoneyDto amount, MoneyDto toAmount, UUID labelId) {
}
