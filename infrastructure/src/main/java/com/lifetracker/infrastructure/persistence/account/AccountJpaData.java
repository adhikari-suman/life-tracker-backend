package com.lifetracker.infrastructure.persistence.account;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Spring Data access to {@code accounts}. Internal to the adapter and query service. */
interface AccountJpaData extends JpaRepository<AccountEntity, UUID> {

    Optional<AccountEntity> findByOwnerIdAndId(UUID ownerId, UUID id);

    List<AccountEntity> findByOwnerIdOrderByName(UUID ownerId);
}
