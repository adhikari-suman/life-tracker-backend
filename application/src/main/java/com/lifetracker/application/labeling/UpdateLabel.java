package com.lifetracker.application.labeling;

import com.lifetracker.domain.labeling.Label;
import com.lifetracker.domain.labeling.LabelId;
import com.lifetracker.domain.labeling.LabelName;
import com.lifetracker.domain.labeling.LabelRepository;
import com.lifetracker.domain.labeling.LabelTree;

import java.util.Optional;

/**
 * Renames, reparents, or archives a label.
 *
 * <p>Reparenting is retroactive by construction (ADR-0015): a posting stores only the label it was
 * tagged with, and roll-up walks the tree as it stands now, so moving a label re-shapes past summaries
 * as well as future ones. That is intended, and it touches no balance — net worth, per-account
 * spending and income never consult labels.
 */
public final class UpdateLabel {

    private final LabelRepository labels;

    public UpdateLabel(LabelRepository labels) {
        this.labels = labels;
    }

    public void execute(UpdateLabelCommand command) {
        LabelId id = LabelId.of(command.labelId());
        Label label = labels.findById(command.owner(), id).orElseThrow(LabelNotFoundException::new);
        LabelTree tree = new LabelTree(labels.findAllByOwner(command.owner()));

        LabelName name = command.name() != null ? new LabelName(command.name()) : label.name();
        Optional<LabelId> parent = command.reparent()
                ? Optional.ofNullable(command.newParentId()).map(LabelId::of)
                : label.parent();

        if (command.reparent()) {
            if (parent.isPresent() && tree.find(parent.get()).isEmpty()) {
                throw new LabelNotFoundException();
            }
            // Checks the cycle, the new siblings' names, and -- easy to miss -- that the whole
            // subtree still fits under the cap, not merely this label.
            tree.requireCanMove(id, parent, name);
        } else if (command.name() != null) {
            tree.requireCanRename(id, name);
        }

        Label updated = label.renamedTo(name).movedTo(parent);
        if (command.archived() != null) {
            updated = command.archived() ? updated.archivedLabel() : updated.restored();
        }
        labels.save(command.owner(), updated);
    }
}
