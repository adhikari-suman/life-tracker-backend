package com.lifetracker.application.sharing;

import com.lifetracker.domain.sharing.ShareLink;
import com.lifetracker.domain.sharing.ShareLinkId;
import com.lifetracker.domain.sharing.ShareLinkRepository;
import com.lifetracker.domain.sharing.ShareTokens;
import com.lifetracker.domain.user.User;
import com.lifetracker.domain.user.UserId;
import com.lifetracker.domain.user.UserRepository;

import java.time.Clock;

/**
 * Turns link sharing on for the owner's Book. Idempotent: if a link is already active it is
 * returned unchanged (the "already on" switch), otherwise a fresh unguessable token is minted. The
 * result flags which happened, so the boundary can answer 200 vs 201.
 *
 * <p>Gated on verification (ADR-0011): only an owner with a verified email may mint a Share Link.
 */
public final class CreateShareLink {

    private final ShareLinkRepository shareLinks;
    private final ShareTokens shareTokens;
    private final UserRepository users;
    private final Clock clock;

    public CreateShareLink(ShareLinkRepository shareLinks, ShareTokens shareTokens, UserRepository users, Clock clock) {
        this.shareLinks = shareLinks;
        this.shareTokens = shareTokens;
        this.users = users;
        this.clock = clock;
    }

    public CreateShareLinkResult execute(UserId ownerId) {
        requireVerified(ownerId);
        return shareLinks.findByOwnerId(ownerId)
                .map(existing -> new CreateShareLinkResult(existing, false))
                .orElseGet(() -> {
                    ShareLink link = ShareLink.create(
                            ShareLinkId.generate(), ownerId, shareTokens.generate(), clock.instant());
                    shareLinks.save(link);
                    return new CreateShareLinkResult(link, true);
                });
    }

    private void requireVerified(UserId ownerId) {
        User owner = users.findById(ownerId).orElseThrow(EmailNotVerifiedException::new);
        if (!owner.isEmailVerified()) {
            throw new EmailNotVerifiedException();
        }
    }
}
