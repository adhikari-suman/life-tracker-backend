package com.lifetracker.infrastructure.persistence.transaction;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/** Spring Data access to {@code postings}. Internal to the adapter and query service. */
interface PostingJpaData extends JpaRepository<PostingEntity, UUID> {

    List<PostingEntity> findByTransactionIdIn(Collection<UUID> transactionIds);
}
