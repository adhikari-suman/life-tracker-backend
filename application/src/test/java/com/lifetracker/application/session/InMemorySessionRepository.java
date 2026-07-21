package com.lifetracker.application.session;

import com.lifetracker.domain.session.Session;
import com.lifetracker.domain.session.SessionId;
import com.lifetracker.domain.session.SessionRepository;
import com.lifetracker.domain.user.UserId;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** An in-memory {@link SessionRepository} fake — assertable, no Mockito. */
final class InMemorySessionRepository implements SessionRepository {

    private final Map<SessionId, Session> byId = new HashMap<>();

    @Override
    public void save(Session session) {
        byId.put(session.id(), session);
    }

    @Override
    public Optional<Session> findById(SessionId id) {
        return Optional.ofNullable(byId.get(id));
    }

    @Override
    public List<Session> findByUserId(UserId userId) {
        List<Session> out = new ArrayList<>();
        for (Session s : byId.values()) {
            if (s.userId().equals(userId)) {
                out.add(s);
            }
        }
        return out;
    }
}
