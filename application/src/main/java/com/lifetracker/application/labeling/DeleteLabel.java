package com.lifetracker.application.labeling;

import com.lifetracker.domain.labeling.LabelHasChildrenException;
import com.lifetracker.domain.labeling.LabelId;
import com.lifetracker.domain.labeling.LabelInUseException;
import com.lifetracker.domain.labeling.LabelRepository;
import com.lifetracker.domain.labeling.LabelTree;
import com.lifetracker.domain.labeling.PostingLabelRepository;
import com.lifetracker.domain.ledger.OwnerId;

/**
 * Deletes a label, but only when nothing depends on it — no children, and no posting still tagged
 * (ADR-0015). Deleting is for tidying, not for destroying history: to retire a label that is in use,
 * archive it; to fold one into another, retag its postings first, deliberately.
 */
public final class DeleteLabel {

    private final LabelRepository labels;
    private final PostingLabelRepository postingLabels;

    public DeleteLabel(LabelRepository labels, PostingLabelRepository postingLabels) {
        this.labels = labels;
        this.postingLabels = postingLabels;
    }

    public void execute(OwnerId owner, LabelId id) {
        labels.findById(owner, id).orElseThrow(LabelNotFoundException::new);
        if (new LabelTree(labels.findAllByOwner(owner)).hasChildren(id)) {
            throw new LabelHasChildrenException();
        }
        if (postingLabels.isInUse(owner, id)) {
            throw new LabelInUseException();
        }
        labels.delete(owner, id);
    }
}
