package com.lifetracker.infrastructure.persistence.transaction;

import com.lifetracker.domain.ledger.OwnerId;
import com.lifetracker.domain.transaction.Transaction;
import com.lifetracker.domain.transaction.TransactionRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * The {@link TransactionRepository} port. Saves the transaction header and its postings in one
 * transaction, owner-scoped. Reads (list, get) are the {@link TransactionQueryService}'s job.
 */
@Repository
class JpaTransactionRepository implements TransactionRepository {

    private final TransactionJpaData transactions;
    private final PostingJpaData postings;

    JpaTransactionRepository(TransactionJpaData transactions, PostingJpaData postings) {
        this.transactions = transactions;
        this.postings = postings;
    }

    @Override
    @Transactional
    public void save(OwnerId owner, Transaction transaction) {
        transactions.save(TransactionMapper.toEntity(owner, transaction));
        postings.saveAll(TransactionMapper.toPostingEntities(transaction));
    }
}
