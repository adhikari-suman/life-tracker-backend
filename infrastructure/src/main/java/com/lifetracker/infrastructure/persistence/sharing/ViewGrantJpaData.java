package com.lifetracker.infrastructure.persistence.sharing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Spring Data access to {@code view_grants}. Internal to the adapters and query service. */
interface ViewGrantJpaData extends JpaRepository<ViewGrantEntity, UUID> {

    List<ViewGrantEntity> findByOwnerUserId(UUID ownerUserId);

    Optional<ViewGrantEntity> findByOwnerUserIdAndGranteeUserId(UUID ownerUserId, UUID granteeUserId);
}
