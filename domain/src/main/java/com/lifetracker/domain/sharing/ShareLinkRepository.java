package com.lifetracker.domain.sharing;

import com.lifetracker.domain.user.UserId;

import java.util.Optional;

/**
 * The store of Share Links. At most one per owner. {@code findByToken} is the anonymous-viewer
 * lookup — a presented token resolves to the owner whose Book it opens.
 */
public interface ShareLinkRepository {

    void save(ShareLink shareLink);

    Optional<ShareLink> findByOwnerId(UserId ownerId);

    Optional<ShareLink> findByToken(ShareToken token);

    /** Burn the owner's link on revoke, so the old token can never be reactivated. */
    void deleteByOwnerId(UserId ownerId);
}
