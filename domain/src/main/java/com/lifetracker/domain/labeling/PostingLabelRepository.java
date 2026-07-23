package com.lifetracker.domain.labeling;

import com.lifetracker.domain.ledger.OwnerId;
import com.lifetracker.domain.transaction.PostingId;

import java.util.Optional;

/**
 * The attachment of labels to postings — the metadata satellite that sits outside the ledger core
 * (ADR-0014). A posting carries at most one label, so assigning replaces whatever was there.
 *
 * <p>Nothing here rewrites a posting: the ledger stays append-only, and retagging is a metadata edit
 * that leaves every balance, net-worth and per-account figure byte-identical.
 */
public interface PostingLabelRepository {

    void assign(OwnerId owner, PostingId posting, LabelId label);

    void clear(OwnerId owner, PostingId posting);

    Optional<LabelId> findByPosting(OwnerId owner, PostingId posting);

    /** Whether any posting is still tagged with this label — what blocks a delete (ADR-0015). */
    boolean isInUse(OwnerId owner, LabelId label);
}
