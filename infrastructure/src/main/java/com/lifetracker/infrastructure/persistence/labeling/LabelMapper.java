package com.lifetracker.infrastructure.persistence.labeling;

import com.lifetracker.domain.labeling.Label;
import com.lifetracker.domain.labeling.LabelId;
import com.lifetracker.domain.labeling.LabelName;
import com.lifetracker.domain.ledger.OwnerId;

/** Converts between the domain {@link Label} (no owner) and the {@link LabelEntity} (owner-stamped). */
final class LabelMapper {

    private LabelMapper() {
    }

    static LabelEntity toEntity(OwnerId owner, Label label) {
        return new LabelEntity(
                label.id().value(),
                owner.value(),
                label.name().value(),
                label.parent().map(LabelId::value).orElse(null),
                label.isArchived());
    }

    static Label toDomain(LabelEntity entity) {
        return Label.rehydrate(
                LabelId.of(entity.getId()),
                new LabelName(entity.getName()),
                entity.getParentId() == null ? null : LabelId.of(entity.getParentId()),
                entity.isArchived());
    }
}
