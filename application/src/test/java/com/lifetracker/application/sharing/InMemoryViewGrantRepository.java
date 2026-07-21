package com.lifetracker.application.sharing;

import com.lifetracker.domain.sharing.ViewGrant;
import com.lifetracker.domain.sharing.ViewGrantId;
import com.lifetracker.domain.sharing.ViewGrantRepository;
import com.lifetracker.domain.user.UserId;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** In-memory {@link ViewGrantRepository} fake. */
final class InMemoryViewGrantRepository implements ViewGrantRepository {

    private final Map<ViewGrantId, ViewGrant> byId = new HashMap<>();

    @Override
    public void save(ViewGrant grant) {
        byId.put(grant.id(), grant);
    }

    @Override
    public Optional<ViewGrant> findById(ViewGrantId id) {
        return Optional.ofNullable(byId.get(id));
    }

    @Override
    public List<ViewGrant> findByOwnerId(UserId ownerId) {
        return byId.values().stream().filter(g -> g.ownerId().equals(ownerId)).toList();
    }

    @Override
    public Optional<ViewGrant> findByOwnerIdAndGranteeId(UserId ownerId, UserId granteeId) {
        return byId.values().stream()
                .filter(g -> g.ownerId().equals(ownerId) && g.granteeId().equals(granteeId))
                .findFirst();
    }

    @Override
    public void deleteById(ViewGrantId id) {
        byId.remove(id);
    }
}
