package com.lifetracker.infrastructure.persistence.transaction;

import com.lifetracker.domain.ledger.OwnerId;
import com.lifetracker.domain.transaction.TransactionId;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Read side for transactions, newest first, optionally filtered to those touching one account.
 * Owner-scoped ({@link OwnerId}) — the ADR-0006 read guard. Returns flat views (the balanced postings),
 * never the aggregate.
 */
@Component
public class TransactionQueryService {

    private final TransactionJpaData transactions;
    private final PostingJpaData postings;

    TransactionQueryService(TransactionJpaData transactions, PostingJpaData postings) {
        this.transactions = transactions;
        this.postings = postings;
    }

    public List<TransactionView> findByOwner(OwnerId owner, UUID accountFilter) {
        List<TransactionEntity> headers = accountFilter == null
                ? transactions.findByOwnerIdOrderByDateDescCreatedAtDesc(owner.value())
                : transactions.findByOwnerAndAccount(owner.value(), accountFilter);
        if (headers.isEmpty()) {
            return List.of();
        }
        List<UUID> ids = headers.stream().map(TransactionEntity::getId).toList();
        Map<UUID, List<PostingEntity>> byTransaction = postings.findByTransactionIdIn(ids).stream()
                .collect(Collectors.groupingBy(PostingEntity::getTransactionId));
        return headers.stream().map(header -> toView(header, byTransaction.getOrDefault(header.getId(), List.of()))).toList();
    }

    public Optional<TransactionView> findById(OwnerId owner, TransactionId id) {
        return transactions.findByOwnerIdAndId(owner.value(), id.value())
                .map(header -> toView(header, postings.findByTransactionIdIn(List.of(header.getId()))));
    }

    private TransactionView toView(TransactionEntity header, List<PostingEntity> postingRows) {
        List<PostingView> views = postingRows.stream()
                .map(p -> new PostingView(p.getAccountId(), p.getSide(), p.getAmount(), p.getCurrency()))
                .toList();
        return new TransactionView(header.getId(), header.getDate(), views);
    }
}
