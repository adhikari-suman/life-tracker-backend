package com.lifetracker.infrastructure.persistence.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * The {@code users} table, as JPA sees it. Code-first: this class is the design surface for the
 * table, and the Liquibase changeset (001-create-users) is written to match it; {@code ddl-auto:
 * validate} fails the boot if they drift (ADR-0009). It mirrors columns, not concepts — the
 * domain {@code User} is a separate, pure class, reassembled by {@code UserMapper}.
 *
 * <p>Package-private: nothing outside {@code infrastructure.persistence} may touch an entity.
 */
@Entity
@Table(name = "users")
class UserEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "email", nullable = false, unique = true, length = 254)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified;

    // Set by the database default (now()); the app never writes it, so it is read-only to JPA.
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    /** JPA requires a no-arg constructor; nothing else should use it. */
    protected UserEntity() {
    }

    UserEntity(UUID id, String email, String passwordHash, boolean emailVerified) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.emailVerified = emailVerified;
    }

    UUID getId() {
        return id;
    }

    String getEmail() {
        return email;
    }

    String getPasswordHash() {
        return passwordHash;
    }

    boolean isEmailVerified() {
        return emailVerified;
    }

    OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
