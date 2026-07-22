package com.lifetracker.application.transaction;

import com.lifetracker.domain.ledger.OwnerId;
import com.lifetracker.domain.transaction.Transaction;
import com.lifetracker.domain.transaction.TransactionRepository;

import java.util.ArrayList;
import java.util.List;

/** In-memory {@link TransactionRepository} fake — records what was saved, for assertions. */
final class InMemoryTransactionRepository implements TransactionRepository {

    final List<Transaction> saved = new ArrayList<>();

    @Override
    public void save(OwnerId owner, Transaction transaction) {
        saved.add(transaction);
    }
}
