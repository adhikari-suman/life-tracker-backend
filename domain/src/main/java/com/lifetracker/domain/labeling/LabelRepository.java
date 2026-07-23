package com.lifetracker.domain.labeling;

import com.lifetracker.domain.ledger.OwnerId;

import java.util.List;
import java.util.Optional;

/**
 * The store of labels, owner-scoped. The owner is passed in on every call and never held on the
 * aggregate — isolation lives around the Ledger (ADR-0006); the adapter stamps and filters by
 * {@code owner_id}.
 *
 * <p>{@link #findAllByOwner} returns the Book's whole tree because the rules that matter (depth,
 * cycles, sibling names) span more than one label and are decided in {@link LabelTree}. A personal
 * Book holds dozens of labels, so loading them all is the simple, correct read.
 */
public interface LabelRepository {

    void save(OwnerId owner, Label label);

    Optional<Label> findById(OwnerId owner, LabelId id);

    List<Label> findAllByOwner(OwnerId owner);

    void delete(OwnerId owner, LabelId id);
}
