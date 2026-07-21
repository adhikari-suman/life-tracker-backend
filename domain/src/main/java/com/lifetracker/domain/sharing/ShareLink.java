package com.lifetracker.domain.sharing;

import com.lifetracker.domain.user.UserId;

import java.time.Instant;
import java.util.Objects;

/**
 * The single anonymous "anyone with the link" capability for an owner's Book — whole-Book,
 * read-only (ADR-0005, ADR-0008). At most one per owner. Revoking it deletes it (burns the token),
 * so a revoked URL can never be reactivated; re-sharing mints a new one. Identity is the
 * {@link ShareLinkId}.
 */
public final class ShareLink {

    private final ShareLinkId id;
    private final UserId ownerId;
    private final ShareToken token;
    private final Instant createdAt;

    private ShareLink(ShareLinkId id, UserId ownerId, ShareToken token, Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.ownerId = Objects.requireNonNull(ownerId, "ownerId");
        this.token = Objects.requireNonNull(token, "token");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    }

    /** Turn link sharing on: a fresh capability for this owner's Book. */
    public static ShareLink create(ShareLinkId id, UserId ownerId, ShareToken token, Instant now) {
        return new ShareLink(id, ownerId, token, now);
    }

    /** Reconstitute from storage. For the persistence adapter, not business code. */
    public static ShareLink rehydrate(ShareLinkId id, UserId ownerId, ShareToken token, Instant createdAt) {
        return new ShareLink(id, ownerId, token, createdAt);
    }

    public ShareLinkId id() {
        return id;
    }

    public UserId ownerId() {
        return ownerId;
    }

    public ShareToken token() {
        return token;
    }

    public Instant createdAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        return o instanceof ShareLink other && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "ShareLink[" + id + ", owner=" + ownerId + "]";
    }
}
