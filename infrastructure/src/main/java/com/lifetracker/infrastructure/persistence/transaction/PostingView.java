package com.lifetracker.infrastructure.persistence.transaction;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * A flat read view of one posting — its id, the account, its side (DEBIT/CREDIT), the amount +
 * currency, and the label attached to it if any.
 *
 * <p>{@code labelId} is null both when the posting cannot carry a label (it is not to an Income or
 * Expense account) and when it simply has not been categorized. The second is a valid state rather
 * than missing data — those postings are exactly what the Uncategorized row of a report accounts for.
 */
public record PostingView(UUID id, UUID accountId, String side, BigDecimal amount, String currency, UUID labelId) {
}
