package com.lifetracker.domain.user;

import java.util.Optional;

/**
 * The store of Users, shaped as the domain needs it. The port lives here; the JPA adapter lives
 * in infrastructure and adapts to this shape, never the reverse.
 */
public interface UserRepository {

    /** Persist a new or updated User. */
    void save(User user);

    Optional<User> findById(UserId id);

    /** Look a User up by login identifier. Email is stored normalized, so this is case-insensitive. */
    Optional<User> findByEmail(Email email);

    /** Whether a User already holds this email — the registration uniqueness check. */
    boolean existsByEmail(Email email);
}
