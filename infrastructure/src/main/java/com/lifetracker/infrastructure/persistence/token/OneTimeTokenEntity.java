package com.lifetracker.infrastructure.persistence.token;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * The {@code one_time_tokens} table (code-first, ADR-0009): verification and reset tokens, stored as
 * a SHA-256 hash with a purpose and expiry (ADR-0011). Timestamps are app-set (the use case's Clock).
 * Package-private: nothing outside {@code infrastructure.persistence} may touch an entity.
 */
@Entity
@Table(name = "one_time_tokens")
class OneTimeTokenEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "token_hash", nullable = false, unique = true, updatable = false, length = 100)
    private String tokenHash;

    @Column(name = "purpose", nullable = false, updatable = false, length = 32)
    private String purpose;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** JPA requires a no-arg constructor; nothing else should use it. */
    protected OneTimeTokenEntity() {
    }

    OneTimeTokenEntity(UUID id, UUID userId, String tokenHash, String purpose, Instant expiresAt, Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.purpose = purpose;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
    }

    UUID getId() {
        return id;
    }

    UUID getUserId() {
        return userId;
    }

    String getTokenHash() {
        return tokenHash;
    }

    String getPurpose() {
        return purpose;
    }

    Instant getExpiresAt() {
        return expiresAt;
    }

    Instant getCreatedAt() {
        return createdAt;
    }
}
