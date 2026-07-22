package com.lifetracker.infrastructure.persistence.account;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * The {@code accounts} table (code-first, ADR-0009). Carries the {@code owner_id} stamped at the
 * boundary — the Ledger aggregate itself holds no owner (ADR-0006). Kind and currency are immutable
 * once set (changing them would mis-book history). Package-private.
 */
@Entity
@Table(name = "accounts")
class AccountEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "owner_id", nullable = false, updatable = false)
    private UUID ownerId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "kind", nullable = false, updatable = false, length = 16)
    private String kind;

    @Column(name = "currency", nullable = false, updatable = false, length = 3)
    private String currency;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected AccountEntity() {
    }

    AccountEntity(UUID id, UUID ownerId, String name, String kind, String currency) {
        this.id = id;
        this.ownerId = ownerId;
        this.name = name;
        this.kind = kind;
        this.currency = currency;
    }

    UUID getId() {
        return id;
    }

    UUID getOwnerId() {
        return ownerId;
    }

    String getName() {
        return name;
    }

    String getKind() {
        return kind;
    }

    String getCurrency() {
        return currency;
    }

    OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
