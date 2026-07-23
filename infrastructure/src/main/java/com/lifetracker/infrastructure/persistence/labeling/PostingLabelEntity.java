package com.lifetracker.infrastructure.persistence.labeling;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * The {@code posting_labels} table (code-first, ADR-0009) — the metadata satellite that attaches a
 * label to a posting from outside the ledger core (ADR-0014).
 *
 * <p>{@code posting_id} is the PRIMARY KEY, not merely a foreign key: that is what makes "a posting
 * carries at most one label" structural rather than a rule someone has to remember. Package-private.
 */
@Entity
@Table(name = "posting_labels")
class PostingLabelEntity {

    @Id
    @Column(name = "posting_id", nullable = false, updatable = false)
    private UUID postingId;

    @Column(name = "label_id", nullable = false)
    private UUID labelId;

    // Denormalized from the posting's account so the ADR-0006 read guard can filter directly.
    @Column(name = "owner_id", nullable = false, updatable = false)
    private UUID ownerId;

    protected PostingLabelEntity() {
    }

    PostingLabelEntity(UUID postingId, UUID labelId, UUID ownerId) {
        this.postingId = postingId;
        this.labelId = labelId;
        this.ownerId = ownerId;
    }

    UUID getPostingId() {
        return postingId;
    }

    UUID getLabelId() {
        return labelId;
    }

    UUID getOwnerId() {
        return ownerId;
    }
}
