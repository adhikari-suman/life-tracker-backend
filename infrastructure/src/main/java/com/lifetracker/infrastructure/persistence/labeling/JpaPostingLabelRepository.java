package com.lifetracker.infrastructure.persistence.labeling;

import com.lifetracker.domain.labeling.LabelId;
import com.lifetracker.domain.labeling.PostingLabelRepository;
import com.lifetracker.domain.ledger.OwnerId;
import com.lifetracker.domain.transaction.PostingId;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * The {@link PostingLabelRepository} port. Writes only the attachment row — never a posting — so the
 * ledger stays append-only however often a posting is retagged (ADR-0014).
 */
@Repository
class JpaPostingLabelRepository implements PostingLabelRepository {

    private final PostingLabelJpaData data;

    JpaPostingLabelRepository(PostingLabelJpaData data) {
        this.data = data;
    }

    @Override
    @Transactional
    public void assign(OwnerId owner, PostingId posting, LabelId label) {
        // posting_id is the primary key, so saving replaces any existing attachment: one label per
        // posting, enforced by the table rather than by remembering to delete first.
        data.save(new PostingLabelEntity(posting.value(), label.value(), owner.value()));
    }

    @Override
    @Transactional
    public void clear(OwnerId owner, PostingId posting) {
        data.deleteByOwnerIdAndPostingId(owner.value(), posting.value());
    }

    @Override
    public Optional<LabelId> findByPosting(OwnerId owner, PostingId posting) {
        return data.findByOwnerIdAndPostingId(owner.value(), posting.value())
                .map(entity -> LabelId.of(entity.getLabelId()));
    }

    @Override
    public boolean isInUse(OwnerId owner, LabelId label) {
        return data.existsByOwnerIdAndLabelId(owner.value(), label.value());
    }
}
