package com.lifetracker.domain.session;

import com.lifetracker.domain.user.UserId;

import java.util.List;
import java.util.Optional;

/**
 * The store of Sessions, as the domain needs it. The port lives here; the JPA adapter lives in
 * infrastructure. Listing a User's Sessions for a screen is a read and belongs to a query service,
 * not here — this port serves the write side (open, rotate, revoke).
 */
public interface SessionRepository {

    void save(Session session);

    Optional<Session> findById(SessionId id);

    /** All of a User's Sessions — the input to "sign out everywhere", which revokes each. */
    List<Session> findByUserId(UserId userId);
}
