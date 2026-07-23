package com.lifetracker.application.labeling;

import com.lifetracker.domain.labeling.Label;
import com.lifetracker.domain.labeling.LabelId;
import com.lifetracker.domain.labeling.LabelRepository;
import com.lifetracker.domain.ledger.OwnerId;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** In-memory owner-scoped {@link LabelRepository} fake. Public so the transaction tests can use it too. */
public final class InMemoryLabelRepository implements LabelRepository {

    private record Key(OwnerId owner, LabelId id) {
    }

    private final Map<Key, Label> store = new LinkedHashMap<>();

    @Override
    public void save(OwnerId owner, Label label) {
        store.put(new Key(owner, label.id()), label);
    }

    @Override
    public Optional<Label> findById(OwnerId owner, LabelId id) {
        return Optional.ofNullable(store.get(new Key(owner, id)));
    }

    @Override
    public List<Label> findAllByOwner(OwnerId owner) {
        List<Label> found = new ArrayList<>();
        store.forEach((key, label) -> {
            if (key.owner().equals(owner)) {
                found.add(label);
            }
        });
        return found;
    }

    @Override
    public void delete(OwnerId owner, LabelId id) {
        store.remove(new Key(owner, id));
    }
}
