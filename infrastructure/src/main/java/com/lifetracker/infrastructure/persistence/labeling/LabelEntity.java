package com.lifetracker.infrastructure.persistence.labeling;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * The {@code labels} table (code-first, ADR-0009). Carries the {@code owner_id} stamped at the
 * boundary — the aggregate itself holds no owner (ADR-0006). {@code parent_id} is null for a root.
 * Package-private.
 */
@Entity
@Table(name = "labels")
class LabelEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "owner_id", nullable = false, updatable = false)
    private UUID ownerId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    // Nullable: a root label has no parent. Mutable -- reparenting is a supported edit (ADR-0015).
    @Column(name = "parent_id")
    private UUID parentId;

    @Column(name = "archived", nullable = false)
    private boolean archived;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected LabelEntity() {
    }

    LabelEntity(UUID id, UUID ownerId, String name, UUID parentId, boolean archived) {
        this.id = id;
        this.ownerId = ownerId;
        this.name = name;
        this.parentId = parentId;
        this.archived = archived;
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

    UUID getParentId() {
        return parentId;
    }

    boolean isArchived() {
        return archived;
    }

    OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
