package com.lifetracker.infrastructure.persistence.labeling;

import com.lifetracker.domain.labeling.Label;
import com.lifetracker.domain.labeling.LabelId;
import com.lifetracker.domain.labeling.LabelRepository;
import com.lifetracker.domain.ledger.OwnerId;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** The {@link LabelRepository} port, backed by Spring Data JPA. Owner-scoped; speaks Ledger types. */
@Repository
class JpaLabelRepository implements LabelRepository {

    private final LabelJpaData data;

    JpaLabelRepository(LabelJpaData data) {
        this.data = data;
    }

    @Override
    public void save(OwnerId owner, Label label) {
        data.save(LabelMapper.toEntity(owner, label));
    }

    @Override
    public Optional<Label> findById(OwnerId owner, LabelId id) {
        return data.findByOwnerIdAndId(owner.value(), id.value()).map(LabelMapper::toDomain);
    }

    @Override
    public List<Label> findAllByOwner(OwnerId owner) {
        return data.findByOwnerIdOrderByName(owner.value()).stream().map(LabelMapper::toDomain).toList();
    }

    @Override
    public void delete(OwnerId owner, LabelId id) {
        // Found through the owner-scoped finder first, so a delete can never reach another Book's row.
        data.findByOwnerIdAndId(owner.value(), id.value()).ifPresent(data::delete);
    }
}
