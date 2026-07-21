package com.lifetracker.infrastructure.persistence.session;

import com.lifetracker.domain.user.UserId;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Read side for Sessions — the active (non-revoked) Sessions a User holds, as flat {@link SessionView}
 * rows for the "active devices" screen. Never loads the aggregate.
 */
@Component
public class SessionQueryService {

    private final SessionJpaData data;

    SessionQueryService(SessionJpaData data) {
        this.data = data;
    }

    public List<SessionView> findActiveByUser(UserId userId) {
        return data.findByUserId(userId.value()).stream()
                .filter(entity -> !entity.isRevoked())
                .map(entity -> new SessionView(
                        entity.getId(), entity.getDeviceLabel(), entity.getCreatedAt(), entity.getLastUsedAt()))
                .toList();
    }
}
