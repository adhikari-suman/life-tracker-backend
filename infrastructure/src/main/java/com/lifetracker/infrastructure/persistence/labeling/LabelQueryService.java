package com.lifetracker.infrastructure.persistence.labeling;

import com.lifetracker.domain.labeling.Label;
import com.lifetracker.domain.labeling.LabelId;
import com.lifetracker.domain.labeling.LabelTree;
import com.lifetracker.domain.ledger.OwnerId;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * Read side for labels. Returns the Book's labels flat, each with its full path; a client builds the
 * tree from {@code parentId}. There is deliberately no search endpoint — a personal Book holds dozens
 * of labels, so the whole set is fetched once and filtered client-side.
 *
 * <p>Owner-scoped ({@link OwnerId}) — the ADR-0006 read guard.
 */
@Component
public class LabelQueryService {

    private final LabelJpaData data;

    LabelQueryService(LabelJpaData data) {
        this.data = data;
    }

    /**
     * Archived labels are excluded unless asked for: they are retired from the picker while still
     * reporting whatever was already tagged with them (ADR-0015).
     *
     * <p>The tree is always built from ALL of the Book's labels, even when archived ones are being
     * filtered out of the result — otherwise an archived parent would break the path of a live child.
     */
    public List<LabelView> findByOwner(OwnerId owner, boolean includeArchived) {
        List<Label> all = data.findByOwnerIdOrderByName(owner.value()).stream().map(LabelMapper::toDomain).toList();
        LabelTree tree = new LabelTree(all);
        return all.stream()
                .filter(label -> includeArchived || !label.isArchived())
                .map(label -> new LabelView(
                        label.id().value(),
                        label.name().value(),
                        tree.pathOf(label.id()),
                        label.parent().map(LabelId::value).orElse(null),
                        label.isArchived()))
                .sorted(Comparator.comparing(LabelView::path, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }
}
