package com.lifetracker.infrastructure.persistence.report;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * One label's share of a report, in one currency. A label with postings in several currencies appears
 * once per currency, because valuing across currencies is deliberately out of scope (ADR-0013).
 *
 * <p>Two figures, and confusing them is the likeliest bug in the whole feature:
 * <ul>
 *   <li>{@code own} — only postings tagged with THIS label. These are what sum to the report total.
 *   <li>{@code rolledUp} — {@code own} plus every descendant's. NEVER sum this across rows: a child's
 *       amount appears in its own row and again in each ancestor's, so adding them double-counts.
 *       The failure is silent — the number simply comes out too big.
 * </ul>
 *
 * <p>{@code labelId} is null on the single Uncategorized row, which carries the postings that have no
 * label. It is not a label anyone created; it is the name for the remainder, and it exists so the
 * breakdown always accounts for every penny in the report.
 */
public record LabelAmountView(UUID labelId, String name, String path, UUID parentLabelId,
                              String currency, BigDecimal own, BigDecimal rolledUp) {
}
