package com.lifetracker.infrastructure.persistence.sharing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/** Spring Data access to {@code share_links}. Internal to the adapters and query service. */
interface ShareLinkJpaData extends JpaRepository<ShareLinkEntity, UUID> {

    Optional<ShareLinkEntity> findByOwnerUserId(UUID ownerUserId);

    Optional<ShareLinkEntity> findByToken(String token);

    void deleteByOwnerUserId(UUID ownerUserId);
}
