package com.lifetracker.infrastructure.persistence.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * The {@code login_attempts} table: one row per failed login, keyed by email. The timestamp is set
 * by the app (not a DB default) — the use case's {@code Clock} owns the instant, so the sliding
 * window stays testable. Code-first: the changeset 005 is written to match, and {@code ddl-auto:
 * validate} fails the boot if they drift (ADR-0009). A log row, not an aggregate — no domain class
 * sits behind it.
 *
 * <p>Package-private: nothing outside {@code infrastructure.persistence} may touch an entity.
 */
@Entity
@Table(name = "login_attempts")
class LoginAttemptEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "email", nullable = false, length = 254)
    private String email;

    @Column(name = "attempted_at", nullable = false, updatable = false)
    private Instant attemptedAt;

    /** JPA requires a no-arg constructor; nothing else should use it. */
    protected LoginAttemptEntity() {
    }

    LoginAttemptEntity(UUID id, String email, Instant attemptedAt) {
        this.id = id;
        this.email = email;
        this.attemptedAt = attemptedAt;
    }

    UUID getId() {
        return id;
    }

    String getEmail() {
        return email;
    }

    Instant getAttemptedAt() {
        return attemptedAt;
    }
}
