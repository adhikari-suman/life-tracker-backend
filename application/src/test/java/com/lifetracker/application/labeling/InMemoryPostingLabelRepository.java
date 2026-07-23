package com.lifetracker.application.labeling;

import com.lifetracker.domain.labeling.LabelId;
import com.lifetracker.domain.labeling.PostingLabelRepository;
import com.lifetracker.domain.ledger.OwnerId;
import com.lifetracker.domain.transaction.PostingId;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** In-memory {@link PostingLabelRepository} fake — one label per posting, like the real table. */
public final class InMemoryPostingLabelRepository implements PostingLabelRepository {

    private record Key(OwnerId owner, PostingId posting) {
    }

    private final Map<Key, LabelId> store = new LinkedHashMap<>();

    @Override
    public void assign(OwnerId owner, PostingId posting, LabelId label) {
        store.put(new Key(owner, posting), label);
    }

    @Override
    public void clear(OwnerId owner, PostingId posting) {
        store.remove(new Key(owner, posting));
    }

    @Override
    public Optional<LabelId> findByPosting(OwnerId owner, PostingId posting) {
        return Optional.ofNullable(store.get(new Key(owner, posting)));
    }

    @Override
    public boolean isInUse(OwnerId owner, LabelId label) {
        return store.entrySet().stream()
                .anyMatch(entry -> entry.getKey().owner().equals(owner) && entry.getValue().equals(label));
    }

    /** How many attachments exist — lets a test assert that nothing was tagged. */
    public int size() {
        return store.size();
    }
}
