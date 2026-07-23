package com.lifetracker.domain.labeling;

import com.lifetracker.domain.account.AccountKind;
import com.lifetracker.domain.ledger.OwnerId;
import com.lifetracker.domain.transaction.PostingId;

import java.util.Optional;

/**
 * Resolves the kind of account a posting was made to. Labelling needs exactly this much of the ledger
 * and no more: only a posting to an Income or Expense account records money entering or leaving your
 * world, and so only such a posting can carry a label (ADR-0014).
 *
 * <p>A port shaped for its consumer rather than for its storage — it exists so the labelling rules can
 * ask their one question without the ledger core learning that labels exist.
 */
public interface PostingKinds {

    /** The kind of the account this posting is to, or empty if the Book holds no such posting. */
    Optional<AccountKind> kindOf(OwnerId owner, PostingId posting);
}
