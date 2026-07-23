package com.lifetracker.infrastructure.persistence.transaction;

import com.lifetracker.domain.ledger.OwnerId;
import com.lifetracker.domain.transaction.TransactionId;
import com.lifetracker.infrastructure.persistence.labeling.PostingLabelQueryService;
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
 *
 * <p>Each posting's label is looked up alongside, in one batch rather than one query per posting. The
 * label lives in its own table beside the ledger (ADR-0014), so it is joined in on the read path and
 * never stored on the posting itself.
 */
@Component
public class TransactionQueryService {

    private final TransactionJpaData transactions;
    private final PostingJpaData postings;
    private final PostingLabelQueryService postingLabels;

    TransactionQueryService(TransactionJpaData transactions, PostingJpaData postings,
                            PostingLabelQueryService postingLabels) {
        this.transactions = transactions;
        this.postings = postings;
        this.postingLabels = postingLabels;
    }

    public List<TransactionView> findByOwner(OwnerId owner, UUID accountFilter) {
        List<TransactionEntity> headers = accountFilter == null
                ? transactions.findByOwnerIdOrderByDateDescCreatedAtDesc(owner.value())
                : transactions.findByOwnerAndAccount(owner.value(), accountFilter);
        if (headers.isEmpty()) {
            return List.of();
        }
        List<UUID> ids = headers.stream().map(TransactionEntity::getId).toList();
        List<PostingEntity> allPostings = postings.findByTransactionIdIn(ids);
        Map<UUID, UUID> labels = labelsFor(owner, allPostings);
        Map<UUID, List<PostingEntity>> byTransaction = allPostings.stream()
                .collect(Collectors.groupingBy(PostingEntity::getTransactionId));
        return headers.stream()
                .map(header -> toView(header, byTransaction.getOrDefault(header.getId(), List.of()), labels))
                .toList();
    }

    public Optional<TransactionView> findById(OwnerId owner, TransactionId id) {
        return transactions.findByOwnerIdAndId(owner.value(), id.value())
                .map(header -> {
                    List<PostingEntity> rows = postings.findByTransactionIdIn(List.of(header.getId()));
                    return toView(header, rows, labelsFor(owner, rows));
                });
    }

    private Map<UUID, UUID> labelsFor(OwnerId owner, List<PostingEntity> rows) {
        return postingLabels.labelIdsByPosting(owner, rows.stream().map(PostingEntity::getId).toList());
    }

    private TransactionView toView(TransactionEntity header, List<PostingEntity> postingRows, Map<UUID, UUID> labels) {
        List<PostingView> views = postingRows.stream()
                .map(p -> new PostingView(p.getId(), p.getAccountId(), p.getSide(), p.getAmount(), p.getCurrency(),
                        labels.get(p.getId())))
                .toList();
        return new TransactionView(header.getId(), header.getDate(), header.getExchangeRate(), views);
    }
}
