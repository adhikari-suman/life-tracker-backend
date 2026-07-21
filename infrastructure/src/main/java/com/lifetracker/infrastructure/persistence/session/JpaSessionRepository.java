package com.lifetracker.infrastructure.persistence.session;

import com.lifetracker.domain.session.Session;
import com.lifetracker.domain.session.SessionId;
import com.lifetracker.domain.session.SessionRepository;
import com.lifetracker.domain.user.UserId;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * The {@link SessionRepository} port, backed by Spring Data JPA. Speaks domain types; the entity
 * and the Spring Data interface stay behind it.
 */
@Repository
class JpaSessionRepository implements SessionRepository {

    private final SessionJpaData data;

    JpaSessionRepository(SessionJpaData data) {
        this.data = data;
    }

    @Override
    public void save(Session session) {
        data.save(SessionMapper.toEntity(session));
    }

    @Override
    public Optional<Session> findById(SessionId id) {
        return data.findById(id.value()).map(SessionMapper::toDomain);
    }

    @Override
    public List<Session> findByUserId(UserId userId) {
        return data.findByUserId(userId.value()).stream().map(SessionMapper::toDomain).toList();
    }
}
