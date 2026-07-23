package com.lifetracker.infrastructure.persistence.labeling;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Spring Data access to {@code labels}. Internal to the adapter and query service. */
interface LabelJpaData extends JpaRepository<LabelEntity, UUID> {

    Optional<LabelEntity> findByOwnerIdAndId(UUID ownerId, UUID id);

    List<LabelEntity> findByOwnerIdOrderByName(UUID ownerId);
}
