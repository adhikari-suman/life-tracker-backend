package com.lifetracker.infrastructure.persistence.session;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * The {@code sessions} table, as JPA sees it. Code-first design surface for the table; the
 * Liquibase changeset {@code 002-create-sessions} is written to match it and {@code ddl-auto:
 * validate} guards the two (ADR-0009). Unlike {@code users.created_at}, all timestamps here are
 * set by the domain ({@code Session.open}/{@code rotate}), so they are ordinary insertable columns.
 * Package-private — nothing outside {@code infrastructure.persistence} may touch an entity.
 */
@Entity
@Table(name = "sessions")
class SessionEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "refresh_token_hash", nullable = false, length = 100)
    private String refreshTokenHash;

    @Column(name = "device_label", nullable = false, length = 200)
    private String deviceLabel;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "last_used_at", nullable = false)
    private OffsetDateTime lastUsedAt;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "revoked", nullable = false)
    private boolean revoked;

    /** JPA requires a no-arg constructor; nothing else should use it. */
    protected SessionEntity() {
    }

    SessionEntity(UUID id, UUID userId, String refreshTokenHash, String deviceLabel,
                  OffsetDateTime createdAt, OffsetDateTime lastUsedAt, OffsetDateTime expiresAt, boolean revoked) {
        this.id = id;
        this.userId = userId;
        this.refreshTokenHash = refreshTokenHash;
        this.deviceLabel = deviceLabel;
        this.createdAt = createdAt;
        this.lastUsedAt = lastUsedAt;
        this.expiresAt = expiresAt;
        this.revoked = revoked;
    }

    UUID getId() {
        return id;
    }

    UUID getUserId() {
        return userId;
    }

    String getRefreshTokenHash() {
        return refreshTokenHash;
    }

    String getDeviceLabel() {
        return deviceLabel;
    }

    OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    OffsetDateTime getLastUsedAt() {
        return lastUsedAt;
    }

    OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    boolean isRevoked() {
        return revoked;
    }
}
