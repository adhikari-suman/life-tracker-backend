package com.lifetracker.domain.labeling;

import java.util.Objects;
import java.util.Optional;

/**
 * A user-defined category — what money was FOR (ADR-0014). One node of a tree at most
 * {@link LabelTree#MAX_DEPTH} levels deep. It holds NO owner: like every Ledger aggregate, isolation
 * is enforced around it, never inside it (CONTEXT-MAP, ADR-0006).
 *
 * <p>A label says nothing about what KIND of transaction something is. "Internal transfer", "opening
 * balance" and "credit-card payment" are facts about the accounts a transaction touches, derived from
 * the account kinds, and are deliberately not labels — putting them here would turn a structural
 * guarantee into a filter someone must remember (ADR-0014).
 *
 * <p>Immutable: a rename or a move returns a new instance. Identity is the {@link LabelId}, so two
 * labels with the same name are not the same label.
 */
public final class Label {

    private final LabelId id;
    private final LabelName name;
    private final Optional<LabelId> parent;
    private final boolean archived;

    private Label(LabelId id, LabelName name, Optional<LabelId> parent, boolean archived) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = Objects.requireNonNull(name, "name");
        this.parent = Objects.requireNonNull(parent, "parent");
        this.archived = archived;
        if (parent.filter(id::equals).isPresent()) {
            throw new LabelCycleException();
        }
    }

    /** Create a root label. */
    public static Label root(LabelId id, LabelName name) {
        return new Label(id, name, Optional.empty(), false);
    }

    /** Create a label beneath a parent. */
    public static Label under(LabelId id, LabelName name, LabelId parent) {
        return new Label(id, name, Optional.of(Objects.requireNonNull(parent, "parent")), false);
    }

    /** Reconstitute from storage. For the persistence adapter, not business code. */
    public static Label rehydrate(LabelId id, LabelName name, LabelId parent, boolean archived) {
        return new Label(id, name, Optional.ofNullable(parent), archived);
    }

    public Label renamedTo(LabelName newName) {
        return new Label(id, newName, parent, archived);
    }

    /** Move under a new parent, or to the root when empty. */
    public Label movedTo(Optional<LabelId> newParent) {
        return new Label(id, name, newParent, archived);
    }

    /** Retire from the picker without disturbing what it was already applied to. */
    public Label archivedLabel() {
        return new Label(id, name, parent, true);
    }

    public Label restored() {
        return new Label(id, name, parent, false);
    }

    public LabelId id() {
        return id;
    }

    public LabelName name() {
        return name;
    }

    public Optional<LabelId> parent() {
        return parent;
    }

    public boolean isArchived() {
        return archived;
    }

    public boolean isRoot() {
        return parent.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        return o instanceof Label other && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Label[" + id + ", " + name.value() + (archived ? ", archived]" : "]");
    }
}
