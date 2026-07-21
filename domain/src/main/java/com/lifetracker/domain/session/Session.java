package com.lifetracker.domain.session;

import com.lifetracker.domain.user.UserId;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * One login on one device — a persistent, revocable login lineage for a {@link UserId}. It holds
 * the CURRENT refresh-token hash and nothing older (the single-current-hash model): presenting a
 * token whose hash does not match the current one is treated as a replay of a retired token —
 * i.e. theft — and revokes the whole Session (reuse detection). A valid rotation advances a
 * sliding expiry, bounded by an absolute cap.
 *
 * <p>The aggregate deals only in hashes and instants; generating and hashing the raw token, and
 * signing access tokens, are infrastructure ports. Identity is the {@link SessionId}: two
 * Sessions are equal exactly when their ids match.
 */
public final class Session {

    private static final Duration SLIDING_WINDOW = Duration.ofDays(30);
    private static final Duration ABSOLUTE_CAP = Duration.ofDays(90);

    private final SessionId id;
    private final UserId userId;
    private final String deviceLabel;
    private final Instant createdAt;
    private RefreshTokenHash refreshTokenHash;
    private Instant lastUsedAt;
    private Instant expiresAt;
    private boolean revoked;

    private Session(SessionId id, UserId userId, String deviceLabel, Instant createdAt,
                    RefreshTokenHash refreshTokenHash, Instant lastUsedAt, Instant expiresAt, boolean revoked) {
        this.id = Objects.requireNonNull(id, "id");
        this.userId = Objects.requireNonNull(userId, "userId");
        this.deviceLabel = Objects.requireNonNull(deviceLabel, "deviceLabel");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.refreshTokenHash = Objects.requireNonNull(refreshTokenHash, "refreshTokenHash");
        this.lastUsedAt = Objects.requireNonNull(lastUsedAt, "lastUsedAt");
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
        this.revoked = revoked;
    }

    /** Open a fresh Session at login: active, unrevoked, expiring one sliding window from now. */
    public static Session open(SessionId id, UserId userId, RefreshTokenHash initialHash,
                               String deviceLabel, Instant now) {
        Objects.requireNonNull(now, "now");
        return new Session(id, userId, deviceLabel, now, initialHash, now, now.plus(SLIDING_WINDOW), false);
    }

    /** Reconstitute a Session from storage. For the persistence adapter, not business code. */
    public static Session rehydrate(SessionId id, UserId userId, String deviceLabel, Instant createdAt,
                                    RefreshTokenHash refreshTokenHash, Instant lastUsedAt, Instant expiresAt,
                                    boolean revoked) {
        return new Session(id, userId, deviceLabel, createdAt, refreshTokenHash, lastUsedAt, expiresAt, revoked);
    }

    /**
     * Rotate the refresh token. The {@code presentedHash} must equal the current one: on a match,
     * the current hash becomes {@code newHash}, last-used advances, and the sliding expiry moves to
     * {@code now + window}, never past the absolute cap. On a mismatch — a retired token replayed —
     * the Session is revoked and {@link RefreshTokenReuseException} is thrown; the caller must
     * persist that revocation. A revoked or expired Session cannot rotate at all
     * ({@link SessionNotActiveException}).
     */
    public void rotate(RefreshTokenHash presentedHash, RefreshTokenHash newHash, Instant now) {
        Objects.requireNonNull(presentedHash, "presentedHash");
        Objects.requireNonNull(newHash, "newHash");
        Objects.requireNonNull(now, "now");
        ensureActive(now);
        if (!refreshTokenHash.equals(presentedHash)) {
            revoked = true;
            throw new RefreshTokenReuseException(id);
        }
        refreshTokenHash = newHash;
        lastUsedAt = now;
        Instant sliding = now.plus(SLIDING_WINDOW);
        Instant cap = createdAt.plus(ABSOLUTE_CAP);
        expiresAt = sliding.isBefore(cap) ? sliding : cap;
    }

    /** End this Session — logout, or part of "sign out everywhere". Idempotent. */
    public void revoke() {
        revoked = true;
    }

    public boolean isActive(Instant now) {
        return !revoked && now.isBefore(expiresAt);
    }

    private void ensureActive(Instant now) {
        if (!isActive(now)) {
            throw new SessionNotActiveException(id);
        }
    }

    public SessionId id() {
        return id;
    }

    public UserId userId() {
        return userId;
    }

    public String deviceLabel() {
        return deviceLabel;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public RefreshTokenHash refreshTokenHash() {
        return refreshTokenHash;
    }

    public Instant lastUsedAt() {
        return lastUsedAt;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public boolean isRevoked() {
        return revoked;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        return o instanceof Session other && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Session[" + id + ", user=" + userId + "]";
    }
}
