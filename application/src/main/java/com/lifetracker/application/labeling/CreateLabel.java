package com.lifetracker.application.labeling;

import com.lifetracker.domain.labeling.Label;
import com.lifetracker.domain.labeling.LabelId;
import com.lifetracker.domain.labeling.LabelName;
import com.lifetracker.domain.labeling.LabelRepository;
import com.lifetracker.domain.labeling.LabelTree;

import java.util.Optional;

/**
 * Creates a label. The rules that span more than one label — the depth cap and sibling-name
 * uniqueness — live in {@link LabelTree} (ADR-0015); this loads the Book's tree and asks it.
 */
public final class CreateLabel {

    private final LabelRepository labels;

    public CreateLabel(LabelRepository labels) {
        this.labels = labels;
    }

    public LabelId execute(CreateLabelCommand command) {
        LabelName name = new LabelName(command.name());
        Optional<LabelId> parent = Optional.ofNullable(command.parentId()).map(LabelId::of);

        LabelTree tree = new LabelTree(labels.findAllByOwner(command.owner()));
        if (parent.isPresent() && tree.find(parent.get()).isEmpty()) {
            throw new LabelNotFoundException();
        }
        tree.requireCanAdd(parent, name);

        LabelId id = LabelId.generate();
        Label label = parent.map(p -> Label.under(id, name, p)).orElseGet(() -> Label.root(id, name));
        labels.save(command.owner(), label);
        return id;
    }
}
