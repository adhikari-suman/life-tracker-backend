package com.lifetracker.domain.sharing;

import com.lifetracker.domain.user.Email;
import com.lifetracker.domain.user.UserId;

import java.time.Instant;
import java.util.Objects;

/**
 * A named, read-only grant: {@code granteeId} may read {@code ownerId}'s whole Book (ADR-0005,
 * ADR-0008). The grantee is an already-registered User; their email is kept alongside for display
 * (who the owner shared with). Revocable. Identity is the {@link ViewGrantId}.
 */
public final class ViewGrant {

    private final ViewGrantId id;
    private final UserId ownerId;
    private final UserId granteeId;
    private final Email granteeEmail;
    private final Instant createdAt;

    private ViewGrant(ViewGrantId id, UserId ownerId, UserId granteeId, Email granteeEmail, Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.ownerId = Objects.requireNonNull(ownerId, "ownerId");
        this.granteeId = Objects.requireNonNull(granteeId, "granteeId");
        this.granteeEmail = Objects.requireNonNull(granteeEmail, "granteeEmail");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    }

    public static ViewGrant create(ViewGrantId id, UserId ownerId, UserId granteeId, Email granteeEmail, Instant now) {
        return new ViewGrant(id, ownerId, granteeId, granteeEmail, now);
    }

    public static ViewGrant rehydrate(ViewGrantId id, UserId ownerId, UserId granteeId, Email granteeEmail, Instant createdAt) {
        return new ViewGrant(id, ownerId, granteeId, granteeEmail, createdAt);
    }

    public ViewGrantId id() {
        return id;
    }

    public UserId ownerId() {
        return ownerId;
    }

    public UserId granteeId() {
        return granteeId;
    }

    public Email granteeEmail() {
        return granteeEmail;
    }

    public Instant createdAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        return o instanceof ViewGrant other && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "ViewGrant[" + id + ", owner=" + ownerId + " -> grantee=" + granteeId + "]";
    }
}
