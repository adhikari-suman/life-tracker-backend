package com.lifetracker.application.sharing;

import com.lifetracker.domain.sharing.ShareLink;
import com.lifetracker.domain.sharing.ShareLinkRepository;
import com.lifetracker.domain.sharing.ShareToken;
import com.lifetracker.domain.user.UserId;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** In-memory {@link ShareLinkRepository} fake — one link per owner. */
final class InMemoryShareLinkRepository implements ShareLinkRepository {

    private final Map<UserId, ShareLink> byOwner = new HashMap<>();

    @Override
    public void save(ShareLink shareLink) {
        byOwner.put(shareLink.ownerId(), shareLink);
    }

    @Override
    public Optional<ShareLink> findByOwnerId(UserId ownerId) {
        return Optional.ofNullable(byOwner.get(ownerId));
    }

    @Override
    public Optional<ShareLink> findByToken(ShareToken token) {
        return byOwner.values().stream().filter(l -> l.token().equals(token)).findFirst();
    }

    @Override
    public void deleteByOwnerId(UserId ownerId) {
        byOwner.remove(ownerId);
    }
}
