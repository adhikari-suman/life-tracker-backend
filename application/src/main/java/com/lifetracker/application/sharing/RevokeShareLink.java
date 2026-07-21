package com.lifetracker.application.sharing;

import com.lifetracker.domain.sharing.ShareLinkRepository;
import com.lifetracker.domain.user.UserId;

/**
 * Turns link sharing off — burns the owner's Share Link so the old URL can never be reactivated.
 * Idempotent: revoking when there is no link is a no-op.
 */
public final class RevokeShareLink {

    private final ShareLinkRepository shareLinks;

    public RevokeShareLink(ShareLinkRepository shareLinks) {
        this.shareLinks = shareLinks;
    }

    public void execute(UserId ownerId) {
        shareLinks.deleteByOwnerId(ownerId);
    }
}
