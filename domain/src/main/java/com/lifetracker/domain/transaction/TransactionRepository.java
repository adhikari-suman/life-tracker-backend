package com.lifetracker.domain.transaction;

import com.lifetracker.domain.ledger.OwnerId;

/**
 * The store of transactions, owner-scoped (the owner is passed in, never held on the aggregate —
 * ADR-0006). Write side only: listing transactions and reading one for a screen are query-service
 * concerns, since reads return flat views and do not load aggregates.
 */
public interface TransactionRepository {

    void save(OwnerId owner, Transaction transaction);
}
