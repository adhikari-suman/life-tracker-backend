package com.lifetracker.domain.user;

import java.util.Objects;

/**
 * A person who can sign in. The single kind of identity in the system; "owner" and "viewer" are
 * relationships to a Book, not kinds of User (see the Identity &amp; Sharing context). Identity
 * is the {@link UserId}: two Users are equal exactly when their ids match, never by email or
 * hash.
 */
public final class User {

    private final UserId id;
    private final Email email;
    private final PasswordHash passwordHash;
    private final boolean emailVerified;

    private User(UserId id, Email email, PasswordHash passwordHash, boolean emailVerified) {
        this.id = Objects.requireNonNull(id, "id");
        this.email = Objects.requireNonNull(email, "email");
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash");
        this.emailVerified = emailVerified;
    }

    /**
     * A newly registered User: identified, credentialed, and not yet email-verified. Verification
     * is a deferred seam (ADR-0007) — a fresh User starts unverified, which today gates nothing
     * and later will gate what they may do, not whether they are signed in.
     */
    public static User register(UserId id, Email email, PasswordHash passwordHash) {
        return new User(id, email, passwordHash, false);
    }

    /** Reconstitute a User from storage. For the persistence adapter, not business code. */
    public static User rehydrate(UserId id, Email email, PasswordHash passwordHash, boolean emailVerified) {
        return new User(id, email, passwordHash, emailVerified);
    }

    public UserId id() {
        return id;
    }

    public Email email() {
        return email;
    }

    public PasswordHash passwordHash() {
        return passwordHash;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        return o instanceof User other && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        // Deliberately omits the password hash.
        return "User[" + id + ", " + email + "]";
    }
}
