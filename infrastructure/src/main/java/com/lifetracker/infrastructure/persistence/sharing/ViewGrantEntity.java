package com.lifetracker.infrastructure.persistence.sharing;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * The {@code view_grants} table (code-first, ADR-0009). One per (owner, grantee); grantee_email is
 * denormalised for display. Package-private.
 */
@Entity
@Table(name = "view_grants")
class ViewGrantEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "owner_user_id", nullable = false, updatable = false)
    private UUID ownerUserId;

    @Column(name = "grantee_user_id", nullable = false, updatable = false)
    private UUID granteeUserId;

    @Column(name = "grantee_email", nullable = false, length = 254)
    private String granteeEmail;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected ViewGrantEntity() {
    }

    ViewGrantEntity(UUID id, UUID ownerUserId, UUID granteeUserId, String granteeEmail, OffsetDateTime createdAt) {
        this.id = id;
        this.ownerUserId = ownerUserId;
        this.granteeUserId = granteeUserId;
        this.granteeEmail = granteeEmail;
        this.createdAt = createdAt;
    }

    UUID getId() {
        return id;
    }

    UUID getOwnerUserId() {
        return ownerUserId;
    }

    UUID getGranteeUserId() {
        return granteeUserId;
    }

    String getGranteeEmail() {
        return granteeEmail;
    }

    OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
