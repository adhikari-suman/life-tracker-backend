package com.lifetracker.application.labeling;

import com.lifetracker.domain.labeling.PostingKinds;
import com.lifetracker.domain.labeling.PostingLabelRepository;
import com.lifetracker.domain.ledger.OwnerId;
import com.lifetracker.domain.transaction.PostingId;

/**
 * Removes a posting's label. The posting becomes uncategorized: its amount moves into the
 * Uncategorized row of a label breakdown, never out of the totals.
 *
 * <p>Clearing a posting that has no label is not an error — the end state is what was asked for.
 */
public final class ClearPostingLabel {

    private final PostingLabelRepository postingLabels;
    private final PostingKinds postingKinds;

    public ClearPostingLabel(PostingLabelRepository postingLabels, PostingKinds postingKinds) {
        this.postingLabels = postingLabels;
        this.postingKinds = postingKinds;
    }

    public void execute(OwnerId owner, PostingId posting) {
        postingKinds.kindOf(owner, posting).orElseThrow(PostingNotFoundException::new);
        postingLabels.clear(owner, posting);
    }
}
