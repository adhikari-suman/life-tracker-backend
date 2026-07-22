package com.lifetracker.application.user;

import com.lifetracker.domain.session.Session;
import com.lifetracker.domain.session.SessionId;
import com.lifetracker.domain.session.SessionRepository;
import com.lifetracker.domain.user.UserId;

import java.util.List;
import java.util.Optional;

/**
 * A no-session {@link SessionRepository} for the reset unit test — password change and token
 * consumption are asserted here; the "revoke every Session" behaviour is covered end-to-end by the
 * integration test, where real Sessions exist.
 */
final class NoSessionsRepository implements SessionRepository {

    @Override
    public void save(Session session) {
    }

    @Override
    public Optional<Session> findById(SessionId id) {
        return Optional.empty();
    }

    @Override
    public List<Session> findByUserId(UserId userId) {
        return List.of();
    }
}
