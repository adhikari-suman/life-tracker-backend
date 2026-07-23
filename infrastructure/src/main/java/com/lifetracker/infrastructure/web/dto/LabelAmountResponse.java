package com.lifetracker.infrastructure.web.dto;

import java.util.UUID;

/**
 * One label's total in a report, in one currency.
 *
 * <p>{@code own} counts only postings tagged with this label and is what sums to the report total;
 * {@code rolledUp} adds every descendant's and must never be summed across rows, since a child's
 * amount appears both in its own row and in each ancestor's. {@code labelId} is null on the
 * Uncategorized row.
 */
public record LabelAmountResponse(UUID labelId, String name, String path, UUID parentLabelId,
                                  String currency, MoneyDto own, MoneyDto rolledUp) {
}
