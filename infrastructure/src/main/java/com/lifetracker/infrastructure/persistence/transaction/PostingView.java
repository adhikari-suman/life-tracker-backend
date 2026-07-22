package com.lifetracker.infrastructure.persistence.transaction;

import java.math.BigDecimal;
import java.util.UUID;

/** A flat read view of one posting — the account, its side (DEBIT/CREDIT), and the amount + currency. */
public record PostingView(UUID accountId, String side, BigDecimal amount, String currency) {
}
