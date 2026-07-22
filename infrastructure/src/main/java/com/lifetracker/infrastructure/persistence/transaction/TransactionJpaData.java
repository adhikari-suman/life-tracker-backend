package com.lifetracker.infrastructure.persistence.transaction;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Spring Data access to {@code transactions}. Internal to the adapter and query service. */
interface TransactionJpaData extends JpaRepository<TransactionEntity, UUID> {

    Optional<TransactionEntity> findByOwnerIdAndId(UUID ownerId, UUID id);

    List<TransactionEntity> findByOwnerIdOrderByDateDescCreatedAtDesc(UUID ownerId);

    @Query("select t from TransactionEntity t where t.ownerId = :owner and t.id in "
            + "(select p.transactionId from PostingEntity p where p.accountId = :accountId) "
            + "order by t.date desc, t.createdAt desc")
    List<TransactionEntity> findByOwnerAndAccount(@Param("owner") UUID owner, @Param("accountId") UUID accountId);
}
