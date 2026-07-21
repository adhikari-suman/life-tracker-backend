package com.lifetracker.infrastructure.persistence.session;

import com.lifetracker.domain.session.RefreshTokenHash;
import com.lifetracker.domain.session.Session;
import com.lifetracker.domain.session.SessionId;
import com.lifetracker.domain.user.UserId;

import java.time.ZoneOffset;

/**
 * Converts between the domain {@link Session} and the {@link SessionEntity}. Plain static methods.
 * The domain keeps instants in UTC; the table stores {@code timestamptz}, so the mapping is a
 * straight UTC offset both ways.
 */
final class SessionMapper {

    private SessionMapper() {
    }

    static SessionEntity toEntity(Session session) {
        return new SessionEntity(
                session.id().value(),
                session.userId().value(),
                session.refreshTokenHash().value(),
                session.deviceLabel(),
                session.createdAt().atOffset(ZoneOffset.UTC),
                session.lastUsedAt().atOffset(ZoneOffset.UTC),
                session.expiresAt().atOffset(ZoneOffset.UTC),
                session.isRevoked());
    }

    static Session toDomain(SessionEntity entity) {
        return Session.rehydrate(
                SessionId.of(entity.getId()),
                UserId.of(entity.getUserId()),
                entity.getDeviceLabel(),
                entity.getCreatedAt().toInstant(),
                new RefreshTokenHash(entity.getRefreshTokenHash()),
                entity.getLastUsedAt().toInstant(),
                entity.getExpiresAt().toInstant(),
                entity.isRevoked());
    }
}
