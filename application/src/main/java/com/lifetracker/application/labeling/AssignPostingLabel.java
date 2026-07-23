package com.lifetracker.application.labeling;

import com.lifetracker.domain.account.AccountKind;
import com.lifetracker.domain.labeling.Label;
import com.lifetracker.domain.labeling.LabelId;
import com.lifetracker.domain.labeling.LabelRepository;
import com.lifetracker.domain.labeling.PostingKinds;
import com.lifetracker.domain.labeling.PostingLabelRepository;
import com.lifetracker.domain.ledger.OwnerId;
import com.lifetracker.domain.transaction.PostingId;

/**
 * Attaches a label to a posting, replacing whatever was there — a posting carries at most one.
 *
 * <p>This never rewrites the posting: the attachment is metadata beside the ledger core (ADR-0014), so
 * a posting can be retagged at any age and the ledger stays append-only. Retagging shifts the label
 * breakdown and leaves every balance, net-worth and per-account figure untouched.
 *
 * <p>Only a posting to an Income or Expense account can carry one; anything else is refused rather
 * than ignored, because it nearly always means the wrong account kind was picked.
 */
public final class AssignPostingLabel {

    private final LabelRepository labels;
    private final PostingLabelRepository postingLabels;
    private final PostingKinds postingKinds;

    public AssignPostingLabel(LabelRepository labels, PostingLabelRepository postingLabels, PostingKinds postingKinds) {
        this.labels = labels;
        this.postingLabels = postingLabels;
        this.postingKinds = postingKinds;
    }

    public void execute(OwnerId owner, PostingId posting, LabelId labelId) {
        AccountKind kind = postingKinds.kindOf(owner, posting).orElseThrow(PostingNotFoundException::new);
        if (!kind.isBoundary()) {
            throw new LabelNotApplicableException(
                    "this posting is to a " + kind + " account, which records money moving between accounts "
                            + "you hold -- there is nothing to categorize");
        }
        Label label = labels.findById(owner, labelId).orElseThrow(LabelNotFoundException::new);
        if (label.isArchived()) {
            throw new LabelArchivedException();
        }
        postingLabels.assign(owner, posting, labelId);
    }
}
