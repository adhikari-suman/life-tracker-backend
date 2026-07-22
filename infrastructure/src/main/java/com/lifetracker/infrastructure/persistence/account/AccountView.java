package com.lifetracker.infrastructure.persistence.account;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * A flat read view of an account, with its balance computed from postings (ADR-0004). The balance is
 * signed — negative for an overdrawn Asset, a Liability's owed amount is positive, etc.
 */
public record AccountView(UUID id, String name, String kind, String currency, BigDecimal balance) {
}
