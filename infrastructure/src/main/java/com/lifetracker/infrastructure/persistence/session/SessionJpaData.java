package com.lifetracker.infrastructure.persistence.session;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data access to the {@code sessions} table. An internal detail of
 * {@link JpaSessionRepository}, never the domain port itself.
 */
interface SessionJpaData extends JpaRepository<SessionEntity, UUID> {

    List<SessionEntity> findByUserId(UUID userId);
}
