package com.lifetracker.infrastructure.persistence.transaction;

import com.lifetracker.domain.ledger.OwnerId;
import com.lifetracker.domain.transaction.Transaction;

import java.util.List;
import java.util.UUID;

/** Converts the domain {@link Transaction} (no owner) into its owner-stamped header + posting rows. */
final class TransactionMapper {

    private TransactionMapper() {
    }

    static TransactionEntity toEntity(OwnerId owner, Transaction transaction) {
        return new TransactionEntity(transaction.id().value(), owner.value(), transaction.date());
    }

    static List<PostingEntity> toPostingEntities(Transaction transaction) {
        return transaction.postings().stream()
                .map(posting -> new PostingEntity(
                        UUID.randomUUID(),
                        transaction.id().value(),
                        posting.accountId().value(),
                        posting.side().name(),
                        posting.amount().amount(),
                        posting.amount().currency().getCurrencyCode()))
                .toList();
    }
}
