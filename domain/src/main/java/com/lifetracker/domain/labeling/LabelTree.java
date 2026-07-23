package com.lifetracker.domain.labeling;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * One Book's whole label tree, in memory, with the rules that span more than one label: how deep it
 * may go, that it may not contain a cycle, and that siblings may not share a name (ADR-0015).
 *
 * <p>A personal Book holds dozens of labels, not thousands, so the tree is loaded whole and reasoned
 * about here rather than in SQL. That keeps the rules in the domain, where they can be tested without
 * a database.
 *
 * <p>Depth is 1-based: a root label is at depth 1, and {@link #MAX_DEPTH} is the deepest a label may
 * sit. Height is the same measure from the other end — a leaf has height 1 — which is what makes the
 * subtree check on a move possible.
 */
public final class LabelTree {

    /**
     * Three levels, e.g. {@code food -> restaurants -> fast food}. Deliberately tight: raising the cap
     * later leaves every stored label valid, while lowering it would strand labels already nested too
     * deep, so this is the recoverable direction to be wrong in.
     */
    public static final int MAX_DEPTH = 3;

    private static final String PATH_SEPARATOR = " / ";

    private final Map<LabelId, Label> byId;
    private final Map<LabelId, List<Label>> childrenByParent;
    private final List<Label> roots;

    public LabelTree(List<Label> labels) {
        Objects.requireNonNull(labels, "labels");
        this.byId = new HashMap<>();
        for (Label label : labels) {
            byId.put(label.id(), label);
        }
        this.childrenByParent = new HashMap<>();
        this.roots = new ArrayList<>();
        for (Label label : labels) {
            label.parent().ifPresentOrElse(
                    parent -> childrenByParent.computeIfAbsent(parent, key -> new ArrayList<>()).add(label),
                    () -> roots.add(label));
        }
    }

    public Optional<Label> find(LabelId id) {
        return Optional.ofNullable(byId.get(id));
    }

    public List<Label> all() {
        return List.copyOf(byId.values());
    }

    public List<Label> roots() {
        return List.copyOf(roots);
    }

    public List<Label> childrenOf(LabelId id) {
        return List.copyOf(childrenByParent.getOrDefault(id, List.of()));
    }

    public boolean hasChildren(LabelId id) {
        return !childrenByParent.getOrDefault(id, List.of()).isEmpty();
    }

    /** 1-based depth: a root sits at 1. */
    public int depthOf(LabelId id) {
        int depth = 0;
        Optional<LabelId> current = Optional.of(id);
        Set<LabelId> seen = new LinkedHashSet<>();
        while (current.isPresent()) {
            LabelId at = current.get();
            if (!seen.add(at)) {
                throw new LabelCycleException();      // defensive: stored data should never cycle
            }
            Label label = byId.get(at);
            if (label == null) {
                break;
            }
            depth++;
            current = label.parent();
        }
        return depth;
    }

    /** Height of the subtree rooted here: a leaf is 1. */
    public int heightOf(LabelId id) {
        int tallest = 0;
        for (Label child : childrenByParent.getOrDefault(id, List.of())) {
            tallest = Math.max(tallest, heightOf(child.id()));
        }
        return tallest + 1;
    }

    /** The chain from this label up to its root, this label first. This is what a roll-up walks. */
    public List<LabelId> selfAndAncestorsOf(LabelId id) {
        List<LabelId> chain = new ArrayList<>();
        Set<LabelId> seen = new LinkedHashSet<>();
        Optional<LabelId> current = Optional.of(id);
        while (current.isPresent()) {
            LabelId at = current.get();
            if (!seen.add(at)) {
                throw new LabelCycleException();
            }
            Label label = byId.get(at);
            if (label == null) {
                break;
            }
            chain.add(at);
            current = label.parent();
        }
        return chain;
    }

    public boolean isSelfOrDescendantOf(LabelId candidate, LabelId ancestor) {
        return selfAndAncestorsOf(candidate).contains(ancestor);
    }

    /** {@code food / restaurants / fast food} — what disambiguates two siblings-of-different-parents. */
    public String pathOf(LabelId id) {
        List<LabelId> chain = selfAndAncestorsOf(id);
        StringBuilder path = new StringBuilder();
        for (int i = chain.size() - 1; i >= 0; i--) {
            Label label = byId.get(chain.get(i));
            if (label == null) {
                continue;
            }
            if (path.length() > 0) {
                path.append(PATH_SEPARATOR);
            }
            path.append(label.name().value());
        }
        return path.toString();
    }

    /**
     * A new label may be created here only if the name is free among its siblings and the parent is
     * not already at the deepest level.
     */
    public void requireCanAdd(Optional<LabelId> parent, LabelName name) {
        requireNameFreeAmongSiblings(parent, name, Optional.empty());
        int depth = parent.map(this::depthOf).orElse(0) + 1;
        if (depth > MAX_DEPTH) {
            throw new LabelDepthExceededException(depth);
        }
    }

    /**
     * A label may move only if it would not land beneath itself, its name stays free among its new
     * siblings, and — the part that is easy to miss — its <em>whole subtree</em> still fits. Moving a
     * two-deep subtree under a label at depth 2 breaks the cap even though the moved label itself
     * would sit at a legal depth.
     */
    public void requireCanMove(LabelId label, Optional<LabelId> newParent, LabelName name) {
        if (newParent.filter(target -> isSelfOrDescendantOf(target, label)).isPresent()) {
            throw new LabelCycleException();
        }
        requireNameFreeAmongSiblings(newParent, name, Optional.of(label));
        int newDepth = newParent.map(this::depthOf).orElse(0) + heightOf(label);
        if (newDepth > MAX_DEPTH) {
            throw new LabelDepthExceededException(newDepth);
        }
    }

    /** Renaming in place: the name must be free among the label's existing siblings. */
    public void requireCanRename(LabelId label, LabelName name) {
        Optional<LabelId> parent = find(label).flatMap(Label::parent);
        requireNameFreeAmongSiblings(parent, name, Optional.of(label));
    }

    private void requireNameFreeAmongSiblings(Optional<LabelId> parent, LabelName name, Optional<LabelId> excluding) {
        List<Label> siblings = parent.map(this::childrenOf).orElseGet(this::roots);
        for (Label sibling : siblings) {
            if (excluding.filter(sibling.id()::equals).isPresent()) {
                continue;
            }
            if (sibling.name().sameAs(name)) {
                throw new DuplicateLabelNameException(name);
            }
        }
    }
}
