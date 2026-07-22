package com.lifetracker.domain.token;

import com.lifetracker.domain.user.UserId;

import java.time.Instant;
import java.util.Objects;

/**
 * A single-use, expiring token that proves control of a User's email — for verification or a
 * password reset (its {@link TokenPurpose}). The raw value is never stored; only its
 * {@link OneTimeTokenHash} lives here, exactly as a refresh secret does (ADR-0007, ADR-0011).
 * Identity is the {@link OneTimeTokenId}: two tokens are equal only when their ids match.
 */
public final class OneTimeToken {

    private final OneTimeTokenId id;
    private final UserId userId;
    private final OneTimeTokenHash tokenHash;
    private final TokenPurpose purpose;
    private final Instant expiresAt;
    private final Instant createdAt;

    private OneTimeToken(OneTimeTokenId id, UserId userId, OneTimeTokenHash tokenHash,
                         TokenPurpose purpose, Instant expiresAt, Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.userId = Objects.requireNonNull(userId, "userId");
        this.tokenHash = Objects.requireNonNull(tokenHash, "tokenHash");
        this.purpose = Objects.requireNonNull(purpose, "purpose");
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    }

    /** Issue a fresh token for a purpose, expiring at {@code expiresAt}. */
    public static OneTimeToken issue(OneTimeTokenId id, UserId userId, OneTimeTokenHash tokenHash,
                                     TokenPurpose purpose, Instant expiresAt, Instant createdAt) {
        return new OneTimeToken(id, userId, tokenHash, purpose, expiresAt, createdAt);
    }

    /** Reconstitute from storage. For the persistence adapter, not business code. */
    public static OneTimeToken rehydrate(OneTimeTokenId id, UserId userId, OneTimeTokenHash tokenHash,
                                         TokenPurpose purpose, Instant expiresAt, Instant createdAt) {
        return new OneTimeToken(id, userId, tokenHash, purpose, expiresAt, createdAt);
    }

    /** Expired once {@code now} has reached {@code expiresAt}. */
    public boolean isExpired(Instant now) {
        return !now.isBefore(expiresAt);
    }

    public boolean isFor(TokenPurpose expected) {
        return purpose == expected;
    }

    public OneTimeTokenId id() {
        return id;
    }

    public UserId userId() {
        return userId;
    }

    public OneTimeTokenHash tokenHash() {
        return tokenHash;
    }

    public TokenPurpose purpose() {
        return purpose;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public Instant createdAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        return o instanceof OneTimeToken other && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        // Omits the hash.
        return "OneTimeToken[" + id + ", " + purpose + ", user=" + userId + "]";
    }
}
