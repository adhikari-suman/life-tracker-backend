package com.lifetracker.infrastructure.persistence.sharing;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * The {@code share_links} table (code-first, ADR-0009). One per owner; the token is stored as-is
 * (retrievable, ADR-0008). Package-private.
 */
@Entity
@Table(name = "share_links")
class ShareLinkEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "owner_user_id", nullable = false, updatable = false, unique = true)
    private UUID ownerUserId;

    @Column(name = "token", nullable = false, unique = true, length = 200)
    private String token;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected ShareLinkEntity() {
    }

    ShareLinkEntity(UUID id, UUID ownerUserId, String token, OffsetDateTime createdAt) {
        this.id = id;
        this.ownerUserId = ownerUserId;
        this.token = token;
        this.createdAt = createdAt;
    }

    UUID getId() {
        return id;
    }

    UUID getOwnerUserId() {
        return ownerUserId;
    }

    String getToken() {
        return token;
    }

    OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
